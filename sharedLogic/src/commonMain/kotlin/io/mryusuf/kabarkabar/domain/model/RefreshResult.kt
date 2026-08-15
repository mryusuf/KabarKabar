package io.mryusuf.kabarkabar.domain.model

import io.mryusuf.kabarkabar.domain.error.SyncError

/**
 * Result of a synchronization / refresh operation.
 */
sealed interface RefreshResult {
    /**
     * Refresh succeeded and any fresh headline snapshot was committed.
     */
    data object Success : RefreshResult

    /**
     * Refresh failed with a domain-classified [error].
     */
    data class Failure(val error: SyncError) : RefreshResult
}
