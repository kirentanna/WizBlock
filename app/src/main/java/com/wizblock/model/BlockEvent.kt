package com.wizblock.model

data class BlockEvent(
    val id: Long,
    val targetType: TargetType,
    val targetValue: String,
    val browserPackage: String,
    val reason: BlockReason,
    val matchedRuleId: String?,
    val sessionId: String?,
    val blockedAt: Long
)
