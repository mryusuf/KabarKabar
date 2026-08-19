package io.mryusuf.kabarkabar.data.local.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import io.mryusuf.kabarkabar.data.local.dao.ArticleDao
import io.mryusuf.kabarkabar.data.local.entity.ArticleEntity

@Database(entities = [ArticleEntity::class], version = 2)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `articles_new` (
                        `id` TEXT NOT NULL,
                        `countryCode` TEXT NOT NULL,
                        `url` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT,
                        `imageUrl` TEXT,
                        `publishedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`, `countryCode`)
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    """
                    INSERT INTO `articles_new` (`id`, `countryCode`, `url`, `title`, `description`, `imageUrl`, `publishedAt`)
                    SELECT `id`, 'us', `url`, `title`, `description`, `imageUrl`, `publishedAt` FROM `articles`
                    """.trimIndent()
                )
                connection.execSQL("DROP TABLE `articles`")
                connection.execSQL("ALTER TABLE `articles_new` RENAME TO `articles`")
            }
        }
    }
}

// Room KMP pattern for database construction
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
