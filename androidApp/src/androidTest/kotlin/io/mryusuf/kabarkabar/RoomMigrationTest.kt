package io.mryusuf.kabarkabar

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.mryusuf.kabarkabar.data.local.db.AppDatabase
import io.mryusuf.kabarkabar.data.local.entity.ArticleEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class RoomMigrationTest {

    @Test
    fun version_1_cache_migrates_to_US_without_losing_the_row() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "phase7c-migration-test.db"
        val databaseFile = context.getDatabasePath(databaseName)
        context.deleteDatabase(databaseName)

        createVersion1Database(databaseFile)

        val database = Room.databaseBuilder<AppDatabase>(
            context = context,
            name = databaseFile.absolutePath,
        ).addMigrations(AppDatabase.MIGRATION_1_2).build()

        try {
            val expected = ArticleEntity(
                id = "article-url:https://example.com/migrated",
                countryCode = "us",
                url = "https://example.com/migrated",
                title = "Migrated headline",
                description = "Old cache",
                imageUrl = null,
                publishedAt = 1L,
            )

            assertEquals(listOf(expected), database.articleDao().observeAll("us").first())
            assertTrue(database.articleDao().observeAll("id").first().isEmpty())
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }

    private fun createVersion1Database(databaseFile: File) {
        databaseFile.parentFile?.mkdirs()
        val database = SQLiteDatabase.openOrCreateDatabase(databaseFile, null)
        try {
            database.execSQL(
                """
                CREATE TABLE articles (
                    id TEXT NOT NULL PRIMARY KEY,
                    url TEXT NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT,
                    imageUrl TEXT,
                    publishedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            database.execSQL(
                "CREATE TABLE room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)"
            )
            database.execSQL(
                "INSERT INTO room_master_table (id, identity_hash) VALUES (42, ?)",
                arrayOf<Any?>("12828472c042f38a57e11b9882d91d63")
            )
            database.execSQL(
                """
                INSERT INTO articles (id, url, title, description, imageUrl, publishedAt)
                VALUES (?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    "article-url:https://example.com/migrated",
                    "https://example.com/migrated",
                    "Migrated headline",
                    "Old cache",
                    null,
                    1L,
                )
            )
            database.version = 1
        } finally {
            database.close()
        }
    }
}
