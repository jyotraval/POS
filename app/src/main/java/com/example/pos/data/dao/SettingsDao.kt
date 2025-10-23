package com.example.pos.data.dao

import androidx.room.*
import com.example.pos.data.entity.Settings
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettings(): Flow<Settings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: Settings)

    @Query("DELETE FROM settings")
    suspend fun deleteAllSettings()
}