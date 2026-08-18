package io.mryusuf.kabarkabar.presentation.articlelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.mryusuf.kabarkabar.domain.error.SyncError
import io.mryusuf.kabarkabar.domain.model.Article
import io.mryusuf.kabarkabar.domain.model.ArticleObservation
import io.mryusuf.kabarkabar.domain.model.RefreshResult
import io.mryusuf.kabarkabar.domain.repository.ArticleRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Orchestrates article-list presentation state from independent persisted-data
 * observation and synchronization-result streams.
 */
class ArticleListViewModel(
    private val repository: ArticleRepository
) : ViewModel() {

    private data class ObservedArticles(
        val latest: ArticleObservation<List<Article>>? = null,
        /** The most recent Data value; an empty list remains meaningful. */
        val lastData: List<Article>? = null,
    ) {
        fun record(observation: ArticleObservation<List<Article>>) =
            copy(
                latest = observation,
                lastData = when (observation) {
                    is ArticleObservation.Data -> observation.value
                    is ArticleObservation.Failure -> lastData
                },
            )
    }

    private data class PendingRefreshFailure(
        val attemptId: Long,
        val error: SyncError,
        val eventDelivered: Boolean,
    )

    private data class SyncState(
        val attemptId: Long? = null,
        val isRunning: Boolean = false,
        val initialSyncCompleted: Boolean = false,
        val lastError: SyncError? = null,
        val pendingFailure: PendingRefreshFailure? = null,
    )

    private val observedArticles = MutableStateFlow(ObservedArticles())
    private val syncState = MutableStateFlow(SyncState())
    private val eventsChannel = Channel<ArticleListUiEvent>(capacity = Channel.BUFFERED)
    private var nextAttemptId = 0L

    /** One-shot effects are buffered until a collector receives them and never replayed. */
    val events = eventsChannel.receiveAsFlow()

    /**
     * Room observation is owned by the ViewModel for its whole lifecycle. This keeps the
     * latest persisted snapshot available even before a UI collector subscribes and avoids
     * using the exposed UI StateFlow as a cache oracle.
     */
    val uiState: StateFlow<ArticleListUiState> = combine(
        observedArticles,
        syncState,
    ) { observed, sync ->
        reduce(observed, sync)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ArticleListUiState(),
    )

    init {
        viewModelScope.launch {
            repository.observeArticles().collect(::recordObservation)
        }
        refresh()
    }

    /** Starts one synchronization unless another one is already in progress. */
    fun refresh() {
        val currentState = syncState.value
        if (currentState.isRunning) return

        val attemptId = ++nextAttemptId
        syncState.value = currentState.copy(
            attemptId = attemptId,
            isRunning = true,
            lastError = null,
            pendingFailure = null,
        )

        viewModelScope.launch {
            try {
                val result = try {
                    repository.refreshArticles()
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    RefreshResult.Failure(SyncError.Unknown)
                }
                recordRefreshResult(attemptId, result)
            } finally {
                syncState.update { state ->
                    if (state.attemptId == attemptId) {
                        state.copy(isRunning = false)
                    } else {
                        state
                    }
                }
            }
        }
    }

    private fun recordObservation(observation: ArticleObservation<List<Article>>) {
        val previous = observedArticles.value
        observedArticles.value = previous.record(observation)

        if (
            observation is ArticleObservation.Failure &&
            previous.latest !is ArticleObservation.Failure &&
            previous.lastData?.isNotEmpty() == true
        ) {
            sendEvent(ArticleListUiEvent.ShowObservationError(mapToUiError(observation.error)))
        }

        deliverPendingRefreshErrorIfCacheExists()
    }

    private fun recordRefreshResult(attemptId: Long, result: RefreshResult) {
        val current = syncState.value
        if (current.attemptId != attemptId) return

        when (result) {
            RefreshResult.Success -> {
                syncState.value = current.copy(
                    initialSyncCompleted = true,
                    lastError = null,
                    pendingFailure = null,
                )
            }

            is RefreshResult.Failure -> {
                val hasCache = hasUsableCache()
                val pendingFailure = PendingRefreshFailure(
                    attemptId = attemptId,
                    error = result.error,
                    eventDelivered = hasCache && sendEvent(
                        ArticleListUiEvent.ShowRefreshError(mapToUiError(result.error))
                    ),
                )
                syncState.value = current.copy(
                    initialSyncCompleted = true,
                    lastError = result.error,
                    pendingFailure = pendingFailure,
                )
            }
        }
    }

    private fun deliverPendingRefreshErrorIfCacheExists() {
        val current = syncState.value
        val pendingFailure = current.pendingFailure ?: return
        if (pendingFailure.eventDelivered || !hasUsableCache()) return

        val delivered = sendEvent(
            ArticleListUiEvent.ShowRefreshError(mapToUiError(pendingFailure.error))
        )
        syncState.update { state ->
            if (state.pendingFailure?.attemptId == pendingFailure.attemptId) {
                state.copy(
                    pendingFailure = pendingFailure.copy(eventDelivered = delivered)
                )
            } else {
                state
            }
        }
    }

    private fun hasUsableCache(): Boolean =
        observedArticles.value.lastData?.isNotEmpty() == true

    private fun reduce(
        observed: ObservedArticles,
        sync: SyncState,
    ): ArticleListUiState {
        val content = when (val latest = observed.latest) {
            null -> when {
                sync.isRunning || !sync.initialSyncCompleted -> ArticleListContent.Loading
                sync.lastError != null -> ArticleListContent.Error(mapToUiError(sync.lastError))
                else -> ArticleListContent.Loading
            }

            is ArticleObservation.Data -> when {
                latest.value.isNotEmpty() -> ArticleListContent.Data(latest.value)
                sync.isRunning || !sync.initialSyncCompleted -> ArticleListContent.Loading
                sync.lastError != null -> ArticleListContent.Error(mapToUiError(sync.lastError))
                else -> ArticleListContent.Empty
            }

            is ArticleObservation.Failure -> {
                observed.lastData
                    ?.takeIf { it.isNotEmpty() }
                    ?.let(ArticleListContent::Data)
                    ?: ArticleListContent.Error(mapToUiError(latest.error))
            }
        }

        return ArticleListUiState(
            content = content,
            isRefreshing = sync.isRunning && content is ArticleListContent.Data,
        )
    }

    private fun sendEvent(event: ArticleListUiEvent): Boolean =
        eventsChannel.trySend(event).isSuccess

    private fun mapToUiError(error: SyncError): ArticleListUiError = when (error) {
        SyncError.Network -> ArticleListUiError.NetworkUnavailable
        SyncError.RemoteApi -> ArticleListUiError.RemoteUnavailable
        SyncError.Persistence -> ArticleListUiError.PersistenceUnavailable
        SyncError.MalformedData -> ArticleListUiError.MalformedData
        SyncError.Unknown -> ArticleListUiError.Unknown
    }
}
