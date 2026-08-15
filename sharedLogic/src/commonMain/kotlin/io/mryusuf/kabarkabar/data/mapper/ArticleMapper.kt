package io.mryusuf.kabarkabar.data.mapper

import io.mryusuf.kabarkabar.data.local.entity.ArticleEntity
import io.mryusuf.kabarkabar.data.remote.dto.NewsApiArticleDto
import io.mryusuf.kabarkabar.domain.model.Article
import io.mryusuf.kabarkabar.domain.model.ArticleId
import io.mryusuf.kabarkabar.domain.model.EpochMilliseconds
import kotlin.time.Instant

/**
 * Result of a remote-to-domain mapping operation.
 */
sealed class ArticleMappingResult {
    /** Valid snapshot (including legitimately empty). */
    data class Success(val articles: List<Article>) : ArticleMappingResult()

    /** Non-empty input produced zero valid articles. */
    object MalformedData : ArticleMappingResult()
}

/**
 * Maps remote DTOs to domain/persistence models with validation.
 */
object ArticleMapper {

    /**
     * Maps a list of DTOs to domain Articles, filtering out malformed ones.
     *
     * Distinguishes between legitimate empty responses and malformed-data failures.
     */
    fun mapToDomain(dtos: List<NewsApiArticleDto>): ArticleMappingResult {
        val articles = dtos.mapNotNull { dto ->
            val url = dto.url ?: return@mapNotNull null
            val title = dto.title ?: return@mapNotNull null

            if (url.isBlank() || title.isBlank()) return@mapNotNull null

            try {
                Article.fromCanonicalUrl(
                    url = url,
                    title = title,
                    description = dto.description,
                    imageUrl = dto.urlToImage,
                    publishedAt = parseIso8601ToEpoch(dto.publishedAt)
                )
            } catch (e: IllegalArgumentException) {
                // Individual entry failure (e.g. invalid URL or title)
                null
            }
        }

        val deduplicatedArticles = articles.distinctBy { it.id }

        return when {
            deduplicatedArticles.isEmpty() && dtos.isNotEmpty() -> ArticleMappingResult.MalformedData
            else -> ArticleMappingResult.Success(deduplicatedArticles)
        }
    }

    /**
     * Maps a list of domain Articles to entities for persistence.
     */
    fun mapToEntities(articles: List<Article>): List<ArticleEntity> {
        return articles.map { article ->
            ArticleEntity(
                id = article.id.value,
                url = article.url,
                title = article.title,
                description = article.description,
                imageUrl = article.imageUrl,
                publishedAt = article.publishedAt.value
            )
        }
    }

    /**
     * Maps an entity back to a domain Article.
     */
    fun mapToDomain(entity: ArticleEntity): Article {
        return Article(
            id = ArticleId.fromValue(entity.id),
            url = entity.url,
            title = entity.title,
            description = entity.description,
            imageUrl = entity.imageUrl,
            publishedAt = EpochMilliseconds(entity.publishedAt)
        )
    }

    private fun parseIso8601ToEpoch(isoString: String?): EpochMilliseconds {
        val value = isoString
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException("Article publication timestamp is required")

        return EpochMilliseconds(Instant.parse(value).toEpochMilliseconds())
    }
}
