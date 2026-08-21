package io.mryusuf.kabarkabar

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import io.mryusuf.kabarkabar.data.local.db.AppDatabase
import platform.Foundation.NSHomeDirectory

class IOSPlatform : Platform {
    override val name: String = "iOS"
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun getHttpClientEngine(): HttpClientEngine = Darwin.create()

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFilePath = NSHomeDirectory() + "/kabarkabar.db"
    return Room.databaseBuilder<AppDatabase>(
        name = dbFilePath
    ).setDriver(BundledSQLiteDriver())
        .addMigrations(AppDatabase.MIGRATION_1_2)
}
