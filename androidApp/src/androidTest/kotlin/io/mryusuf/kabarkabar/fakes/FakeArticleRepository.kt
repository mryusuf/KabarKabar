package io.mryusuf.kabarkabar.fakes

import io.mryusuf.kabarkabar.domain.error.SyncError
import io.mryusuf.kabarkabar.domain.model.Article
import io.mryusuf.kabarkabar.domain.model.ArticleId
import io.mryusuf.kabarkabar.domain.model.ArticleObservation
import io.mryusuf.kabarkabar.domain.model.NewsCountry
import io.mryusuf.kabarkabar.domain.model.RefreshResult
import io.mryusuf.kabarkabar.domain.repository.ArticleRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeArticleRepository(
    initialArticles: List<Article> = emptyList(),
    initialRefreshResult: RefreshResult = RefreshResult.Success,
    private val refreshGate: CompletableDeferred<Unit>? = null,
    private val initialHasMore: Boolean = false,
) : ArticleRepository {
    private val countryObservations =
        mutableMapOf<NewsCountry, MutableStateFlow<ArticleObservation<List<Article>>>>()

    private fun getOrCreateObservation(country: NewsCountry, initial: List<Article> = emptyList()) =
        countryObservations.getOrPut(country) {
            MutableStateFlow(ArticleObservation.Data(initial))
        }

    private val hasMoreByCountry = mutableMapOf<NewsCountry, Boolean>()

    init {
        getOrCreateObservation(NewsCountry.US, initialArticles)
        hasMoreByCountry[NewsCountry.US] = initialHasMore
    }

    var refreshResult: RefreshResult = initialRefreshResult
    var refreshCallCount: Int = 0
        private set
    var loadMoreCallCount: Int = 0
        private set
    var loadMoreResult: RefreshResult = RefreshResult.Success
    private var nextPage: List<Article>? = null
    private var hasMoreAfterLoadMore = false

    override fun observeArticles(country: NewsCountry): Flow<ArticleObservation<List<Article>>> =
        getOrCreateObservation(country)

    override fun observeArticle(id: ArticleId, country: NewsCountry): Flow<ArticleObservation<Article?>> =
        getOrCreateObservation(country).map { observation ->
            when (observation) {
                is ArticleObservation.Data -> {
                    val article = observation.value.find { it.id == id }
                    ArticleObservation.Data(article)
                }
                is ArticleObservation.Failure -> ArticleObservation.Failure(observation.error)
            }
        }

    override suspend fun refreshArticles(country: NewsCountry): RefreshResult {
        refreshCallCount += 1
        refreshGate?.await()
        if (refreshResult is RefreshResult.Success) {
            hasMoreByCountry[country] = initialHasMore
        }
        return refreshResult
    }

    override suspend fun loadMoreArticles(country: NewsCountry): RefreshResult {
        loadMoreCallCount += 1
        val result = loadMoreResult
        if (result is RefreshResult.Success) {
            nextPage?.let { page ->
                val existing = when (val observation = getOrCreateObservation(country).value) {
                    is ArticleObservation.Data -> observation.value
                    is ArticleObservation.Failure -> emptyList()
                }
                emit((existing + page).distinctBy { it.id }, country)
            }
            hasMoreByCountry[country] = hasMoreAfterLoadMore
        }
        return result
    }

    override fun canLoadMore(country: NewsCountry): Boolean = hasMoreByCountry[country] == true

    fun configureLoadMore(
        result: RefreshResult,
        articles: List<Article>? = null,
        hasMoreAfter: Boolean = false,
    ) {
        loadMoreResult = result
        nextPage = articles
        hasMoreAfterLoadMore = hasMoreAfter
    }

    fun emit(articles: List<Article>, country: NewsCountry = NewsCountry.US) {
        getOrCreateObservation(country).value = ArticleObservation.Data(articles)
    }

    fun emitFailure(error: SyncError, country: NewsCountry = NewsCountry.US) {
        getOrCreateObservation(country).value = ArticleObservation.Failure(error)
    }
}
