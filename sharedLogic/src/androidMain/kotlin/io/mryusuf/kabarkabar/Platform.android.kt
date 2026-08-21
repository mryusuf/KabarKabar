package io.mryusuf.kabarkabar

import android.content.Context
import android.os.Build
import androidx.room.Room
import androidx.room.RoomDatabase
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.mryusuf.kabarkabar.data.local.db.AppDatabase

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

private lateinit var appContext: Context

fun initializeAndroidPlatform(context: Context) {
    appContext = context.applicationContext
}

actual fun getHttpClientEngine(): HttpClientEngine = OkHttp.create()

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = appContext.getDatabasePath("kabarkabar.db")
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    ).addMigrations(AppDatabase.MIGRATION_1_2)
}
