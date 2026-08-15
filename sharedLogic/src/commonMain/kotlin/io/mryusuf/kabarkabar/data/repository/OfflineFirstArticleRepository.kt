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
import io.mryusuf.kabarkabar.domain.repository.ArticleRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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

    private val refreshMutex = Mutex()

    override fun observeArticles(): Flow<ArticleObservation<List<Article>>> {
        return flow {
            emitAll(localDataSource.observeAll())
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

    override fun observeArticle(id: ArticleId): Flow<ArticleObservation<Article?>> {
        return flow {
            emitAll(localDataSource.observeById(id.value))
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

    override suspend fun refreshArticles(): RefreshResult {
        return refreshMutex.withLock {
            val remoteResponse = try {
                remoteDataSource.fetchTopHeadlines(country = "id")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                return@withLock RefreshResult.Failure(mapToSyncError(e))
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
                    is ArticleMappingResult.Success -> ArticleMapper.mapToEntities(mappingResult.articles)
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
                localDataSource.replaceSnapshot(entities)
                RefreshResult.Success
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                RefreshResult.Failure(SyncError.Persistence)
            }
        }
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
