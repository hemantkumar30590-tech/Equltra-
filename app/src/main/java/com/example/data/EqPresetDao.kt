package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EqPresetDao {
    @Query("SELECT * FROM custom_eq_presets ORDER BY id DESC")
    fun getAllCustomPresets(): Flow<List<EqPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: EqPresetEntity)

    @Query("DELETE FROM custom_eq_presets WHERE id = :id")
    suspend fun deletePresetById(id: Int)
}
