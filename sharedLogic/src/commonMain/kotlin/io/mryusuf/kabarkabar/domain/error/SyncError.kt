package io.mryusuf.kabarkabar.domain.error

/**
 * Domain-level vocabulary for article synchronization and persistence failures.
 *
 * Encapsulates infrastructure failures into structured, domain-safe categories
 * without exposing raw network, database, or platform exceptions.
 */
sealed interface SyncError {
    /**
     * Network connectivity failure (e.g. device offline, DNS resolution failure, connection timeout).
     */
    data object Network : SyncError

    /**
     * Remote API error (e.g. HTTP 4xx, 5xx, rate limiting, server error).
     */
    data object RemoteApi : SyncError

    /**
     * Local storage or persistence failure (e.g. database disk I/O, transaction failure).
     */
    data object Persistence : SyncError

    /**
     * Malformed, unparseable, or unusable payload returned from remote source.
     */
    data object MalformedData : SyncError

    /**
     * Any unknown or unexpected synchronization error.
     */
    data object Unknown : SyncError
}
