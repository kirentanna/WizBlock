package com.wizblock.policy

import com.wizblock.model.Rule
import com.wizblock.model.ScheduleWindow
import com.wizblock.model.Profile
import com.wizblock.model.UsageLimit

data class PolicySnapshot(
    val blockingEnabled: Boolean,
    val quickBlockUntilMs: Long,
    val rules: List<Rule>,
    val schedules: List<ScheduleWindow>,
    val usageLimits: List<UsageLimit>,
    val profiles: List<Profile> = listOf(Profile.Default),
    val enabledCategoryIds: Set<String> = emptySet()
)
