package com.wizblock.data.repository

import com.wizblock.data.local.room.UsageLimitDao
import com.wizblock.data.local.room.UsageLimitEntity
import com.wizblock.domain.TargetNormalizer
import com.wizblock.model.DEFAULT_PROFILE_ID
import com.wizblock.model.TargetType
import com.wizblock.model.UsageLimit
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UsageLimitRepositoryImpl(
    private val dao: UsageLimitDao,
    private val targetNormalizer: TargetNormalizer
) : UsageLimitRepository {
    override fun observeAll(): Flow<List<UsageLimit>> = dao.observeAll().map { list ->
        list.map { it.toModel() }
    }

    override fun observeEnabled(): Flow<List<UsageLimit>> = dao.observeEnabled().map { list ->
        list.map { it.toModel() }
    }

    override suspend fun upsert(
        targetType: TargetType,
        targetValue: String,
        minutesPerDay: Int,
        opensPerDay: Int,
        enabled: Boolean,
        profileId: String
    ): Boolean {
        if (minutesPerDay <= 0 && opensPerDay <= 0) return false
        val normalizedValue = targetNormalizer.normalize(targetType, targetValue)?.targetValue ?: return false
        val safeProfileId = profileId.ifBlank { DEFAULT_PROFILE_ID }

        val now = System.currentTimeMillis()
        val safeMinutes = minutesPerDay.coerceAtLeast(0)
        val safeOpens = opensPerDay.coerceAtLeast(0)
        val existing = dao.findByTarget(targetType.name, normalizedValue, safeProfileId)
        if (existing == null) {
            dao.insert(
                UsageLimitEntity(
                    id = UUID.randomUUID().toString(),
                    targetType = targetType.name,
                    targetValue = normalizedValue,
                    profileId = safeProfileId,
                    minutesPerDay = safeMinutes,
                    opensPerDay = safeOpens,
                    enabled = enabled,
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else {
            dao.updateValues(
                id = existing.id,
                minutesPerDay = safeMinutes,
                opensPerDay = safeOpens,
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

private fun UsageLimitEntity.toModel(): UsageLimit {
    return UsageLimit(
        id = id,
        targetType = TargetType.valueOf(targetType),
        targetValue = targetValue,
        profileId = profileId ?: DEFAULT_PROFILE_ID,
        minutesPerDay = minutesPerDay,
        opensPerDay = opensPerDay,
        enabled = enabled,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
