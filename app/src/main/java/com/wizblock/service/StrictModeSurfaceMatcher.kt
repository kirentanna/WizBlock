package com.wizblock.service

object StrictModeSurfaceMatcher {
    fun isAllowedSettingsSurface(visibleText: String): Boolean {
        return isAllowedSettingsSurface(className = null, visibleText = visibleText)
    }

    fun isAllowedSettingsSurface(className: String?, visibleText: String): Boolean {
        if (isBlockedSettingsSurface(className, visibleText)) return false

        val normalizedClass = className.orEmpty().lowercase()
        val normalizedText = visibleText.lowercase()

        val safeSignals = listOf("wi-fi", "wifi", "bluetooth", "sound", "volume")
        return safeSignals.any { normalizedText.contains(it) || normalizedClass.contains(it) }
    }

    fun isBlockedSettingsSurface(className: String?, visibleText: String): Boolean {
        val normalizedClass = className.orEmpty().lowercase()
        val normalizedText = visibleText.lowercase()

        val dangerousSignals = listOf(
            "uninstall",
            "force stop",
            "app info",
            "appinfodashboard",
            "applicationdetails",
            "device admin",
            "deviceadmin",
            "special app access",
            "specialappaccess",
            "draw over other apps",
            "manageoverlay",
            "overlay",
            "display over other apps",
            "modify system settings",
            "modifysystem",
            "install unknown apps",
            "unknownapp",
            "accessibility",
            "accessibility services"
        )
        return dangerousSignals.any { normalizedText.contains(it) || normalizedClass.contains(it) }
    }

    fun isDeviceAdminDisableSurface(
        packageName: String,
        className: String?,
        visibleText: String
    ): Boolean {
        val normalizedPackage = packageName.lowercase()
        val normalizedClass = className.orEmpty().lowercase()
        val normalizedText = visibleText.lowercase()

        if (!isSettingsPackage(normalizedPackage)) return false

        val deviceAdminClass = listOf(
            "deviceadminadd",
            "deviceadminsettings",
            "secdeviceadminadd"
        ).any { normalizedClass.contains(it) }

        val mentionsWizBlockAdmin = normalizedText.contains("wizblock") &&
            (normalizedText.contains("device admin") || normalizedText.contains("device administrator"))

        val dangerousAdminAction = listOf("deactivate", "disable", "remove", "turn off")
            .any { normalizedText.contains(it) } &&
            listOf("device admin", "device administrator", "device admin app", "wizblock")
                .any { normalizedText.contains(it) }

        return deviceAdminClass || mentionsWizBlockAdmin || dangerousAdminAction
    }

    private fun isSettingsPackage(packageName: String): Boolean {
        return packageName == "com.android.settings" ||
            packageName.startsWith("com.samsung.android.settings") ||
            packageName.startsWith("com.miui.securitycenter") ||
            packageName.startsWith("com.coloros.safecenter") ||
            packageName.startsWith("com.oplus.safecenter") ||
            packageName.startsWith("com.vivo.permissionmanager") ||
            packageName.startsWith("com.huawei.systemmanager")
    }
}
