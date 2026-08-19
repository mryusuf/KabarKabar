package io.mryusuf.kabarkabar.data.repository

import io.ktor.client.plugins.ResponseException
import io.mryusuf.kabarkabar.data.local.db.LocalArticleDataSource
import io.mryusuf.kabarkabar.data.mapper.ArticleMapper
import io.mryusuf.kabarkabar.data.mapper.ArticleMappingResult
import io.mryusuf.kabarkabar.data.remote.api.RemoteArticleDataSource
import io.mryusuf.kabarkabar.domain.error.SyncError
import io.mryusuf.kabarkabar.domain.model.Article
import io.mryusuf.kabarkabar.domain.model.ArticleId
import io.mryusuf.kabarkabar.domain.model.ArticleObservation
import io.mryusuf.kabarkabar.domain.model.RefreshResult
import io.mryusuf.kabarkabar.domain.model.NewsCountry
import io.mryusuf.kabarkabar.domain.repository.ArticleRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

/**
 * Repository that implements an offline-first strategy for articles.
 *
 * Local persistence (Room) is the exclusive read source.
 */
class OfflineFirstArticleRepository(
    private val remoteDataSource: RemoteArticleDataSource,
    private val localDataSource: LocalArticleDataSource,
) : ArticleRepository {

    private val countryMutexes = NewsCountry.values().associateWith { Mutex() }
    private val pagingStates = MutableStateFlow<Map<NewsCountry, PagingState>>(emptyMap())
    private val operationGenerations = MutableStateFlow<Map<NewsCountry, Long>>(emptyMap())
    private val pageSize = 20

    private data class PagingState(
        val currentPage: Int,
        val totalResults: Int,
        val hasMore: Boolean,
    )

    private fun mutexFor(country: NewsCountry): Mutex = countryMutexes.getValue(country)

    private fun currentGeneration(country: NewsCountry): Long =
        operationGenerations.value[country] ?: 0L

    private fun advanceGeneration(country: NewsCountry): Long {
        var nextGeneration = 0L
        operationGenerations.update { generations ->
            nextGeneration = (generations[country] ?: 0L) + 1L
            generations + (country to nextGeneration)
        }
        return nextGeneration
    }

    override fun observeArticles(country: NewsCountry): Flow<ArticleObservation<List<Article>>> {
        return flow {
            emitAll(localDataSource.observeAll(country))
        }
            .map { entities ->
                val articles = entities.map { ArticleMapper.mapToDomain(it) }
                val observation: ArticleObservation<List<Article>> = ArticleObservation.Data(articles)
                observation
            }
            .catch { e ->
                if (e is CancellationException) throw e
                emit(ArticleObservation.Failure(SyncError.Persistence))
            }
    }

    override fun observeArticle(id: ArticleId, country: NewsCountry): Flow<ArticleObservation<Article?>> {
        return flow {
            emitAll(localDataSource.observeById(id.value, country))
        }
            .map { entity ->
                val article = entity?.let { ArticleMapper.mapToDomain(it) }
                val observation: ArticleObservation<Article?> = ArticleObservation.Data(article)
                observation
            }
            .catch { e ->
                if (e is CancellationException) throw e
                emit(ArticleObservation.Failure(SyncError.Persistence))
            }
    }

    override suspend fun refreshArticles(country: NewsCountry): RefreshResult {
        val generation = advanceGeneration(country)
        return mutexFor(country).withLock {
            val remoteResponse = try {
                remoteDataSource.fetchTopHeadlines(country, page = 1, pageSize = pageSize)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (generation != currentGeneration(country)) {
                    return@withLock RefreshResult.Success
                }
                return@withLock RefreshResult.Failure(mapToSyncError(e))
            }

            if (generation != currentGeneration(country)) {
                return@withLock RefreshResult.Success
            }

            if (remoteResponse.status != "ok") {
                return@withLock RefreshResult.Failure(SyncError.RemoteApi)
            }

            val totalResults = remoteResponse.totalResults
                ?: return@withLock RefreshResult.Failure(SyncError.MalformedData)
            if (totalResults < 0) {
                return@withLock RefreshResult.Failure(SyncError.MalformedData)
            }

            val remoteArticles = remoteResponse.articles
                ?: return@withLock RefreshResult.Failure(SyncError.MalformedData)

            val entities = try {
                when (val mappingResult = ArticleMapper.mapToDomain(remoteArticles)) {
                    is ArticleMappingResult.Success -> {
                        ArticleMapper.mapToEntities(mappingResult.articles, country)
                    }
                    is ArticleMappingResult.MalformedData -> {
                        return@withLock RefreshResult.Failure(SyncError.MalformedData)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val error = if (e is IllegalArgumentException) {
                    SyncError.MalformedData
                } else {
                    SyncError.Unknown
                }
                return@withLock RefreshResult.Failure(error)
            }

            try {
                if (generation != currentGeneration(country)) {
                    return@withLock RefreshResult.Success
                }
                localDataSource.replaceSnapshot(entities, country)
                pagingStates.update { states ->
                    states + (country to PagingState(
                        currentPage = 1,
                        totalResults = totalResults,
                        hasMore = totalResults > pageSize,
                    ))
                }
                RefreshResult.Success
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                RefreshResult.Failure(SyncError.Persistence)
            }
        }
    }

    override suspend fun loadMoreArticles(country: NewsCountry): RefreshResult {
        val generation = currentGeneration(country)
        return mutexFor(country).withLock {
            if (generation != currentGeneration(country)) {
                return@withLock RefreshResult.Success
            }
            val currentState = pagingStates.value[country]
                ?: return@withLock RefreshResult.Success
            if (!currentState.hasMore) return@withLock RefreshResult.Success

            val nextPage = currentState.currentPage + 1
            val remoteResponse = try {
                remoteDataSource.fetchTopHeadlines(country, page = nextPage, pageSize = pageSize)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (generation != currentGeneration(country)) {
                    return@withLock RefreshResult.Success
                }
                return@withLock RefreshResult.Failure(mapToSyncError(e))
            }

            if (generation != currentGeneration(country)) {
                return@withLock RefreshResult.Success
            }

            if (remoteResponse.status != "ok") {
                return@withLock RefreshResult.Failure(SyncError.RemoteApi)
            }

            val totalResults = remoteResponse.totalResults
                ?: return@withLock RefreshResult.Failure(SyncError.MalformedData)
            if (totalResults < 0) {
                return@withLock RefreshResult.Failure(SyncError.MalformedData)
            }

            val remoteArticles = remoteResponse.articles
                ?: return@withLock RefreshResult.Failure(SyncError.MalformedData)

            val entities = try {
                when (val mappingResult = ArticleMapper.mapToDomain(remoteArticles)) {
                    is ArticleMappingResult.Success -> {
                        ArticleMapper.mapToEntities(mappingResult.articles, country)
                    }
                    is ArticleMappingResult.MalformedData -> {
                        return@withLock RefreshResult.Failure(SyncError.MalformedData)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val error = if (e is IllegalArgumentException) {
                    SyncError.MalformedData
                } else {
                    SyncError.Unknown
                }
                return@withLock RefreshResult.Failure(error)
            }

            try {
                localDataSource.appendArticles(entities, country)
                if (generation != currentGeneration(country)) {
                    return@withLock RefreshResult.Success
                }
                pagingStates.update { states ->
                    states + (country to currentState.copy(
                        currentPage = nextPage,
                        totalResults = totalResults,
                        hasMore = (nextPage * pageSize) < totalResults,
                    ))
                }
                RefreshResult.Success
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                RefreshResult.Failure(SyncError.Persistence)
            }
        }
    }

    override fun canLoadMore(country: NewsCountry): Boolean {
        return pagingStates.value[country]?.hasMore == true
    }

    private fun mapToSyncError(e: Exception): SyncError {
        return when (e) {
            is ResponseException -> SyncError.RemoteApi
            is SerializationException -> SyncError.MalformedData
            is IOException -> SyncError.Network
            else -> SyncError.Unknown
        }
    }
}
