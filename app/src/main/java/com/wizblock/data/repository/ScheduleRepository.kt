package com.wizblock.data.repository

import com.wizblock.model.ScheduleWindow
import com.wizblock.model.TargetType
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun observeAll(): Flow<List<ScheduleWindow>>
    fun observeEnabled(): Flow<List<ScheduleWindow>>
    suspend fun upsert(
        targetType: TargetType,
        targetValue: String,
        startMinute: Int,
        endMinute: Int,
        daysMask: Int,
        enabled: Boolean = true,
        profileId: String = com.wizblock.model.DEFAULT_PROFILE_ID
    ): Boolean
    suspend fun setEnabled(id: String, enabled: Boolean)
    suspend fun delete(id: String)
    suspend fun deleteByProfileId(profileId: String)
}
