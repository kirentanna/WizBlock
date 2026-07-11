package com.wizblock.data.repository

import com.wizblock.model.TargetType
import com.wizblock.model.UsageCounter
import kotlinx.coroutines.flow.Flow

interface UsageCounterRepository {
    fun observeByDate(localDate: String): Flow<List<UsageCounter>>
    suspend fun incrementUsage(
        localDate: String,
        targetType: TargetType,
        targetValue: String,
        secondsDelta: Int,
        opensDelta: Int
    )

    suspend fun getCounter(localDate: String, targetType: TargetType, targetValue: String): UsageCounter?
}
