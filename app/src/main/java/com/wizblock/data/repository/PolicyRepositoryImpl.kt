package com.wizblock.data.repository

import com.wizblock.policy.PolicySnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class PolicyRepositoryImpl(
    private val blockingStateRepository: BlockingStateRepository,
    private val ruleRepository: RuleRepository,
    private val scheduleRepository: ScheduleRepository,
    private val usageLimitRepository: UsageLimitRepository,
    private val profileRepository: ProfileRepository
) : PolicyRepository {
    @Suppress("UNCHECKED_CAST")
    override val snapshot: Flow<PolicySnapshot> = combine(
        listOf(
            blockingStateRepository.blockingEnabled,
            blockingStateRepository.quickBlockUntilMs,
            ruleRepository.observeEnabled(),
            scheduleRepository.observeAll(),
            usageLimitRepository.observeEnabled(),
            profileRepository.observeAll(),
            blockingStateRepository.enabledCategoryIds
        )
    ) { values ->
        val blockingEnabled = values[0] as Boolean
        val quickBlockUntilMs = values[1] as Long
        val rules = values[2] as List<com.wizblock.model.Rule>
        val schedules = values[3] as List<com.wizblock.model.ScheduleWindow>
        val limits = values[4] as List<com.wizblock.model.UsageLimit>
        val profiles = values[5] as List<com.wizblock.model.Profile>
        val categoryIds = values[6] as Set<String>
        PolicySnapshot(
            blockingEnabled = blockingEnabled,
            quickBlockUntilMs = quickBlockUntilMs,
            rules = rules,
            schedules = schedules,
            usageLimits = limits,
            profiles = profiles,
            enabledCategoryIds = categoryIds
        )
    }
}
