package com.example.data

import kotlinx.coroutines.flow.Flow

class EqRepository(private val dao: EqPresetDao) {
    val customPresets: Flow<List<EqPresetEntity>> = dao.getAllCustomPresets()

    suspend fun saveCustomPreset(preset: EqPresetEntity) {
        dao.insertPreset(preset)
    }

    suspend fun deleteCustomPreset(id: Int) {
        dao.deletePresetById(id)
    }
}
