package io.mryusuf.kabarkabar.fakes

import io.mryusuf.kabarkabar.domain.error.SyncError
import io.mryusuf.kabarkabar.domain.model.Article
import io.mryusuf.kabarkabar.domain.model.ArticleId
import io.mryusuf.kabarkabar.domain.model.ArticleObservation
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
    private val _articles = MutableStateFlow<ArticleObservation<List<Article>>>(
        ArticleObservation.Data(initialArticles)
    )

    var refreshResult: RefreshResult = initialRefreshResult
    var refreshCallCount: Int = 0
        private set

    override fun observeArticles(): Flow<ArticleObservation<List<Article>>> = _articles

    override fun observeArticle(id: ArticleId): Flow<ArticleObservation<Article?>> =
        _articles.map { observation ->
            when (observation) {
                is ArticleObservation.Data -> {
                    val article = observation.value.find { it.id == id }
                    ArticleObservation.Data(article)
                }
                is ArticleObservation.Failure -> ArticleObservation.Failure(observation.error)
            }
        }

    override suspend fun refreshArticles(): RefreshResult {
        refreshCallCount += 1
        refreshGate?.await()
        return refreshResult
    }

    fun emit(articles: List<Article>) {
        _articles.value = ArticleObservation.Data(articles)
    }

    fun emitFailure(error: SyncError) {
        _articles.value = ArticleObservation.Failure(error)
    }
}
