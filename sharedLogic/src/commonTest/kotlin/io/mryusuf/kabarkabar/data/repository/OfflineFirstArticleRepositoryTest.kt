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
import io.mryusuf.kabarkabar.domain.model.NewsCountry
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

        val result = repository.refreshArticles(NewsCountry.ID)

        assertEquals(RefreshResult.Success, result)
        assertEquals("id", remoteDataSource.requestedCountry)
        assertEquals(1, remoteDataSource.requestedPage)
        assertEquals(20, remoteDataSource.requestedPageSize)
        assertEquals(1, localDataSource.articles.value.size)
        assertEquals("id", localDataSource.articles.value[0].countryCode)
        assertEquals("Title", localDataSource.articles.value[0].title)
    }

    @Test
    fun country_scoped_list_and_detail_reads_keep_same_id_separate() = runTest {
        val usArticle = articleEntity(NewsCountry.US, "US headline")
        val idArticle = articleEntity(NewsCountry.ID, "ID headline")
        localDataSource.articles.value = listOf(usArticle, idArticle)
        val sharedId = ArticleId.fromValue(usArticle.id)

        val usList = repository.observeArticles(NewsCountry.US).first()
        val idList = repository.observeArticles(NewsCountry.ID).first()
        val usDetail = repository.observeArticle(sharedId, NewsCountry.US).first()
        val idDetail = repository.observeArticle(sharedId, NewsCountry.ID).first()

        assertEquals(listOf("US headline"), (usList as ArticleObservation.Data).value.map { it.title })
        assertEquals(listOf("ID headline"), (idList as ArticleObservation.Data).value.map { it.title })
        assertEquals("US headline", (usDetail as ArticleObservation.Data).value?.title)
        assertEquals("ID headline", (idDetail as ArticleObservation.Data).value?.title)
    }

    @Test
    fun US_refresh_replaces_only_the_US_snapshot() = runTest {
        val usCached = articleEntity(NewsCountry.US, "US cached")
        val idCached = articleEntity(NewsCountry.ID, "ID cached")
        localDataSource.articles.value = listOf(usCached, idCached)
        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 1,
            articles = listOf(
                NewsApiArticleDto(
                    title = "US fresh",
                    url = "https://example.com/us-fresh",
                    publishedAt = "2024-03-20T12:34:56Z",
                )
            ),
        )

        assertEquals(RefreshResult.Success, repository.refreshArticles(NewsCountry.US))

        assertEquals(
            listOf("US fresh"),
            (repository.observeArticles(NewsCountry.US).first() as ArticleObservation.Data)
                .value.map { it.title },
        )
        assertEquals(
            listOf("ID cached"),
            (repository.observeArticles(NewsCountry.ID).first() as ArticleObservation.Data)
                .value.map { it.title },
        )
    }

    @Test
    fun US_empty_snapshot_does_not_clear_ID_cache() = runTest {
        val usCached = articleEntity(NewsCountry.US, "US cached")
        val idCached = articleEntity(NewsCountry.ID, "ID cached")
        localDataSource.articles.value = listOf(usCached, idCached)
        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 0,
            articles = emptyList(),
        )

        assertEquals(RefreshResult.Success, repository.refreshArticles(NewsCountry.US))

        assertTrue(
            (repository.observeArticles(NewsCountry.US).first() as ArticleObservation.Data)
                .value.isEmpty(),
        )
        assertEquals(
            listOf("ID cached"),
            (repository.observeArticles(NewsCountry.ID).first() as ArticleObservation.Data)
                .value.map { it.title },
        )
    }

    @Test
    fun US_failure_does_not_clear_ID_cache() = runTest {
        val usCached = articleEntity(NewsCountry.US, "US cached")
        val idCached = articleEntity(NewsCountry.ID, "ID cached")
        localDataSource.articles.value = listOf(usCached, idCached)
        remoteDataSource.exception = IOException("offline")

        assertEquals(
            RefreshResult.Failure(SyncError.Network),
            repository.refreshArticles(NewsCountry.US),
        )

        assertEquals(
            listOf("US cached"),
            (repository.observeArticles(NewsCountry.US).first() as ArticleObservation.Data)
                .value.map { it.title },
        )
        assertEquals(
            listOf("ID cached"),
            (repository.observeArticles(NewsCountry.ID).first() as ArticleObservation.Data)
                .value.map { it.title },
        )
    }

    @Test
    fun ID_empty_snapshot_does_not_clear_US_cache() = runTest {
        val usCached = articleEntity(NewsCountry.US, "US cached")
        val idCached = articleEntity(NewsCountry.ID, "ID cached")
        localDataSource.articles.value = listOf(usCached, idCached)
        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 0,
            articles = emptyList(),
        )

        assertEquals(RefreshResult.Success, repository.refreshArticles(NewsCountry.ID))

        assertEquals(
            listOf("US cached"),
            (repository.observeArticles(NewsCountry.US).first() as ArticleObservation.Data)
                .value.map { it.title },
        )
        assertTrue(
            (repository.observeArticles(NewsCountry.ID).first() as ArticleObservation.Data)
                .value.isEmpty(),
        )
    }

    @Test
    fun ID_failure_does_not_clear_US_cache() = runTest {
        val usCached = articleEntity(NewsCountry.US, "US cached")
        val idCached = articleEntity(NewsCountry.ID, "ID cached")
        localDataSource.articles.value = listOf(usCached, idCached)
        remoteDataSource.exception = IOException("offline")

        assertEquals(
            RefreshResult.Failure(SyncError.Network),
            repository.refreshArticles(NewsCountry.ID),
        )

        assertEquals(
            listOf("US cached"),
            (repository.observeArticles(NewsCountry.US).first() as ArticleObservation.Data)
                .value.map { it.title },
        )
        assertEquals(
            listOf("ID cached"),
            (repository.observeArticles(NewsCountry.ID).first() as ArticleObservation.Data)
                .value.map { it.title },
        )
    }

    @Test
    fun refreshArticles_malformedData_preservesCache() = runTest {
        localDataSource.articles.value = listOf(
            ArticleEntity(
                "article-url:https://example.com/cached",
                "us",
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

        val result = repository.refreshArticles(NewsCountry.US)

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

        val result = repository.refreshArticles(NewsCountry.US)

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

        val result = repository.refreshArticles(NewsCountry.US)

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

        val result = repository.refreshArticles(NewsCountry.US)

        assertEquals(RefreshResult.Failure(SyncError.MalformedData), result)
        assertEquals(listOf(cachedArticle()), localDataSource.articles.value)
    }

    @Test
    fun refreshArticles_networkFailure_preservesCacheAndClassifiesNetwork() = runTest {
        localDataSource.articles.value = listOf(cachedArticle())
        remoteDataSource.exception = IOException("offline")

        val result = repository.refreshArticles(NewsCountry.US)

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

        val result = repository.refreshArticles(NewsCountry.US)

        assertEquals(RefreshResult.Failure(SyncError.Persistence), result)
        assertEquals(listOf(cachedArticle()), localDataSource.articles.value)
    }

    @Test
    fun refreshArticles_rethrowsCancellation() = runTest {
        remoteDataSource.exception = CancellationException("cancelled")

        assertFailsWith<CancellationException> {
            repository.refreshArticles(NewsCountry.US)
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

        val firstRefresh = async { repository.refreshArticles(NewsCountry.US) }
        firstStarted.await()
        val secondRefresh = async { repository.refreshArticles(NewsCountry.US) }
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
                "us",
                "https://example.com",
                "Title",
                null,
                null,
                0L
            )
        )

        val observation = repository.observeArticles(NewsCountry.US).first()

        assertTrue(observation is ArticleObservation.Data)
        assertEquals(1, observation.value.size)
        assertEquals("Title", observation.value[0].title)
    }

    @Test
    fun observeArticles_localSourceCreationFailureBecomesPersistenceFailure() = runTest {
        localDataSource.observeException = IllegalStateException("read failed")

        val observation = repository.observeArticles(NewsCountry.US).first()

        assertEquals(ArticleObservation.Failure(SyncError.Persistence), observation)
    }

    @Test
    fun observeArticle_readsPersistedArticleByStableId() = runTest {
        val entity = cachedArticle()
        localDataSource.articles.value = listOf(entity)

        val observation = repository.observeArticle(ArticleId.fromValue(entity.id), NewsCountry.US).first()

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
            .observeArticle(
                ArticleId.fromCanonicalUrl("https://example.com/missing"),
                NewsCountry.US,
            )
            .first()

        assertEquals(ArticleObservation.Data(null), observation)
    }

    @Test
    fun loadMoreArticles_success_appendsArticlesAndUpdatesPaging() = runTest {
        // Initial refresh
        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 40,
            articles = (1..20).map { articleDto(it) }
        )
        repository.refreshArticles(NewsCountry.US)

        assertEquals(20, localDataSource.articles.value.size)
        assertTrue(repository.canLoadMore(NewsCountry.US))

        // Load more
        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 40,
            articles = (21..40).map { articleDto(it) }
        )
        val result = repository.loadMoreArticles(NewsCountry.US)

        assertEquals(RefreshResult.Success, result)
        assertEquals(2, remoteDataSource.requestedPage)
        assertEquals(40, localDataSource.articles.value.size)
        // (2 * 20) < 40 is false, so hasMore should be false
        assertEquals(false, repository.canLoadMore(NewsCountry.US))
    }

    @Test
    fun loadMoreArticles_deduplicatesByStableId() = runTest {
        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 40,
            articles = listOf(articleDto(1, title = "First title"))
        )
        repository.refreshArticles(NewsCountry.US)

        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 40,
            articles = listOf(
                articleDto(1, title = "Later duplicate title"),
                articleDto(2),
            )
        )
        repository.loadMoreArticles(NewsCountry.US)

        assertEquals(2, localDataSource.articles.value.size)
        assertEquals(
            "First title",
            localDataSource.articles.value.first { it.id == ArticleId.fromCanonicalUrl("https://example.com/1").value }
                .title,
        )
    }

    @Test
    fun canLoadMore_is_false_until_page_one_metadata_is_known() = runTest {
        assertEquals(false, repository.canLoadMore(NewsCountry.US))
    }

    @Test
    fun loadMoreArticles_rejects_negative_total_without_mutating_cache() = runTest {
        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 40,
            articles = listOf(articleDto(1)),
        )
        repository.refreshArticles(NewsCountry.US)
        val before = localDataSource.articles.value

        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = -1,
            articles = emptyList(),
        )

        assertEquals(
            RefreshResult.Failure(SyncError.MalformedData),
            repository.loadMoreArticles(NewsCountry.US),
        )
        assertEquals(before, localDataSource.articles.value)
        assertEquals(true, repository.canLoadMore(NewsCountry.US))
    }

    @Test
    fun loadMoreArticles_remoteApiError_preserves_cache_and_page_state() = runTest {
        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 40,
            articles = listOf(articleDto(1)),
        )
        repository.refreshArticles(NewsCountry.US)
        val before = localDataSource.articles.value

        remoteDataSource.response = NewsApiResponseDto(
            status = "error",
            totalResults = null,
            articles = emptyList(),
        )

        assertEquals(
            RefreshResult.Failure(SyncError.RemoteApi),
            repository.loadMoreArticles(NewsCountry.US),
        )
        assertEquals(before, localDataSource.articles.value)
        assertEquals(true, repository.canLoadMore(NewsCountry.US))
    }

    @Test
    fun loadMoreArticles_persistenceFailure_preserves_cache_and_retries_same_page() = runTest {
        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 40,
            articles = listOf(articleDto(1)),
        )
        repository.refreshArticles(NewsCountry.US)
        val before = localDataSource.articles.value

        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 40,
            articles = listOf(articleDto(2)),
        )
        localDataSource.appendException = IllegalStateException("append failed")

        assertEquals(
            RefreshResult.Failure(SyncError.Persistence),
            repository.loadMoreArticles(NewsCountry.US),
        )
        assertEquals(before, localDataSource.articles.value)
        assertEquals(true, repository.canLoadMore(NewsCountry.US))

        localDataSource.appendException = null
        assertEquals(RefreshResult.Success, repository.loadMoreArticles(NewsCountry.US))
        assertEquals(2, remoteDataSource.requestedPage)
    }

    @Test
    fun refresh_after_pagination_replaces_later_pages() = runTest {
        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 40,
            articles = listOf(articleDto(1)),
        )
        repository.refreshArticles(NewsCountry.US)

        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 40,
            articles = listOf(articleDto(2)),
        )
        repository.loadMoreArticles(NewsCountry.US)
        assertEquals(2, localDataSource.articles.value.size)

        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 1,
            articles = listOf(articleDto(3)),
        )
        assertEquals(RefreshResult.Success, repository.refreshArticles(NewsCountry.US))

        assertEquals(listOf("Article 3"), localDataSource.articles.value.map { it.title })
        assertEquals(false, repository.canLoadMore(NewsCountry.US))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun country_refresh_is_not_blocked_by_another_country_page() = runTest {
        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 40,
            articles = listOf(articleDto(1)),
        )
        repository.refreshArticles(NewsCountry.US)

        val usPageGate = CompletableDeferred<Unit>()
        remoteDataSource.fetchHandler = { requestNumber ->
            if (requestNumber == 2) {
                usPageGate.await()
            }
            NewsApiResponseDto(
                status = "ok",
                totalResults = 40,
                articles = listOf(articleDto(requestNumber)),
            )
        }

        val usPage = async { repository.loadMoreArticles(NewsCountry.US) }
        runCurrent()
        val idRefresh = async { repository.refreshArticles(NewsCountry.ID) }
        runCurrent()

        assertTrue(idRefresh.isCompleted)
        assertEquals(RefreshResult.Success, idRefresh.await())

        usPageGate.complete(Unit)
        assertEquals(RefreshResult.Success, usPage.await())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun refresh_invalidates_a_page_response_before_append() = runTest {
        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 40,
            articles = listOf(articleDto(1)),
        )
        repository.refreshArticles(NewsCountry.US)

        val pageGate = CompletableDeferred<Unit>()
        val refreshGate = CompletableDeferred<Unit>()
        remoteDataSource.fetchHandler = { requestNumber ->
            when (requestNumber) {
                2 -> {
                    pageGate.await()
                    NewsApiResponseDto(
                        status = "ok",
                        totalResults = 40,
                        articles = listOf(articleDto(2)),
                    )
                }
                3 -> {
                    refreshGate.await()
                    NewsApiResponseDto(
                        status = "ok",
                        totalResults = 1,
                        articles = listOf(articleDto(3)),
                    )
                }
                else -> error("Unexpected request $requestNumber")
            }
        }

        val page = async { repository.loadMoreArticles(NewsCountry.US) }
        runCurrent()
        val refresh = async { repository.refreshArticles(NewsCountry.US) }
        runCurrent()

        pageGate.complete(Unit)
        runCurrent()
        assertEquals(0, localDataSource.appendCallCount)
        assertEquals(listOf("Article 1"), localDataSource.articles.value.map { it.title })

        refreshGate.complete(Unit)
        assertEquals(RefreshResult.Success, page.await())
        assertEquals(RefreshResult.Success, refresh.await())
        assertEquals(listOf("Article 3"), localDataSource.articles.value.map { it.title })
    }

    @Test
    fun paging_state_is_isolated_per_country() = runTest {
        // US has more
        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 40,
            articles = (1..20).map { articleDto(it) }
        )
        repository.refreshArticles(NewsCountry.US)

        // ID has no more
        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 10,
            articles = (1..10).map { articleDto(it) }
        )
        repository.refreshArticles(NewsCountry.ID)

        assertTrue(repository.canLoadMore(NewsCountry.US))
        assertTrue(!repository.canLoadMore(NewsCountry.ID))
    }

    @Test
    fun refresh_resets_paging_state() = runTest {
        // Load more until hasMore is false
        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 20,
            articles = (1..20).map { articleDto(it) }
        )
        repository.refreshArticles(NewsCountry.US)
        assertTrue(!repository.canLoadMore(NewsCountry.US))

        // Refresh with new total results
        remoteDataSource.response = NewsApiResponseDto(
            status = "ok",
            totalResults = 100,
            articles = (1..20).map { articleDto(it) }
        )
        repository.refreshArticles(NewsCountry.US)
        assertTrue(repository.canLoadMore(NewsCountry.US))
    }

    private fun articleDto(index: Int, title: String = "Article $index"): NewsApiArticleDto {
        return NewsApiArticleDto(
            title = title,
            url = "https://example.com/$index",
            publishedAt = "2024-03-20T12:34:56Z"
        )
    }

    private fun cachedArticle(): ArticleEntity = ArticleEntity(
        "article-url:https://example.com/cached",
        "us",
        "https://example.com/cached",
        "Cached",
        null,
        null,
        1L
    )

    private fun articleEntity(country: NewsCountry, title: String): ArticleEntity {
        val url = "https://example.com/shared-country-story"
        return ArticleEntity(
            id = ArticleId.fromCanonicalUrl(url).value,
            countryCode = country.code,
            url = url,
            title = title,
            description = null,
            imageUrl = null,
            publishedAt = 1L,
        )
    }
}

class FakeRemoteArticleDataSource : RemoteArticleDataSource {
    var response: NewsApiResponseDto? = null
    var exception: Exception? = null
    var requestedCountry: String? = null
    var requestedPage: Int? = null
    var requestedPageSize: Int? = null
    var fetchCount: Int = 0
    var fetchHandler: (suspend (Int) -> NewsApiResponseDto)? = null

    override suspend fun fetchTopHeadlines(
        country: NewsCountry,
        page: Int,
        pageSize: Int,
    ): NewsApiResponseDto {
        requestedCountry = country.code
        requestedPage = page
        requestedPageSize = pageSize
        fetchCount += 1
        fetchHandler?.let { return it(fetchCount) }
        exception?.let { throw it }
        return response ?: NewsApiResponseDto("ok", 0, emptyList())
    }
}

class FakeLocalArticleDataSource : LocalArticleDataSource {
    val articles = MutableStateFlow<List<ArticleEntity>>(emptyList())
    var appendCallCount: Int = 0
    var replaceException: Exception? = null
    var appendException: Exception? = null
    var observeException: Exception? = null

    override fun observeAll(country: NewsCountry): Flow<List<ArticleEntity>> {
        observeException?.let { throw it }
        return articles.map { list -> list.filter { it.countryCode == country.code } }
    }

    override fun observeById(id: String, country: NewsCountry): Flow<ArticleEntity?> {
        observeException?.let { throw it }
        return articles.map { list -> list.find { it.id == id && it.countryCode == country.code } }
    }

    override suspend fun replaceSnapshot(articles: List<ArticleEntity>, country: NewsCountry) {
        replaceException?.let { throw it }
        val otherCountries = this.articles.value.filter { it.countryCode != country.code }
        this.articles.value = otherCountries + articles
    }

    override suspend fun appendArticles(articles: List<ArticleEntity>, country: NewsCountry) {
        appendException?.let { throw it }
        appendCallCount += 1
        require(articles.all { it.countryCode == country.code })
        val existingIds = this.articles.value
            .filter { it.countryCode == country.code }
            .map { it.id }
            .toMutableSet()
        val newArticles = articles.filter { existingIds.add(it.id) }
        this.articles.value += newArticles
    }
}
