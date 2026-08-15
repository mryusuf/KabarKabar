package io.mryusuf.kabarkabar.domain

import io.mryusuf.kabarkabar.domain.error.SyncError
import io.mryusuf.kabarkabar.domain.model.Article
import io.mryusuf.kabarkabar.domain.model.ArticleObservation
import kotlin.test.Test
import kotlin.test.assertEquals

class ArticleObservationTest {

    @Test
    fun should_keep_an_empty_successful_snapshot_distinct_from_failure() {
        val observation: ArticleObservation<List<Article>> =
            ArticleObservation.Data(emptyList())

        assertEquals(emptyList(), (observation as ArticleObservation.Data).value)
    }

    @Test
    fun should_expose_local_read_failure_as_a_domain_error() {
        val observation: ArticleObservation<List<Article>> =
            ArticleObservation.Failure(SyncError.Persistence)

        assertEquals(SyncError.Persistence, (observation as ArticleObservation.Failure).error)
    }
}
