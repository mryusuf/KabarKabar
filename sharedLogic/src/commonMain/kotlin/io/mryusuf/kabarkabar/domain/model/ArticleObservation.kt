package io.mryusuf.kabarkabar.domain.model

import io.mryusuf.kabarkabar.domain.error.SyncError

/**
 * Domain-safe outcome emitted while observing persisted article data.
 *
 * An empty [Data] value is a valid committed empty snapshot. [Failure] is a
 * local observation failure and must never be represented as an empty list.
 */
sealed interface ArticleObservation<out T> {
    data class Data<T>(val value: T) : ArticleObservation<T>

    data class Failure(val error: SyncError) : ArticleObservation<Nothing>
}
