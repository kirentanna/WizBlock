package com.wizblock.policy

import com.wizblock.model.BlockReason
import com.wizblock.model.TargetType

sealed interface PolicyDecision {
    data object Inactive : PolicyDecision
    data object Allowed : PolicyDecision
    data object NotMatched : PolicyDecision
    data class Blocked(
        val targetType: TargetType,
        val targetValue: String,
        val reason: BlockReason,
        val matchedRuleId: String?
    ) : PolicyDecision
}
