package com.wizblock.data.repository

import com.wizblock.model.TargetType
import com.wizblock.model.UsageLimit
import kotlinx.coroutines.flow.Flow

interface UsageLimitRepository {
    fun observeAll(): Flow<List<UsageLimit>>
    fun observeEnabled(): Flow<List<UsageLimit>>
    suspend fun upsert(
        targetType: TargetType,
        targetValue: String,
        minutesPerDay: Int,
        opensPerDay: Int,
        enabled: Boolean = true,
        profileId: String = com.wizblock.model.DEFAULT_PROFILE_ID
    ): Boolean

    suspend fun setEnabled(id: String, enabled: Boolean)
    suspend fun delete(id: String)
    suspend fun deleteByProfileId(profileId: String)
}
