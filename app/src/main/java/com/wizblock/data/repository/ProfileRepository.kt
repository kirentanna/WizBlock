package com.wizblock.data.repository

import com.wizblock.model.Profile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeAll(): Flow<List<Profile>>
    suspend fun ensureDefaultProfile()
    suspend fun setEnabled(id: String, enabled: Boolean)
    suspend fun upsert(profile: Profile)
    suspend fun delete(id: String)
}
