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
) : ArticleRepository {
    private val countryObservations =
        mutableMapOf<NewsCountry, MutableStateFlow<ArticleObservation<List<Article>>>>()

    private fun getOrCreateObservation(country: NewsCountry, initial: List<Article> = emptyList()) =
        countryObservations.getOrPut(country) {
            MutableStateFlow(ArticleObservation.Data(initial))
        }

    init {
        getOrCreateObservation(NewsCountry.US, initialArticles)
    }

    var refreshResult: RefreshResult = initialRefreshResult
    var refreshCallCount: Int = 0
        private set

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
        return refreshResult
    }

    fun emit(articles: List<Article>, country: NewsCountry = NewsCountry.US) {
        getOrCreateObservation(country).value = ArticleObservation.Data(articles)
    }

    fun emitFailure(error: SyncError, country: NewsCountry = NewsCountry.US) {
        getOrCreateObservation(country).value = ArticleObservation.Failure(error)
    }
}
