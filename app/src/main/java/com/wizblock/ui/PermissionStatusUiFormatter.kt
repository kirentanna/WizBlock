package com.wizblock.ui

data class PermissionStatusRowUi(
    val title: String,
    val status: String,
    val description: String,
    val disclosure: String?,
    val healthy: Boolean,
    val actionLabel: String?
)

object PermissionStatusUiFormatter {
    fun permissionRows(permissionState: PermissionState): List<PermissionStatusRowUi> {
        return listOf(
            PermissionStatusRowUi(
                title = "Accessibility",
                status = if (permissionState.accessibilityGranted) "Enabled" else "Required",
                description = "Lets WizBlock detect active apps and browser URLs.",
                disclosure = if (permissionState.accessibilityGranted) null else SensitivePermissionDisclosure.accessibilityBody,
                healthy = permissionState.accessibilityGranted,
                actionLabel = if (permissionState.accessibilityGranted) null else SensitivePermissionDisclosure.accessibilityAction
            ),
            PermissionStatusRowUi(
                title = "Overlay permission",
                status = if (permissionState.overlayGranted) "Enabled" else "Required",
                description = "Lets WizBlock show the block screen over distracting apps and browsers.",
                disclosure = if (permissionState.overlayGranted) null else SensitivePermissionDisclosure.overlayBody,
                healthy = permissionState.overlayGranted,
                actionLabel = if (permissionState.overlayGranted) null else SensitivePermissionDisclosure.overlayAction
            )
        )
    }
}
