package com.wizblock.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SensitivePermissionDisclosureTest {
    @Test
    fun accessibilityDisclosureNamesLocalUseAndConsent() {
        val disclosure = SensitivePermissionDisclosure.accessibilityBody

        assertThat(disclosure).contains("active app")
        assertThat(disclosure).contains("browser URL")
        assertThat(disclosure).contains("on this device")
        assertThat(SensitivePermissionDisclosure.accessibilityAction).contains("I understand")
    }

    @Test
    fun deviceAdminDisclosureNamesUninstallProtectionAndDisablePath() {
        val disclosure = SensitivePermissionDisclosure.deviceAdminBody

        assertThat(disclosure).contains("Device Admin")
        assertThat(disclosure).contains("prevent normal uninstall")
        assertThat(disclosure).contains("Strict Mode")
        assertThat(disclosure).contains("timer ends")
        assertThat(SensitivePermissionDisclosure.deviceAdminConfirmAction).contains("I understand")
    }
}
