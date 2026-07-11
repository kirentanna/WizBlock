package com.wizblock.ui

data class PermissionState(
    val accessibilityGranted: Boolean = false,
    val overlayGranted: Boolean = false,
    val serviceHealth: ServiceHealth = ServiceHealth.OFFLINE
)
