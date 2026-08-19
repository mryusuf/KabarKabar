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

    val articleWithImage = Article(
        id = ArticleId.fromCanonicalUrl("https://example.com/image"),
        url = "https://example.com/image",
        title = "Article with Image",
        description = "This article has a hero image",
        imageUrl = "https://example.com/hero.jpg",
        publishedAt = EpochMilliseconds(1723957200000L)
    )

    val articleWithBlankImageUrl = Article(
        id = ArticleId.fromCanonicalUrl("https://example.com/blank-image"),
        url = "https://example.com/blank-image",
        title = "Article with Blank Image URL",
        description = "This article has no usable hero image URL",
        imageUrl = "   ",
        publishedAt = EpochMilliseconds(1723960800000L)
    )

    val articleWithMalformedImageUrl = Article(
        id = ArticleId.fromCanonicalUrl("https://example.com/malformed-image"),
        url = "https://example.com/malformed-image",
        title = "Article with Malformed Image URL",
        description = "This article has an unsupported hero image URL",
        imageUrl = "not-a-url",
        publishedAt = EpochMilliseconds(1723964400000L)
    )
}
