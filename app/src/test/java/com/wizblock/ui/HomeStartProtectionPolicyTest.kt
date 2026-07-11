package com.wizblock.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeStartProtectionPolicyTest {
    @Test
    fun normalWithoutDurationStartsNoLimitProtection() {
        val result = HomeStartProtectionPolicy.resolve(durationMs = null, strictMode = false)

        assertThat(result).isEqualTo(HomeStartProtectionRequest.NormalNoLimit)
    }

    @Test
    fun normalWithDurationStartsTimedFocus() {
        val result = HomeStartProtectionPolicy.resolve(durationMs = 25 * 60_000L, strictMode = false)

        assertThat(result).isEqualTo(HomeStartProtectionRequest.TimedFocus(25 * 60_000L))
    }

    @Test
    fun strictWithDurationStartsStrictTimer() {
        val result = HomeStartProtectionPolicy.resolve(durationMs = 60 * 60_000L, strictMode = true)

        assertThat(result).isEqualTo(HomeStartProtectionRequest.StrictTimer(60 * 60_000L))
    }

    @Test
    fun strictWithoutDurationIsInvalid() {
        val result = HomeStartProtectionPolicy.resolve(durationMs = null, strictMode = true)

        assertThat(result).isEqualTo(HomeStartProtectionRequest.InvalidStrictWithoutDuration)
    }
}
