package com.wizblock.data.repository

import com.wizblock.data.local.room.UsageCounterDao
import com.wizblock.data.local.room.UsageCounterEntity
import com.wizblock.domain.TargetNormalizer
import com.wizblock.model.TargetType
import com.wizblock.model.UsageCounter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UsageCounterRepositoryImpl(
    private val dao: UsageCounterDao,
    private val targetNormalizer: TargetNormalizer
) : UsageCounterRepository {
    override fun observeByDate(localDate: String): Flow<List<UsageCounter>> {
        return dao.observeByDate(localDate).map { list -> list.map { it.toModel() } }
    }

    override suspend fun incrementUsage(
        localDate: String,
        targetType: TargetType,
        targetValue: String,
        secondsDelta: Int,
        opensDelta: Int
    ) {
        val target = targetNormalizer.normalize(targetType, targetValue) ?: return
        val normalizedValue = target.targetValue
        val id = counterId(localDate, target)
        val existing = dao.findById(id)
        val now = System.currentTimeMillis()
        val updated = if (existing == null) {
            UsageCounterEntity(
                id = id,
                localDate = localDate,
                targetType = targetType.name,
                targetValue = normalizedValue,
                usedSeconds = secondsDelta.coerceAtLeast(0),
                openCount = opensDelta.coerceAtLeast(0),
                updatedAt = now
            )
        } else {
            existing.copy(
                usedSeconds = (existing.usedSeconds + secondsDelta).coerceAtLeast(0),
                openCount = (existing.openCount + opensDelta).coerceAtLeast(0),
                updatedAt = now
            )
        }
        dao.upsert(updated)
    }

    override suspend fun getCounter(localDate: String, targetType: TargetType, targetValue: String): UsageCounter? {
        val target = targetNormalizer.normalize(targetType, targetValue) ?: return null
        return dao.findById(counterId(localDate, target))?.toModel()
    }

    private fun counterId(localDate: String, target: com.wizblock.model.TargetKey): String {
        return "$localDate|${target.id}"
    }
}

private fun UsageCounterEntity.toModel(): UsageCounter {
    return UsageCounter(
        id = id,
        localDate = localDate,
        targetType = TargetType.valueOf(targetType),
        targetValue = targetValue,
        usedSeconds = usedSeconds,
        openCount = openCount,
        updatedAt = updatedAt
    )
}
