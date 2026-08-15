package io.mryusuf.kabarkabar.domain

import io.mryusuf.kabarkabar.domain.model.Article
import io.mryusuf.kabarkabar.domain.model.ArticleId
import io.mryusuf.kabarkabar.domain.model.EpochMilliseconds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ArticleDomainTest {

    @Test
    fun should_derive_a_stable_id_from_the_canonical_url() {
        val url = "https://example.com/news/one"

        val first = Article.fromCanonicalUrl(
            url = url,
            title = "Title",
            description = null,
            imageUrl = null,
            publishedAt = EpochMilliseconds(0L)
        )
        val second = Article.fromCanonicalUrl(
            url = url,
            title = "Updated title",
            description = null,
            imageUrl = null,
            publishedAt = EpochMilliseconds(1L)
        )

        assertEquals(ArticleId.fromCanonicalUrl(url), first.id)
        assertEquals(first.id, second.id)
        assertEquals(url, first.url)
    }

    @Test
    fun should_use_different_ids_for_different_canonical_urls() {
        val first = Article.fromCanonicalUrl(
            url = "https://example.com/news/one",
            title = "First",
            description = null,
            imageUrl = null,
            publishedAt = EpochMilliseconds(0L)
        )
        val second = Article.fromCanonicalUrl(
            url = "https://example.com/news/two",
            title = "Second",
            description = null,
            imageUrl = null,
            publishedAt = EpochMilliseconds(0L)
        )

        kotlin.test.assertNotEquals(first.id, second.id)
    }

    @Test
    fun should_fail_when_id_does_not_match_the_canonical_url() {
        assertFailsWith<IllegalArgumentException> {
            Article(
                id = ArticleId.fromValue("article-url:https://example.com/other"),
                url = "https://example.com/news/one",
                title = "Title",
                description = null,
                imageUrl = null,
                publishedAt = EpochMilliseconds(0L)
            )
        }
    }

    @Test
    fun should_fail_when_id_is_blank() {
        assertFailsWith<IllegalArgumentException> {
            Article(
                id = ArticleId.fromValue(" "),
                url = "https://example.com",
                title = "Title",
                description = null,
                imageUrl = null,
                publishedAt = EpochMilliseconds(0L)
            )
        }
    }

    @Test
    fun should_fail_when_id_is_not_url_derived() {
        assertFailsWith<IllegalArgumentException> {
            ArticleId.fromValue("arbitrary-id")
        }
    }

    @Test
    fun should_fail_when_url_is_blank() {
        assertFailsWith<IllegalArgumentException> {
            Article(
                id = ArticleId.fromCanonicalUrl("https://example.com"),
                url = "",
                title = "Title",
                description = null,
                imageUrl = null,
                publishedAt = EpochMilliseconds(0L)
            )
        }
    }

    @Test
    fun should_fail_when_url_has_no_host() {
        assertFailsWith<IllegalArgumentException> {
            Article.fromCanonicalUrl(
                url = "https:///missing-host",
                title = "Title",
                description = null,
                imageUrl = null,
                publishedAt = EpochMilliseconds(0L)
            )
        }
    }

    @Test
    fun should_fail_when_title_is_blank() {
        assertFailsWith<IllegalArgumentException> {
            Article(
                id = ArticleId.fromCanonicalUrl("https://example.com"),
                url = "https://example.com",
                title = " \n ",
                description = null,
                imageUrl = null,
                publishedAt = EpochMilliseconds(0L)
            )
        }
    }
}
