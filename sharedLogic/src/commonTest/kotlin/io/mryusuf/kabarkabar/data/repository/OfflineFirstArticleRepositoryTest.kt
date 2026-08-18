package io.mryusuf.kabarkabar.data.repository

import io.mryusuf.kabarkabar.data.local.db.LocalArticleDataSource
import io.mryusuf.kabarkabar.data.local.entity.ArticleEntity
import io.mryusuf.kabarkabar.data.mapper.ArticleMapper
import io.mryusuf.kabarkabar.data.remote.api.RemoteArticleDataSource
import io.mryusuf.kabarkabar.data.remote.dto.NewsApiArticleDto
import io.mryusuf.kabarkabar.data.remote.dto.NewsApiResponseDto
import io.mryusuf.kabarkabar.domain.error.SyncError
import io.mryusuf.kabarkabar.domain.model.ArticleId
import io.mryusuf.kabarkabar.domain.model.ArticleObservation
import io.mryusuf.kabarkabar.domain.model.RefreshResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OfflineFirstArticleRepositoryTest {

    private val remoteDataSource = FakeRemoteArticleDataSource()
    private val localDataSource = FakeLocalArticleDataSource()
    private val repository = OfflineFirstArticleRepository(
        remoteDataSource = remoteDataSource,
        localDataSource = localDataSource,
        country = "us",
    )

    @Test
    fun refreshArticles_success_persistsArticles() = runTest {
        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 1,
            articles = listOf(
                NewsApiArticleDto(
                    title = "Title",
                    url = "https://example.com",
                    publishedAt = "2024-03-20T12:34:56Z"
                )
            )
        )

        val result = repository.refreshArticles()

        assertEquals(RefreshResult.Success, result)
        assertEquals("us", remoteDataSource.requestedCountry)
        assertEquals(1, localDataSource.articles.value.size)
        assertEquals("Title", localDataSource.articles.value[0].title)
    }

    @Test
    fun refreshArticles_malformedData_preservesCache() = runTest {
        localDataSource.articles.value = listOf(
            ArticleEntity(
                "article-url:https://example.com/cached",
                "https://example.com/cached",
                "Title 1",
                null,
                null,
                1L
            )
        )
        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 1,
            articles = listOf(
                NewsApiArticleDto(
                    title = "",
                    url = "https://example.com",
                    publishedAt = "2024-03-20T12:34:56Z"
                ) // Malformed
            )
        )

        val result = repository.refreshArticles()

        assertTrue(result is RefreshResult.Failure)
        assertEquals(SyncError.MalformedData, result.error)
        assertEquals(1, localDataSource.articles.value.size)
        assertEquals("Title 1", localDataSource.articles.value[0].title)
    }

    @Test
    fun refreshArticles_remoteApiError_preservesCache() = runTest {
        localDataSource.articles.value = listOf(cachedArticle())
        remoteDataSource.response = NewsApiResponseDto(
            status = "error",
            totalResults = null,
            articles = null
        )

        val result = repository.refreshArticles()

        assertEquals(RefreshResult.Failure(SyncError.RemoteApi), result)
        assertEquals(listOf(cachedArticle()), localDataSource.articles.value)
    }

    @Test
    fun refreshArticles_validEmptySnapshot_replacesCache() = runTest {
        localDataSource.articles.value = listOf(cachedArticle())
        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 0,
            articles = emptyList()
        )

        val result = repository.refreshArticles()

        assertEquals(RefreshResult.Success, result)
        assertTrue(localDataSource.articles.value.isEmpty())
    }

    @Test
    fun refreshArticles_missingSuccessfulResponseFields_preservesCacheAsMalformed() = runTest {
        localDataSource.articles.value = listOf(cachedArticle())
        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = null,
            articles = null
        )

        val result = repository.refreshArticles()

        assertEquals(RefreshResult.Failure(SyncError.MalformedData), result)
        assertEquals(listOf(cachedArticle()), localDataSource.articles.value)
    }

    @Test
    fun refreshArticles_networkFailure_preservesCacheAndClassifiesNetwork() = runTest {
        localDataSource.articles.value = listOf(cachedArticle())
        remoteDataSource.exception = IOException("offline")

        val result = repository.refreshArticles()

        assertEquals(RefreshResult.Failure(SyncError.Network), result)
        assertEquals(listOf(cachedArticle()), localDataSource.articles.value)
    }

    @Test
    fun refreshArticles_localFailureIsPersistenceErrorAndPreservesCache() = runTest {
        localDataSource.articles.value = listOf(cachedArticle())
        localDataSource.replaceException = IllegalStateException("write failed")
        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 1,
            articles = listOf(
                NewsApiArticleDto(
                    title = "Fresh",
                    url = "https://example.com/fresh",
                    publishedAt = "2024-03-20T12:34:56Z"
                )
            )
        )

        val result = repository.refreshArticles()

        assertEquals(RefreshResult.Failure(SyncError.Persistence), result)
        assertEquals(listOf(cachedArticle()), localDataSource.articles.value)
    }

    @Test
    fun refreshArticles_rethrowsCancellation() = runTest {
        remoteDataSource.exception = CancellationException("cancelled")

        assertFailsWith<CancellationException> {
            repository.refreshArticles()
        }
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun refreshArticles_serializesNetworkAndPersistenceSoLaterRefreshWins() = runTest {
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val firstResponse = NewsApiResponseDto(
            status = "ok",
            totalResults = 1,
            articles = listOf(
                NewsApiArticleDto(
                    title = "Older",
                    url = "https://example.com/older",
                    publishedAt = "2024-03-20T12:34:56Z"
                )
            )
        )
        val secondResponse = NewsApiResponseDto(
            status = "ok",
            totalResults = 1,
            articles = listOf(
                NewsApiArticleDto(
                    title = "Newer",
                    url = "https://example.com/newer",
                    publishedAt = "2024-03-21T12:34:56Z"
                )
            )
        )
        remoteDataSource.fetchHandler = { callNumber ->
            if (callNumber == 1) {
                firstStarted.complete(Unit)
                releaseFirst.await()
                firstResponse
            } else {
                secondResponse
            }
        }

        val firstRefresh = async { repository.refreshArticles() }
        firstStarted.await()
        val secondRefresh = async { repository.refreshArticles() }
        runCurrent()

        assertEquals(1, remoteDataSource.fetchCount)

        releaseFirst.complete(Unit)
        assertEquals(RefreshResult.Success, firstRefresh.await())
        assertEquals(RefreshResult.Success, secondRefresh.await())
        assertEquals("Newer", localDataSource.articles.value.single().title)
    }

    @Test
    fun observeArticles_emitsFromLocal() = runTest {
        localDataSource.articles.value = listOf(
            ArticleEntity(
                "article-url:https://example.com",
                "https://example.com",
                "Title",
                null,
                null,
                0L
            )
        )

        val observation = repository.observeArticles().first()

        assertTrue(observation is ArticleObservation.Data)
        assertEquals(1, observation.value.size)
        assertEquals("Title", observation.value[0].title)
    }

    @Test
    fun observeArticles_localSourceCreationFailureBecomesPersistenceFailure() = runTest {
        localDataSource.observeException = IllegalStateException("read failed")

        val observation = repository.observeArticles().first()

        assertEquals(ArticleObservation.Failure(SyncError.Persistence), observation)
    }

    @Test
    fun observeArticle_readsPersistedArticleByStableId() = runTest {
        val entity = cachedArticle()
        localDataSource.articles.value = listOf(entity)

        val observation = repository.observeArticle(ArticleId.fromValue(entity.id)).first()

        assertEquals(
            ArticleObservation.Data(
                ArticleMapper.mapToDomain(entity)
            ),
            observation
        )
    }

    @Test
    fun observeArticle_emitsDataNullForMissingArticle() = runTest {
        val observation = repository
            .observeArticle(ArticleId.fromCanonicalUrl("https://example.com/missing"))
            .first()

        assertEquals(ArticleObservation.Data(null), observation)
    }

    private fun cachedArticle(): ArticleEntity = ArticleEntity(
        "article-url:https://example.com/cached",
        "https://example.com/cached",
        "Cached",
        null,
        null,
        1L
    )
}

class FakeRemoteArticleDataSource : RemoteArticleDataSource {
    var response: NewsApiResponseDto? = null
    var exception: Exception? = null
    var requestedCountry: String? = null
    var fetchCount: Int = 0
    var fetchHandler: (suspend (Int) -> NewsApiResponseDto)? = null

    override suspend fun fetchTopHeadlines(country: String): NewsApiResponseDto {
        requestedCountry = country
        fetchCount += 1
        fetchHandler?.let { return it(fetchCount) }
        exception?.let { throw it }
        return response ?: NewsApiResponseDto("ok", 0, emptyList())
    }
}

class FakeLocalArticleDataSource : LocalArticleDataSource {
    val articles = MutableStateFlow<List<ArticleEntity>>(emptyList())
    var replaceException: Exception? = null
    var observeException: Exception? = null

    override fun observeAll(): Flow<List<ArticleEntity>> {
        observeException?.let { throw it }
        return articles
    }

    override fun observeById(id: String): Flow<ArticleEntity?> {
        observeException?.let { throw it }
        return articles.map { list -> list.find { it.id == id } }
    }

    override suspend fun replaceSnapshot(articles: List<ArticleEntity>) {
        replaceException?.let { throw it }
        this.articles.value = articles
    }
}
