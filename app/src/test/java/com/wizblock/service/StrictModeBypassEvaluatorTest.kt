package com.wizblock.service

import com.google.common.truth.Truth.assertThat
import com.wizblock.model.StrictModeMode
import com.wizblock.model.StrictModeState
import com.wizblock.model.TargetType
import org.junit.Test

class StrictModeBypassEvaluatorTest {
    @Test
    fun activeStrictMode_blocksAccessibilitySettingsCurrentWindow() {
        val decision = StrictModeBypassEvaluator.evaluate(
            packageName = "com.android.settings",
            className = "com.android.settings.Settings\$AccessibilitySettingsActivity",
            visibleText = "Accessibility Installed apps WizBlock On TalkBack",
            state = activeStrictMode(blockDeviceSettings = true)
        )

        assertThat(decision?.targetType).isEqualTo(TargetType.APP_PACKAGE)
        assertThat(decision?.targetValue).isEqualTo("com.android.settings")
    }

    @Test
    fun activeStrictMode_allowsWifiSettingsCurrentWindow() {
        val decision = StrictModeBypassEvaluator.evaluate(
            packageName = "com.android.settings",
            className = "com.android.settings.Settings\$WifiSettingsActivity",
            visibleText = "Wi-Fi Network preferences Saved networks",
            state = activeStrictMode(blockDeviceSettings = true)
        )

        assertThat(decision).isNull()
    }

    @Test
    fun activeStrictMode_allowsGenericSettingsStartupEvent() {
        val decision = StrictModeBypassEvaluator.evaluate(
            packageName = "com.android.settings",
            className = "android.widget.FrameLayout",
            visibleText = "Home WhatsApp Camera Apps Digital clock",
            state = activeStrictMode(blockDeviceSettings = true)
        )

        assertThat(decision).isNull()
    }

    @Test
    fun inactiveStrictMode_allowsAccessibilitySettingsCurrentWindow() {
        val decision = StrictModeBypassEvaluator.evaluate(
            packageName = "com.android.settings",
            className = "com.android.settings.Settings\$AccessibilitySettingsActivity",
            visibleText = "Accessibility Installed apps WizBlock On TalkBack",
            state = StrictModeState.Inactive
        )

        assertThat(decision).isNull()
    }

    private fun activeStrictMode(blockDeviceSettings: Boolean): StrictModeState {
        return StrictModeState(
            active = true,
            mode = StrictModeMode.TIMER,
            timerExpiresAtMs = Long.MAX_VALUE,
            cooldownDurationMs = 0L,
            cooldownStartedAtMs = 0L,
            blockDeviceSettings = blockDeviceSettings,
            uninstallProtectionEnabled = true
        )
    }
}
