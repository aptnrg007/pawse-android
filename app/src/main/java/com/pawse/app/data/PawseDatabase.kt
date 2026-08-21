package com.pawse.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AppLimit::class], version = 1, exportSchema = false)
abstract class PawseDatabase : RoomDatabase() {
    abstract fun appLimitDao(): AppLimitDao

    companion object {
        @Volatile
        private var instance: PawseDatabase? = null

        fun getInstance(context: Context): PawseDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PawseDatabase::class.java,
                    "pawse.db",
                ).build().also { instance = it }
            }
    }
}
