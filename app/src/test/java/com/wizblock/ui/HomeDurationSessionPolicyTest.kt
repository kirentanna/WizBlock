package com.wizblock.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeDurationSessionPolicyTest {
    @Test
    fun defaultQuickPresetsAreTwentyFiveMinutesOneHourAndThreeHours() {
        assertThat(HomeDurationSessionPolicy.defaultPresetDurationsMs()).containsExactly(
            25L * 60_000L,
            60L * 60_000L,
            3L * 60L * 60_000L
        ).inOrder()
    }

    @Test
    fun durationLabelsUseCompactMinuteHourAndDayCopy() {
        assertThat(HomeDurationSessionPolicy.labelForDuration(25L * 60_000L)).isEqualTo("25m")
        assertThat(HomeDurationSessionPolicy.labelForDuration(90L * 60_000L)).isEqualTo("1h 30m")
        assertThat(HomeDurationSessionPolicy.labelForDuration(25L * 60L * 60_000L)).isEqualTo("1d 1h")
    }

    @Test
    fun customDurationSupportsMinutesHoursAndDaysWithFiveMinuteMinimum() {
        assertThat(HomeDurationSessionPolicy.customDurationMs(minutes = 0, hours = 0, days = 0))
            .isEqualTo(5L * 60_000L)
        assertThat(HomeDurationSessionPolicy.customDurationMs(minutes = 25, hours = 1, days = 2))
            .isEqualTo(((2L * 24L * 60L) + 60L + 25L) * 60_000L)
    }

    @Test
    fun savedDurationsAreClampedToSupportedRange() {
        val presets = HomeDurationSessionPolicy.normalizePresetDurationsMs(
            listOf(0L, 60_000L, 30L * 24L * 60L * 60_000L)
        )

        assertThat(presets).containsExactly(
            5L * 60_000L,
            5L * 60_000L,
            14L * 24L * 60L * 60_000L
        ).inOrder()
    }
}
