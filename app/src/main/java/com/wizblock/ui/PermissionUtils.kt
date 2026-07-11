package com.wizblock.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import com.wizblock.service.WebsiteAccessibilityService

object PermissionUtils {
    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun isAccessibilityEnabled(context: Context): Boolean {
        val expected = ComponentName(context, WebsiteAccessibilityService::class.java).flattenToString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabledServices) }
        while (splitter.hasNext()) {
            if (splitter.next().equals(expected, ignoreCase = true)) {
                return true
            }
        }

        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val list = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return list.any { info ->
            info.resolveInfo?.serviceInfo?.name == WebsiteAccessibilityService::class.java.name
        }
    }
}
