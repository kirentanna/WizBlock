package com.wizblock.data.repository

import com.wizblock.data.local.room.BlockEventDao
import com.wizblock.data.local.room.BlockEventEntity
import com.wizblock.model.BlockEvent
import com.wizblock.model.BlockReason
import com.wizblock.model.DailyBlockSummary
import com.wizblock.model.TargetType
import com.wizblock.model.TopBlockedTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BlockEventRepositoryImpl(
    private val dao: BlockEventDao
) : BlockEventRepository {
    override fun observeRecent(): Flow<List<BlockEvent>> {
        return dao.observeRecent().map { events ->
            events.map {
                BlockEvent(
                    id = it.id,
                    targetType = TargetType.valueOf(it.targetType),
                    targetValue = it.targetValue,
                    browserPackage = it.browserPackage,
                    reason = BlockReason.valueOf(it.reason),
                    matchedRuleId = it.matchedRuleId,
                    sessionId = it.sessionId,
                    blockedAt = it.blockedAt
                )
            }
        }
    }

    override fun observeDailySummary(startMs: Long, endMs: Long): Flow<DailyBlockSummary> {
        return dao.observeDailySummary(startMs, endMs).map { rows ->
            var appBlocks = 0
            var domainBlocks = 0
            var keywordBlocks = 0
            rows.forEach { row ->
                when (TargetType.valueOf(row.targetType)) {
                    TargetType.APP_PACKAGE -> appBlocks = row.count.toInt()
                    TargetType.DOMAIN -> domainBlocks = row.count.toInt()
                    TargetType.KEYWORD -> keywordBlocks = row.count.toInt()
                }
            }
            DailyBlockSummary(
                total = appBlocks + domainBlocks + keywordBlocks,
                appBlocks = appBlocks,
                domainBlocks = domainBlocks,
                keywordBlocks = keywordBlocks
            )
        }
    }

    override fun observeTopBlocked(startMs: Long, endMs: Long, limit: Int): Flow<List<TopBlockedTarget>> {
        return dao.observeTopBlocked(startMs, endMs, limit).map { rows ->
            rows.map {
                TopBlockedTarget(
                    targetType = TargetType.valueOf(it.targetType),
                    targetValue = it.targetValue,
                    count = it.count.toInt()
                )
            }
        }
    }

    override suspend fun countTargetBlocks(
        targetType: TargetType,
        targetValue: String,
        startMs: Long,
        endMs: Long
    ): Int {
        return dao.countTargetBlocks(targetType.name, targetValue, startMs, endMs)
    }

    override suspend fun record(
        targetType: TargetType,
        targetValue: String,
        browserPackage: String,
        reason: BlockReason,
        matchedRuleId: String?,
        sessionId: String?
    ) {
        dao.insert(
            BlockEventEntity(
                targetType = targetType.name,
                targetValue = targetValue,
                browserPackage = browserPackage,
                reason = reason.name,
                matchedRuleId = matchedRuleId,
                sessionId = sessionId,
                blockedAt = System.currentTimeMillis()
            )
        )
        dao.trimToRecent200()
    }
}
