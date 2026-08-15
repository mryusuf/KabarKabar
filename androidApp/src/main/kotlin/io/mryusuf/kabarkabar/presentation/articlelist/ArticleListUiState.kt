package io.mryusuf.kabarkabar.presentation.articlelist

import io.mryusuf.kabarkabar.domain.model.Article

/** Durable content state for the article list. */
sealed interface ArticleListContent {
    data object Loading : ArticleListContent

    data class Data(val articles: List<Article>) : ArticleListContent {
        init {
            require(articles.isNotEmpty()) {
                "Data requires at least one article; use Empty for zero articles"
            }
        }
    }

    data object Empty : ArticleListContent

    data class Error(val error: ArticleListUiError) : ArticleListContent
}

/** Presentation-safe categories mapped from domain outcomes by the ViewModel. */
enum class ArticleListUiError {
    NetworkUnavailable,
    RemoteUnavailable,
    PersistenceUnavailable,
    MalformedData,
    Unknown,
}

/** Refreshing is orthogonal to the durable content state. */
data class ArticleListUiState(
    val content: ArticleListContent = ArticleListContent.Loading,
    val isRefreshing: Boolean = false,
)
