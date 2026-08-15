package io.mryusuf.kabarkabar.domain.model

/**
 * Stable local identity for an article.
 *
 * The canonical URL is retained as the source of identity instead of being
 * reduced to a lossy hash. This makes the ID deterministic and collision-free
 * for distinct canonical URL strings.
 */
data class ArticleId(val value: String) {
    init {
        require(value.startsWith(ARTICLE_ID_PREFIX)) {
            "Article ID must be derived from a canonical article URL"
        }
        require(value.length > ARTICLE_ID_PREFIX.length) { "Article ID must not be blank" }
    }

    companion object {
        fun fromCanonicalUrl(url: String): ArticleId {
            val canonicalUrl = canonicalizeArticleUrl(url)
            return ArticleId("$ARTICLE_ID_PREFIX$canonicalUrl")
        }

        /**
         * Rehydrates an ID supplied by a persistence or navigation boundary.
         * Article construction still verifies it against the canonical URL.
         */
        fun fromValue(value: String): ArticleId = ArticleId(value)
    }
}

private const val ARTICLE_ID_PREFIX = "article-url:"

/**
 * Validates and normalizes the URL representation used as article identity.
 * Full response validation remains the responsibility of the data boundary.
 */
internal fun canonicalizeArticleUrl(rawUrl: String): String {
    val normalized = rawUrl.trim()
    require(normalized.isNotBlank()) { "Article URL must not be blank" }
    require(
        normalized.startsWith("https://") || normalized.startsWith("http://")
    ) { "Article URL must use HTTP or HTTPS" }
    val authority = normalized
        .substringAfter("://")
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
    require(authority.isNotBlank()) {
        "Article URL must include a host"
    }
    require(normalized.none { it.isWhitespace() }) {
        "Article URL must not contain whitespace"
    }
    return normalized
}
