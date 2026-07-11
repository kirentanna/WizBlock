package com.wizblock.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MainNavigationPolicyTest {
    @Test
    fun statusRowOpensPermissionDetailsInsteadOfLegacySettings() {
        assertThat(MainDestination.PERMISSION_STATUS.route).isEqualTo("permissions")
        assertThat(MainDestination.PERMISSION_STATUS.route).isNotEqualTo("settings")
    }

    @Test
    fun primaryTabsDoNotExposeLegacyAnalyticsOrSettings() {
        val tabRoutes = MainDestination.primaryTabs.map { it.route }

        assertThat(tabRoutes).containsExactly("home", "blocklist/DOMAIN", "lock").inOrder()
        assertThat(tabRoutes).doesNotContain("settings")
        assertThat(tabRoutes).doesNotContain("analytics")
    }
}
