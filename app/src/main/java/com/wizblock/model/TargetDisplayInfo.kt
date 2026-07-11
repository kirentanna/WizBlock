package com.wizblock.model

data class TargetDisplayInfo(
    val targetKey: TargetKey,
    val title: String,
    val subtitle: String,
    val iconPackageName: String? = null
)
