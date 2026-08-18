package io.mryusuf.kabarkabar.data.local.db

import androidx.room.RoomDatabaseConstructor
import androidx.room.constructDatabase

actual object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase = constructDatabase(AppDatabaseConstructor::class)
}
