package io.mryusuf.kabarkabar.data.mapper

import io.mryusuf.kabarkabar.data.local.entity.ArticleEntity
import io.mryusuf.kabarkabar.data.remote.dto.NewsApiArticleDto
import io.mryusuf.kabarkabar.domain.model.NewsCountry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ArticleMapperTest {

    @Test
    fun mapToDomain_filtersOutMalformedArticles() {
        val dtos = listOf(
            NewsApiArticleDto(
                title = "Valid Title",
                url = "https://example.com/1",
                description = "Desc",
                urlToImage = "https://example.com/img",
                publishedAt = "2024-03-20T12:34:56Z"
            ),
            NewsApiArticleDto(
                title = "", // Empty title
                url = "https://example.com/2",
                publishedAt = "2024-03-20T12:34:56Z"
            )
        )

        val result = ArticleMapper.mapToDomain(dtos)

        assertTrue(result is ArticleMappingResult.Success)
        assertEquals(1, result.articles.size)
        assertEquals("Valid Title", result.articles[0].title)
        assertEquals("https://example.com/1", result.articles[0].url)
    }

    @Test
    fun mapToDomain_detectsMalformedDataFailure() {
        val dtos = listOf(
            NewsApiArticleDto(
                title = "", // Empty title
                url = "https://example.com/2",
                publishedAt = "2024-03-20T12:34:56Z"
            )
        )

        val result = ArticleMapper.mapToDomain(dtos)

        assertTrue(result is ArticleMappingResult.MalformedData)
    }

    @Test
    fun mapToDomain_handlesEmptyList() {
        val result = ArticleMapper.mapToDomain(emptyList())
        assertTrue(result is ArticleMappingResult.Success)
        assertTrue(result.articles.isEmpty())
    }

    @Test
    fun mapToEntities_preservesData() {
        val dtos = listOf(
            NewsApiArticleDto(
                title = "Title",
                url = "https://example.com/1",
                description = "Desc",
                urlToImage = "https://example.com/img",
                publishedAt = "2024-03-20T12:34:56Z"
            )
        )
        val result = ArticleMapper.mapToDomain(dtos) as ArticleMappingResult.Success
        val entities = ArticleMapper.mapToEntities(result.articles, NewsCountry.US)

        assertEquals(1, entities.size)
        assertEquals("us", entities[0].countryCode)
        assertEquals("Title", entities[0].title)
        assertEquals("https://example.com/1", entities[0].url)
        assertEquals("Desc", entities[0].description)
        assertEquals("https://example.com/img", entities[0].imageUrl)
        assertEquals(1710938096000L, entities[0].publishedAt)
        assertEquals(result.articles.single(), ArticleMapper.mapToDomain(entities.single()))
    }

    @Test
    fun mapToDomain_preservesPublishedAtAsEpochMilliseconds() {
        val result = ArticleMapper.mapToDomain(
            listOf(
                NewsApiArticleDto(
                    title = "Title",
                    url = "https://example.com/1",
                    publishedAt = "2024-03-20T12:34:56+07:00"
                )
            )
        )

        assertTrue(result is ArticleMappingResult.Success)
        assertEquals(1710912896000L, result.articles.single().publishedAt.value)
    }

    @Test
    fun mapToDomain_rejectsMalformedPublishedAt() {
        val result = ArticleMapper.mapToDomain(
            listOf(
                NewsApiArticleDto(
                    title = "Title",
                    url = "https://example.com/1",
                    publishedAt = "not-a-timestamp"
                )
            )
        )

        assertTrue(result is ArticleMappingResult.MalformedData)
    }

    @Test
    fun mapToDomain_keepsFirstArticleForDuplicateCanonicalUrl() {
        val result = ArticleMapper.mapToDomain(
            listOf(
                NewsApiArticleDto(
                    title = "First title",
                    url = "https://example.com/1",
                    publishedAt = "2024-03-20T12:34:56Z"
                ),
                NewsApiArticleDto(
                    title = "Second title",
                    url = "https://example.com/1",
                    publishedAt = "2024-03-21T12:34:56Z"
                )
            )
        )

        assertTrue(result is ArticleMappingResult.Success)
        assertEquals(1, result.articles.size)
        assertEquals("First title", result.articles.single().title)
    }

    @Test
    fun articleEntity_rejectsMismatchedIdentity() {
        assertFailsWith<IllegalArgumentException> {
            ArticleEntity(
                id = "article-url:https://example.com/other",
                countryCode = "us",
                url = "https://example.com/article",
                title = "Title",
                description = null,
                imageUrl = null,
                publishedAt = 1L
            )
        }
    }

    @Test
    fun articleEntity_rejectsUnsupportedCountry() {
        assertFailsWith<IllegalArgumentException> {
            ArticleEntity(
                id = "article-url:https://example.com/article",
                countryCode = "gb",
                url = "https://example.com/article",
                title = "Title",
                description = null,
                imageUrl = null,
                publishedAt = 1L
            )
        }
    }
}
