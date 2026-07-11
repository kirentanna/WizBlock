package com.wizblock.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PermissionStatusUiFormatterTest {
    @Test
    fun permissionRowsOnlyIncludeRealPermissions() {
        val rows = PermissionStatusUiFormatter.permissionRows(
            PermissionState(
                accessibilityGranted = true,
                overlayGranted = true,
                serviceHealth = ServiceHealth.DEGRADED
            )
        )

        assertThat(rows.map { it.title }).containsExactly("Accessibility", "Overlay permission").inOrder()
        assertThat(rows.map { it.title }).doesNotContain("Blocking service")
    }
}
