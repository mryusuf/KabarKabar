package io.mryusuf.kabarkabar.presentation.articlelist

import io.mryusuf.kabarkabar.domain.model.Article
import io.mryusuf.kabarkabar.domain.model.NewsCountry

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

/** Refreshing and Paging are orthogonal to the durable content state. */
data class ArticleListUiState(
    val content: ArticleListContent = ArticleListContent.Loading,
    val isRefreshing: Boolean = false,
    val isPaging: Boolean = false,
    val hasMore: Boolean = false,
    val isPagingError: Boolean = false,
    val selectedCountry: NewsCountry = NewsCountry.US,
) {
    init {
        require(!isRefreshing || content is ArticleListContent.Data) {
            "Only displayed article data can be marked as refreshing"
        }
        require(!isPaging || content is ArticleListContent.Data) {
            "Only displayed article data can be marked as paging"
        }
    }
}
