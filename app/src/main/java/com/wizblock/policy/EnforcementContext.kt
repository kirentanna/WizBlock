package com.wizblock.policy

data class EnforcementContext(
    val packageName: String,
    val host: String?,
    val addressText: String?,
    val nowMs: Long
)
