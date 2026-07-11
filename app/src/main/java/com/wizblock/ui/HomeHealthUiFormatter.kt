package com.wizblock.ui

data class HomeHealthCardUi(
    val title: String,
    val subtitle: String,
    val healthy: Boolean
)

object HomeHealthUiFormatter {
    fun card(permissionState: PermissionState): HomeHealthCardUi? {
        val missingPermission = !permissionState.accessibilityGranted || !permissionState.overlayGranted
        if (missingPermission) {
            return HomeHealthCardUi(
                title = "Permissions need attention",
                subtitle = "Accessibility or overlay missing",
                healthy = false
            )
        }
        if (permissionState.serviceHealth != ServiceHealth.OFFLINE) {
            return HomeHealthCardUi(
                title = "All systems ready",
                subtitle = "Accessibility, overlay, service",
                healthy = true
            )
        }
        return null
    }
}
