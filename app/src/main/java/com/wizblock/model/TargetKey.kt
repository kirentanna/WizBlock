package com.wizblock.model

data class TargetKey(
    val targetType: TargetType,
    val targetValue: String
) {
    val id: String = "${targetType.name}|${targetValue.lowercase()}"
}
