package io.mryusuf.kabarkabar.domain.model

/**
 * Domain representation of a news article.
 *
 * @property id Stable deterministic identifier derived from the canonical [url].
 * @property url Canonical source URL of the article.
 * @property title Title of the article (required and non-empty for a valid domain article).
 * @property description Optional summary/description of the article.
 * @property imageUrl Optional URL to the article's preview/hero image.
 * @property publishedAt Publication timestamp represented by [EpochMilliseconds].
 */
data class Article(
    val id: ArticleId,
    val url: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val publishedAt: EpochMilliseconds,
) {
    init {
        val canonicalUrl = canonicalizeArticleUrl(url)
        require(url == canonicalUrl) { "Article URL must be canonical" }
        require(id == ArticleId.fromCanonicalUrl(canonicalUrl)) {
            "Article ID must be derived from the canonical URL"
        }
        require(title.isNotBlank()) { "Article title must not be blank" }
    }

    companion object {
        fun fromCanonicalUrl(
            url: String,
            title: String,
            description: String?,
            imageUrl: String?,
            publishedAt: EpochMilliseconds,
        ): Article {
            val canonicalUrl = canonicalizeArticleUrl(url)
            return Article(
                id = ArticleId.fromCanonicalUrl(canonicalUrl),
                url = canonicalUrl,
                title = title,
                description = description,
                imageUrl = imageUrl,
                publishedAt = publishedAt,
            )
        }
    }
}
