package com.wizblock.model

data class Rule(
    val id: String,
    val kind: RuleKind,
    val action: RuleAction,
    val operator: MatchOperator,
    val value: String,
    val profileId: String? = DEFAULT_PROFILE_ID,
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
