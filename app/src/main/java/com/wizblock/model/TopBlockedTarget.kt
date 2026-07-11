package com.wizblock.model

data class TopBlockedTarget(
    val targetType: TargetType,
    val targetValue: String,
    val count: Int
)
