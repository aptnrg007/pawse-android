package com.pawse.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLimitDao {

    @Query("SELECT * FROM AppLimit ORDER BY appName")
    fun observeAll(): Flow<List<AppLimit>>

    @Query("SELECT * FROM AppLimit WHERE enabled = 1")
    suspend fun getAllEnabled(): List<AppLimit>

    @Upsert
    suspend fun upsert(appLimit: AppLimit)

    @Delete
    suspend fun delete(appLimit: AppLimit)
}
