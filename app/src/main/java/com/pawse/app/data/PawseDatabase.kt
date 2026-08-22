package com.pawse.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

@Database(entities = [AppLimit::class], version = 2, exportSchema = false)
abstract class PawseDatabase : RoomDatabase() {
    abstract fun appLimitDao(): AppLimitDao

    companion object {
        @Volatile
        private var instance: PawseDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE AppLimit ADD COLUMN avatar TEXT NOT NULL DEFAULT '${Avatar.TURTLE.name}'",
                )
            }
        }

        fun getInstance(context: Context): PawseDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PawseDatabase::class.java,
                    "pawse.db",
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
