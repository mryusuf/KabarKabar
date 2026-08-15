package io.mryusuf.kabarkabar.domain.repository

import io.mryusuf.kabarkabar.domain.model.Article
import io.mryusuf.kabarkabar.domain.model.ArticleId
import io.mryusuf.kabarkabar.domain.model.ArticleObservation
import io.mryusuf.kabarkabar.domain.model.RefreshResult
import kotlinx.coroutines.flow.Flow

/**
 * Contract for managing and observing persisted news articles.
 *
 * This is the exclusive boundary exposed to the presentation layer for data operations.
 * It maps infrastructure failures into domain-level outcomes and exposes
 * persisted data as streams of domain [Article] models.
 */
interface ArticleRepository {
    /**
     * Observes a stream of all currently persisted articles.
     *
     * Following the offline-first principle, this observes local source of truth only.
     */
    fun observeArticles(): Flow<ArticleObservation<List<Article>>>

    /**
     * Observes a single persisted article by its stable deterministic [id].
     *
     * Emits [ArticleObservation.Data] with null if no article with the given ID exists;
     * local read failures are emitted as [ArticleObservation.Failure].
     */
    fun observeArticle(id: ArticleId): Flow<ArticleObservation<Article?>>

    /**
     * Triggers a remote synchronization.
     *
     * Fetches fresh content from the remote source, validates/maps it, and transactionally
     * replaces the local headline snapshot on success. Failure to synchronize preserves
     * the existing cache.
     *
     * @return [RefreshResult] classifying the outcome into domain-safe categories.
     */
    suspend fun refreshArticles(): RefreshResult
}
