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
                url = "https://example.com/ios",
                title = "iOS article",
                description = null,
                imageUrl = null,
                publishedAt = 1L
            )

            database.articleDao().deleteAll()
            database.articleDao().insertAll(listOf(expected))

            assertEquals(listOf(expected), database.articleDao().observeAll().first())
        } finally {
            database.close()
        }
    }
}
