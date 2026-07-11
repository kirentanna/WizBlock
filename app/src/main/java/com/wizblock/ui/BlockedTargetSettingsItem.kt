package com.wizblock.ui

import com.wizblock.model.ScheduleWindow
import com.wizblock.model.TargetType
import com.wizblock.model.UsageLimit

data class BlockedTargetSettingsItem(
    val ruleId: String? = null,
    val profileId: String? = null,
    val enabled: Boolean = true,
    val blocklistNames: List<String> = emptyList(),
    val targetType: TargetType,
    val targetValue: String,
    val title: String,
    val subtitle: String,
    val schedule: ScheduleWindow?,
    val usageLimit: UsageLimit?
)
