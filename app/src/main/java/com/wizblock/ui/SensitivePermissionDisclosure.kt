package com.wizblock.ui

object SensitivePermissionDisclosure {
    const val accessibilityBody =
        "WizBlock uses Accessibility to read the active app, visible browser URL text, and window changes on this device so it can enforce your local blocklists. This data is not sent anywhere."

    const val accessibilityAction = "I understand, open Accessibility settings"

    const val overlayBody =
        "WizBlock uses overlay permission to show a blocking screen over apps and browsers that match your local rules."

    const val overlayAction = "Open overlay settings"

    const val deviceAdminTitle = "Enable uninstall protection?"

    const val deviceAdminBody =
        "WizBlock uses Android Device Admin only to prevent normal uninstall while Strict Mode uninstall protection is enabled. It does not request wipe, password, camera, or lock-screen policies. You can disable this after the Strict Mode timer ends."

    const val deviceAdminConfirmAction = "I understand, enable Device Admin"
}
