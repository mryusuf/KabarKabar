package io.mryusuf.kabarkabar.data.repository

import io.mryusuf.kabarkabar.data.remote.dto.NewsApiArticleDto
import io.mryusuf.kabarkabar.data.remote.dto.NewsApiResponseDto
import io.mryusuf.kabarkabar.domain.error.SyncError
import io.mryusuf.kabarkabar.domain.model.ArticleId
import io.mryusuf.kabarkabar.domain.model.ArticleObservation
import io.mryusuf.kabarkabar.domain.model.NewsCountry
import io.mryusuf.kabarkabar.domain.model.RefreshResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class OfflineFirstArticleRepositoryAcceptanceTest {

    private val remoteDataSource = FakeRemoteArticleDataSource()
    private val localDataSource = FakeLocalArticleDataSource()
    private val repository = OfflineFirstArticleRepository(
        remoteDataSource = remoteDataSource,
        localDataSource = localDataSource,
    )

    @Test
    fun t1_fetch_map_persist_expose() = runTest {
        // Given: the local store initially has no articles
        assertEquals(0, localDataSource.articles.value.size)

        // Given: the remote source returns a valid NewsAPI snapshot
        val remoteDto = NewsApiArticleDto(
            title = "Acceptance Test Article",
            url = "https://example.com/t1",
            publishedAt = "2024-03-20T12:00:00Z",
            description = "Description",
            urlToImage = "https://example.com/image.jpg"
        )
        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 1,
            articles = listOf(remoteDto)
        )

        // When: refreshArticles() succeeds
        val refreshResult = repository.refreshArticles(NewsCountry.US)
        assertEquals(RefreshResult.Success, refreshResult)
        assertEquals(1, remoteDataSource.fetchCount)

        // Then: the local boundary contains the committed snapshot before it is observed
        val persistedArticle = localDataSource.articles.value.single()
        assertEquals("us", persistedArticle.countryCode)
        assertEquals("https://example.com/t1", persistedArticle.url)
        assertEquals("Description", persistedArticle.description)
        assertEquals("https://example.com/image.jpg", persistedArticle.imageUrl)
        assertEquals(1710936000000L, persistedArticle.publishedAt)

        // Then: observeArticles() exposes the persisted domain articles
        val observation = repository.observeArticles(NewsCountry.US).first()
        assertIs<ArticleObservation.Data<*>>(observation)
        val articles = (observation as ArticleObservation.Data).value
        assertEquals(1, articles.size)

        // Then: remote DTOs are validated/mapped and exposed values come from the local source
        val article = articles[0]
        assertEquals("Acceptance Test Article", article.title)
        assertEquals("https://example.com/t1", article.url)
        assertEquals("Description", article.description)
        assertEquals("https://example.com/image.jpg", article.imageUrl)

        // Then: article identity and publication timestamp survive the round trip
        assertEquals(ArticleId.fromCanonicalUrl("https://example.com/t1"), article.id)
        assertEquals(1710936000000L, article.publishedAt.value) // 2024-03-20T12:00:00Z
    }

    @Test
    fun t2_remote_failure_cache_survives() = runTest {
        // Given: valid persisted articles already exist
        val cachedDto = NewsApiArticleDto(
            title = "Cached Article",
            url = "https://example.com/cached",
            publishedAt = "2024-03-20T10:00:00Z"
        )
        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 1,
            articles = listOf(cachedDto)
        )
        assertEquals(RefreshResult.Success, repository.refreshArticles(NewsCountry.US))

        // Given: observeArticles() exposes that cache
        val initialObservation = repository.observeArticles(NewsCountry.US).first()
        assertIs<ArticleObservation.Data<*>>(initialObservation)
        val initialArticles = (initialObservation as ArticleObservation.Data).value
        assertEquals(1, initialArticles.size)
        assertEquals("Cached Article", initialArticles[0].title)

        // When: the remote refresh fails
        remoteDataSource.response = null
        remoteDataSource.exception = IOException("Network failure")
        val refreshResult = repository.refreshArticles(NewsCountry.US)

        // Then: refreshArticles() returns the appropriate domain failure
        assertEquals(RefreshResult.Failure(SyncError.Network), refreshResult)

        // Then: the previous persisted snapshot remains unchanged
        // Then: observeArticles() continues exposing the same cached content
        val postFailureObservation = repository.observeArticles(NewsCountry.US).first()
        assertIs<ArticleObservation.Data<*>>(postFailureObservation)
        val postFailureArticles = (postFailureObservation as ArticleObservation.Data).value

        assertEquals(initialArticles, postFailureArticles)
        assertEquals(1, postFailureArticles.size)
        assertEquals("Cached Article", postFailureArticles[0].title)
    }

    @Test
    fun t3_remote_failure_plus_no_local_data_error() = runTest {
        // Given: no usable cached article data exists
        assertEquals(0, localDataSource.articles.value.size)
        val observationBefore = repository.observeArticles(NewsCountry.US).first()
        assertIs<ArticleObservation.Data<*>>(observationBefore)
        assertEquals(0, (observationBefore as ArticleObservation.Data).value.size)

        // Given: remote refresh fails
        remoteDataSource.exception = IOException("Network failure")

        // When: higher-level repository behavior is evaluated (refreshArticles)
        val refreshResult = repository.refreshArticles(NewsCountry.US)

        // Then: the failure remains represented as the appropriate domain error
        assertEquals(RefreshResult.Failure(SyncError.Network), refreshResult)

        // Then: no fake/empty article content is manufactured
        val observationAfter = repository.observeArticles(NewsCountry.US).first()
        assertIs<ArticleObservation.Data<*>>(observationAfter)
        val finalArticles = (observationAfter as ArticleObservation.Data).value
        assertEquals(0, finalArticles.size)

        // Then: empty data and error remain semantically distinct
        // (RefreshResult is Failure, Observation is Data(empty))
        assertIs<RefreshResult.Failure>(refreshResult)
        assertIs<ArticleObservation.Data<*>>(observationAfter)
    }
}
