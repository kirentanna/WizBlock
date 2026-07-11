package com.wizblock.data.repository

import com.wizblock.data.local.room.ScheduleDao
import com.wizblock.data.local.room.ScheduleEntity
import com.wizblock.model.DEFAULT_PROFILE_ID
import com.wizblock.model.ScheduleWindow
import com.wizblock.model.TargetType
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScheduleRepositoryImpl(
    private val dao: ScheduleDao
) : ScheduleRepository {
    override fun observeAll(): Flow<List<ScheduleWindow>> = dao.observeAll().map { items ->
        items.map { it.toModel() }
    }

    override fun observeEnabled(): Flow<List<ScheduleWindow>> = dao.observeEnabled().map { items ->
        items.map { it.toModel() }
    }

    override suspend fun upsert(
        targetType: TargetType,
        targetValue: String,
        startMinute: Int,
        endMinute: Int,
        daysMask: Int,
        enabled: Boolean,
        profileId: String
    ): Boolean {
        val normalizedTargetValue = targetValue.trim()
        val safeProfileId = profileId.ifBlank { DEFAULT_PROFILE_ID }
        if (normalizedTargetValue.isBlank()) return false
        if (startMinute !in 0..1439) return false
        if (endMinute !in 0..1439) return false
        if (daysMask <= 0 || daysMask > 0b1111111) return false

        val now = System.currentTimeMillis()
        val existing = dao.findByTarget(targetType.name, normalizedTargetValue, safeProfileId)
        if (existing == null) {
            dao.insert(
                ScheduleEntity(
                    id = UUID.randomUUID().toString(),
                    targetType = targetType.name,
                    targetValue = normalizedTargetValue,
                    profileId = safeProfileId,
                    startMinute = startMinute,
                    endMinute = endMinute,
                    daysMask = daysMask,
                    enabled = enabled,
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else {
            dao.updateWindow(
                id = existing.id,
                startMinute = startMinute,
                endMinute = endMinute,
                daysMask = daysMask,
                enabled = enabled,
                updatedAt = now
            )
        }
        return true
    }

    override suspend fun setEnabled(id: String, enabled: Boolean) {
        dao.setEnabled(id, enabled, System.currentTimeMillis())
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    override suspend fun deleteByProfileId(profileId: String) {
        dao.deleteByProfileId(profileId.ifBlank { DEFAULT_PROFILE_ID })
    }
}

private fun ScheduleEntity.toModel(): ScheduleWindow {
    return ScheduleWindow(
        id = id,
        targetType = TargetType.valueOf(targetType),
        targetValue = targetValue,
        profileId = profileId ?: DEFAULT_PROFILE_ID,
        startMinute = startMinute,
        endMinute = endMinute,
        daysMask = daysMask,
        enabled = enabled,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
