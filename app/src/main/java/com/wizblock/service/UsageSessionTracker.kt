package com.wizblock.service

import com.wizblock.data.repository.UsageCounterRepository
import com.wizblock.model.TargetKey
import com.wizblock.model.UsageCounter
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UsageSessionTracker(
    private val scope: CoroutineScope,
    private val usageCounterRepository: UsageCounterRepository,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    @Volatile
    var countersByTarget: Map<TargetKey, UsageCounter> = emptyMap()
        private set

    private var countersJob: Job? = null
    private var observedUsageDate: String = ""
    private var activeUsageSession: UsageSession? = null

    fun ensureObservation(nowMs: Long) {
        val localDate = localDateString(nowMs)
        if (observedUsageDate == localDate) return
        observedUsageDate = localDate
        countersJob?.cancel()
        countersJob = scope.launch {
            usageCounterRepository.observeByDate(localDate).collectLatest { counters ->
                countersByTarget = counters.associateBy { TargetKey(it.targetType, it.targetValue) }
            }
        }
    }

    fun update(currentTarget: TargetKey?, nowMs: Long) {
        val localDate = localDateString(nowMs)
        val previous = activeUsageSession
        if (previous != null) {
            val sameTarget = currentTarget != null &&
                previous.localDate == localDate &&
                previous.target == currentTarget
            val elapsedSeconds = ((nowMs - previous.startedAtMs) / 1000L).coerceAtLeast(0L).toInt()
            if (!sameTarget) {
                flush(nowMs)
            } else if (elapsedSeconds >= USAGE_FLUSH_STEP_SECONDS) {
                scope.launch {
                    usageCounterRepository.incrementUsage(
                        localDate = previous.localDate,
                        targetType = previous.target.targetType,
                        targetValue = previous.target.targetValue,
                        secondsDelta = elapsedSeconds,
                        opensDelta = 0
                    )
                }
                activeUsageSession = previous.copy(startedAtMs = nowMs)
                return
            } else {
                return
            }
        }

        if (currentTarget != null) {
            scope.launch {
                usageCounterRepository.incrementUsage(
                    localDate = localDate,
                    targetType = currentTarget.targetType,
                    targetValue = currentTarget.targetValue,
                    secondsDelta = 0,
                    opensDelta = 1
                )
            }
            activeUsageSession = UsageSession(
                localDate = localDate,
                target = currentTarget,
                startedAtMs = nowMs
            )
        }
    }

    fun flush(nowMs: Long) {
        val previous = activeUsageSession ?: return
        activeUsageSession = null
        val elapsedSeconds = ((nowMs - previous.startedAtMs) / 1000L).coerceAtLeast(0L).toInt()
        if (elapsedSeconds <= 0) return
        scope.launch {
            usageCounterRepository.incrementUsage(
                localDate = previous.localDate,
                targetType = previous.target.targetType,
                targetValue = previous.target.targetValue,
                secondsDelta = elapsedSeconds,
                opensDelta = 0
            )
        }
    }

    fun stop(nowMs: Long) {
        countersJob?.cancel()
        flush(nowMs)
    }

    private fun localDateString(nowMs: Long): String {
        return Instant.ofEpochMilli(nowMs).atZone(zoneId).toLocalDate().toString()
    }

    private data class UsageSession(
        val localDate: String,
        val target: TargetKey,
        val startedAtMs: Long
    )

    private companion object {
        const val USAGE_FLUSH_STEP_SECONDS = 5
    }
}
