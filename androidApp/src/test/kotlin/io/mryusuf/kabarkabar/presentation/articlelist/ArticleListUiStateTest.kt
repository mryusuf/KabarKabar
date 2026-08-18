package io.mryusuf.kabarkabar.presentation.articlelist

import io.mryusuf.kabarkabar.domain.model.Article
import io.mryusuf.kabarkabar.domain.model.EpochMilliseconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleListUiStateTest {

    @Test
    fun default_state_starts_in_initial_loading() {
        val state = ArticleListUiState()

        assertEquals(ArticleListContent.Loading, state.content)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun refreshing_keeps_cached_data_visible() {
        val state = ArticleListUiState(
            content = ArticleListContent.Data(
                listOf(
                    Article.fromCanonicalUrl(
                        url = "https://example.com/news/one",
                        title = "Title",
                        description = null,
                        imageUrl = null,
                        publishedAt = EpochMilliseconds(0L)
                    )
                )
            ),
            isRefreshing = true
        )

        assertTrue(state.content is ArticleListContent.Data)
        assertTrue(state.isRefreshing)
    }

    @Test
    fun data_cannot_represent_an_empty_snapshot() {
        assertThrows(IllegalArgumentException::class.java) {
            ArticleListContent.Data(emptyList())
        }
    }

    @Test
    fun refreshing_cannot_be_combined_with_non_data_content() {
        assertThrows(IllegalArgumentException::class.java) {
            ArticleListUiState(
                content = ArticleListContent.Loading,
                isRefreshing = true,
            )
        }
    }

    @Test
    fun empty_and_error_are_distinct_content_states() {
        val empty = ArticleListContent.Empty
        val error = ArticleListContent.Error(ArticleListUiError.NetworkUnavailable)

        assertFalse(empty == error)
    }
}
