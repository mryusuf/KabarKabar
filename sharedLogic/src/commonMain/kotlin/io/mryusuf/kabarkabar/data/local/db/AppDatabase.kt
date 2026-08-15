package io.mryusuf.kabarkabar.data.local.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import io.mryusuf.kabarkabar.data.local.dao.ArticleDao
import io.mryusuf.kabarkabar.data.local.entity.ArticleEntity

@Database(entities = [ArticleEntity::class], version = 1)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
}

// Room KMP pattern for database construction
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
