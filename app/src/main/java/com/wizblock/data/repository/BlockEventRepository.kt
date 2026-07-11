package com.wizblock.data.repository

import com.wizblock.model.BlockEvent
import com.wizblock.model.BlockReason
import com.wizblock.model.DailyBlockSummary
import com.wizblock.model.TargetType
import com.wizblock.model.TopBlockedTarget
import kotlinx.coroutines.flow.Flow

interface BlockEventRepository {
    fun observeRecent(): Flow<List<BlockEvent>>
    fun observeDailySummary(startMs: Long, endMs: Long): Flow<DailyBlockSummary>
    fun observeTopBlocked(startMs: Long, endMs: Long, limit: Int): Flow<List<TopBlockedTarget>>
    suspend fun countTargetBlocks(targetType: TargetType, targetValue: String, startMs: Long, endMs: Long): Int
    suspend fun record(
        targetType: TargetType,
        targetValue: String,
        browserPackage: String,
        reason: BlockReason,
        matchedRuleId: String?,
        sessionId: String?
    )
}
