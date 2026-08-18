package io.mryusuf.kabarkabar.presentation.articledetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.mryusuf.kabarkabar.domain.error.SyncError
import io.mryusuf.kabarkabar.domain.model.ArticleId
import io.mryusuf.kabarkabar.domain.model.ArticleObservation
import io.mryusuf.kabarkabar.domain.repository.ArticleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Observes one persisted article by stable ID.
 *
 * This ViewModel does not trigger remote fetches; it relies on the repository's
 * offline-first observation of the local database.
 */
class ArticleDetailViewModel(
    private val repository: ArticleRepository,
    articleId: ArticleId,
) : ViewModel() {

    val uiState: StateFlow<ArticleDetailUiState> = repository.observeArticle(articleId)
        .map { observation ->
            when (observation) {
                is ArticleObservation.Data -> observation.value?.let(ArticleDetailUiState::Data)
                    ?: ArticleDetailUiState.NotFound
                is ArticleObservation.Failure -> {
                    ArticleDetailUiState.Error(mapToUiError(observation.error))
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ArticleDetailUiState.Loading,
        )

    private fun mapToUiError(error: SyncError): ArticleDetailUiError = when (error) {
        SyncError.Network -> ArticleDetailUiError.NetworkUnavailable
        SyncError.RemoteApi -> ArticleDetailUiError.RemoteUnavailable
        SyncError.Persistence -> ArticleDetailUiError.PersistenceUnavailable
        SyncError.MalformedData -> ArticleDetailUiError.MalformedData
        SyncError.Unknown -> ArticleDetailUiError.Unknown
    }
}
