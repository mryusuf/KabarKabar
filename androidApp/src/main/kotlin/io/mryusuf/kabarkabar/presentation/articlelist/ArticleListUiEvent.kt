package io.mryusuf.kabarkabar.presentation.articlelist

/** One-shot UI effects that should not be persisted in StateFlow. */
sealed interface ArticleListUiEvent {
    /** Non-blocking error typically shown as a Snackbar during background refresh. */
    data class ShowRefreshError(val error: ArticleListUiError) : ArticleListUiEvent

    /** Non-blocking local-observation error while previously displayed data is retained. */
    data class ShowObservationError(val error: ArticleListUiError) : ArticleListUiEvent
}
