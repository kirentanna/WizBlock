package com.wizblock.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StrictModeSurfaceMatcherTest {
    @Test
    fun samsungDeviceAdminAddClass_isBlocked() {
        assertThat(
            StrictModeSurfaceMatcher.isDeviceAdminDisableSurface(
                packageName = "com.android.settings",
                className = "com.samsung.android.settings.applications.specialaccess.SecDeviceAdminAdd",
                visibleText = "Device admin app WizBlock Deactivate"
            )
        ).isTrue()
    }

    @Test
    fun wizBlockDeviceAdminListRow_isBlockedEvenBeforeDeactivateDialog() {
        assertThat(
            StrictModeSurfaceMatcher.isDeviceAdminDisableSurface(
                packageName = "com.android.settings",
                className = "com.android.settings.Settings",
                visibleText = "Device admin apps WizBlock"
            )
        ).isTrue()
    }

    @Test
    fun stockAndroidDeviceAdminAction_isBlocked() {
        assertThat(
            StrictModeSurfaceMatcher.isDeviceAdminDisableSurface(
                packageName = "com.android.settings",
                className = "com.android.settings.DeviceAdminAdd",
                visibleText = "Deactivate this device admin app"
            )
        ).isTrue()
    }

    @Test
    fun unrelatedSettingsScreen_isAllowed() {
        assertThat(
            StrictModeSurfaceMatcher.isDeviceAdminDisableSurface(
                packageName = "com.android.settings",
                className = "com.android.settings.Settings",
                visibleText = "Wi-Fi Bluetooth Sound Volume"
            )
        ).isFalse()
    }

    @Test
    fun nonSettingsPackage_isAllowed() {
        assertThat(
            StrictModeSurfaceMatcher.isDeviceAdminDisableSurface(
                packageName = "com.example.app",
                className = "com.android.settings.DeviceAdminAdd",
                visibleText = "Device admin app WizBlock Deactivate"
            )
        ).isFalse()
    }

    @Test
    fun basicWifiSettingsSurface_isAllowed() {
        assertThat(
            StrictModeSurfaceMatcher.isAllowedSettingsSurface(
                visibleText = "Wi-Fi Network preferences Saved networks"
            )
        ).isTrue()
    }

    @Test
    fun wifiSettingsClass_isAllowedEvenBeforeTextLoads() {
        assertThat(
            StrictModeSurfaceMatcher.isAllowedSettingsSurface(
                className = "com.android.settings.Settings\$WifiSettingsActivity",
                visibleText = ""
            )
        ).isTrue()
    }

    @Test
    fun bluetoothSettingsClass_isAllowedEvenBeforeTextLoads() {
        assertThat(
            StrictModeSurfaceMatcher.isAllowedSettingsSurface(
                className = "com.android.settings.Settings\$BluetoothSettingsActivity",
                visibleText = ""
            )
        ).isTrue()
    }

    @Test
    fun genericSettingsStartupEvent_isNotBlocked() {
        assertThat(
            StrictModeSurfaceMatcher.isBlockedSettingsSurface(
                className = "android.widget.FrameLayout",
                visibleText = "Home WhatsApp Camera Apps Digital clock"
            )
        ).isFalse()
    }

    @Test
    fun accessibilitySettingsSurface_isBlocked() {
        assertThat(
            StrictModeSurfaceMatcher.isAllowedSettingsSurface(
                visibleText = "Accessibility Installed apps WizBlock Off TalkBack Visibility enhancements"
            )
        ).isFalse()
    }

    @Test
    fun wizBlockAccessibilityServiceSurface_isBlocked() {
        assertThat(
            StrictModeSurfaceMatcher.isAllowedSettingsSurface(
                visibleText = "WizBlock Use WizBlock Shortcut Permissions Accessibility service"
            )
        ).isFalse()
    }

    @Test
    fun accessibilitySettingsClass_isBlockedEvenBeforeTextLoads() {
        assertThat(
            StrictModeSurfaceMatcher.isAllowedSettingsSurface(
                className = "com.android.settings.Settings\$AccessibilitySettingsActivity",
                visibleText = ""
            )
        ).isFalse()
    }

    @Test
    fun applicationDetailsSettingsClass_isBlockedEvenBeforeTextLoads() {
        assertThat(
            StrictModeSurfaceMatcher.isAllowedSettingsSurface(
                className = "com.android.settings.Settings\$AppInfoDashboardActivity",
                visibleText = ""
            )
        ).isFalse()
    }
}
