package com.wizblock.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeHealthUiFormatterTest {
    @Test
    fun cardShowsReadyStateWhenPermissionsReadyButServiceStillStarting() {
        val card = HomeHealthUiFormatter.card(
            PermissionState(
                accessibilityGranted = true,
                overlayGranted = true,
                serviceHealth = ServiceHealth.DEGRADED
            )
        )

        assertThat(card?.title).isEqualTo("All systems ready")
        assertThat(card?.healthy).isTrue()
    }

    @Test
    fun cardShowsPermissionIssueWhenRequiredPermissionMissing() {
        val card = HomeHealthUiFormatter.card(
            PermissionState(
                accessibilityGranted = true,
                overlayGranted = false,
                serviceHealth = ServiceHealth.DEGRADED
            )
        )

        assertThat(card?.title).isEqualTo("Permissions need attention")
        assertThat(card?.healthy).isFalse()
    }

    @Test
    fun cardShowsReadyStateWhenEverythingHealthy() {
        val card = HomeHealthUiFormatter.card(
            PermissionState(
                accessibilityGranted = true,
                overlayGranted = true,
                serviceHealth = ServiceHealth.HEALTHY
            )
        )

        assertThat(card?.title).isEqualTo("All systems ready")
        assertThat(card?.healthy).isTrue()
    }
}
