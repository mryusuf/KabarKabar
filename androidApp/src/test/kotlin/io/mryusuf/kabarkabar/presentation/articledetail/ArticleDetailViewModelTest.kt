package io.mryusuf.kabarkabar.presentation.articledetail

import io.mryusuf.kabarkabar.domain.error.SyncError
import io.mryusuf.kabarkabar.domain.model.Article
import io.mryusuf.kabarkabar.domain.model.ArticleId
import io.mryusuf.kabarkabar.domain.model.ArticleObservation
import io.mryusuf.kabarkabar.domain.model.NewsCountry
import io.mryusuf.kabarkabar.domain.model.RefreshResult
import io.mryusuf.kabarkabar.domain.repository.ArticleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class ArticleDetailViewModelTest {

    @Test
    fun persisted_read_failure_maps_to_presentation_safe_error() = runTest {
        val articleId = ArticleId.fromCanonicalUrl("https://example.com/article")
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository(ArticleObservation.Failure(SyncError.Persistence)),
            articleId = articleId,
            country = NewsCountry.US,
        )

        val state = viewModel.uiState.first { it !is ArticleDetailUiState.Loading }

        assertEquals(
            ArticleDetailUiState.Error(ArticleDetailUiError.PersistenceUnavailable),
            state,
        )
    }

    @Test
    fun missing_persisted_article_maps_to_not_found() = runTest {
        val articleId = ArticleId.fromCanonicalUrl("https://example.com/missing")
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository(ArticleObservation.Data(null)),
            articleId = articleId,
            country = NewsCountry.US,
        )

        val state = viewModel.uiState.first { it !is ArticleDetailUiState.Loading }

        assertEquals(ArticleDetailUiState.NotFound, state)
    }
}

private class FakeArticleRepository(
    private val observation: ArticleObservation<Article?>,
) : ArticleRepository {
    override fun observeArticles(country: NewsCountry): Flow<ArticleObservation<List<Article>>> =
        flowOf(ArticleObservation.Data(emptyList()))

    override fun observeArticle(id: ArticleId, country: NewsCountry): Flow<ArticleObservation<Article?>> =
        flowOf(observation)

    override suspend fun refreshArticles(country: NewsCountry): RefreshResult = RefreshResult.Success
}
