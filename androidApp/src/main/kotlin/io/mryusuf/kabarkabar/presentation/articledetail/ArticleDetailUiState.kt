package io.mryusuf.kabarkabar.presentation.articledetail

import io.mryusuf.kabarkabar.domain.model.Article

/** Durable content state for the article detail screen. */
sealed interface ArticleDetailUiState {
    data object Loading : ArticleDetailUiState
    data class Data(val article: Article) : ArticleDetailUiState
    data object NotFound : ArticleDetailUiState
    data class Error(val error: ArticleDetailUiError) : ArticleDetailUiState
}

enum class ArticleDetailUiError {
    NetworkUnavailable,
    RemoteUnavailable,
    PersistenceUnavailable,
    MalformedData,
    Unknown,
}
