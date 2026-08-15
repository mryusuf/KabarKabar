package io.mryusuf.kabarkabar

import androidx.room.RoomDatabase
import io.ktor.client.engine.HttpClientEngine
import io.mryusuf.kabarkabar.data.local.db.AppDatabase

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun getHttpClientEngine(): HttpClientEngine

expect fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>
