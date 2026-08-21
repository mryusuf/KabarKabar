package io.mryusuf.kabarkabar

import io.mryusuf.kabarkabar.data.local.entity.ArticleEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RoomInfrastructureIOSTest {

    @Test
    fun databaseBuilder_usesGeneratedImplementationAndPersists() = runTest {
        val database = getDatabaseBuilder().build()
        try {
            val expected = ArticleEntity(
                id = "article-url:https://example.com/ios",
                countryCode = "us",
                url = "https://example.com/ios",
                title = "iOS article",
                description = null,
                imageUrl = null,
                publishedAt = 1L
            )

            database.articleDao().deleteAll("us")
            database.articleDao().insertAll(listOf(expected))

            assertEquals(listOf(expected), database.articleDao().observeAll("us").first())
        } finally {
            database.close()
        }
    }

    @Test
    fun databaseBuilder_scopes_same_article_id_by_country() = runTest {
        val database = getDatabaseBuilder().build()
        try {
            val url = "https://example.com/shared"
            val usArticle = ArticleEntity(
                id = "article-url:$url",
                countryCode = "us",
                url = url,
                title = "US headline",
                description = null,
                imageUrl = null,
                publishedAt = 1L,
            )
            val idArticle = usArticle.copy(
                countryCode = "id",
                title = "ID headline",
            )
            val dao = database.articleDao()
            dao.deleteAll("us")
            dao.deleteAll("id")
            dao.insertAll(listOf(usArticle, idArticle))

            assertEquals(listOf(usArticle), dao.observeAll("us").first())
            assertEquals(listOf(idArticle), dao.observeAll("id").first())
            assertEquals(usArticle, dao.observeById(usArticle.id, "us").first())
            assertEquals(idArticle, dao.observeById(idArticle.id, "id").first())
        } finally {
            database.close()
        }
    }
}
