package com.wizblock.ui

import com.google.common.truth.Truth.assertThat
import com.wizblock.model.StrictModeState
import org.junit.Test

class HomeProtectionVisualPolicyTest {
    @Test
    fun protectionOffUsesPlayVisual() {
        val mode = HomeProtectionVisualPolicy.resolve(
            blockingEnabled = false,
            strictModeState = StrictModeState.Inactive,
            nowMs = 1_000L
        )

        assertThat(mode).isEqualTo(HomeProtectionVisualMode.Off)
    }

    @Test
    fun normalProtectionUsesShieldVisual() {
        val mode = HomeProtectionVisualPolicy.resolve(
            blockingEnabled = true,
            strictModeState = StrictModeState.Inactive,
            nowMs = 1_000L
        )

        assertThat(mode).isEqualTo(HomeProtectionVisualMode.NormalActive)
    }

    @Test
    fun normalProtectionShowsLogoWithLowerCenteredLock() {
        val mode = HomeProtectionVisualPolicy.resolve(
            blockingEnabled = true,
            strictModeState = StrictModeState.Inactive,
            nowMs = 1_000L
        )

        val treatment = HomeProtectionVisualPolicy.treatmentFor(mode)

        assertThat(treatment.showWizBlockIcon).isTrue()
        assertThat(treatment.showLock).isTrue()
        assertThat(treatment.lockCenterXFraction).isEqualTo(0.5f)
        assertThat(treatment.lockCenterYFraction).isEqualTo(0.80f)
        assertThat(treatment.lockVisualCenterYFraction).isEqualTo(0.80f)
    }

    @Test
    fun activeStrictModeUsesLockedShieldVisual() {
        val mode = HomeProtectionVisualPolicy.resolve(
            blockingEnabled = true,
            strictModeState = StrictModeState.Inactive.copy(
                active = true,
                timerExpiresAtMs = 2_000L
            ),
            nowMs = 1_000L
        )

        assertThat(mode).isEqualTo(HomeProtectionVisualMode.StrictActive)
    }

    @Test
    fun activeStrictModeShowsLogoWithLowerCenteredLock() {
        val mode = HomeProtectionVisualPolicy.resolve(
            blockingEnabled = true,
            strictModeState = StrictModeState.Inactive.copy(
                active = true,
                timerExpiresAtMs = 2_000L
            ),
            nowMs = 1_000L
        )

        val treatment = HomeProtectionVisualPolicy.treatmentFor(mode)

        assertThat(treatment.showWizBlockIcon).isTrue()
        assertThat(treatment.showLock).isTrue()
        assertThat(treatment.lockCenterXFraction).isEqualTo(0.5f)
        assertThat(treatment.lockCenterYFraction).isEqualTo(0.80f)
        assertThat(treatment.lockVisualCenterYFraction).isEqualTo(0.80f)
    }
}
