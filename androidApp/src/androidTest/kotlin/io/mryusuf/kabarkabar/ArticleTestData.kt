package io.mryusuf.kabarkabar

import io.mryusuf.kabarkabar.domain.model.Article
import io.mryusuf.kabarkabar.domain.model.ArticleId
import io.mryusuf.kabarkabar.domain.model.EpochMilliseconds

object ArticleTestData {
    val article1 = Article(
        id = ArticleId.fromCanonicalUrl("https://example.com/1"),
        url = "https://example.com/1",
        title = "Phase 5 Test Headline",
        description = "Cached article used for offline UI acceptance testing",
        imageUrl = null,
        publishedAt = EpochMilliseconds(1723950000000L) // Some fixed timestamp
    )

    val article2 = Article(
        id = ArticleId.fromCanonicalUrl("https://example.com/2"),
        url = "https://example.com/2",
        title = "Another Test Article",
        description = "Description for another article",
        imageUrl = null,
        publishedAt = EpochMilliseconds(1723953600000L)
    )
}
