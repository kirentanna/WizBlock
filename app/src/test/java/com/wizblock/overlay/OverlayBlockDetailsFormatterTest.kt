package com.wizblock.overlay

import com.google.common.truth.Truth.assertThat
import com.wizblock.model.TargetDisplayInfo
import com.wizblock.model.TargetKey
import com.wizblock.model.TargetType
import org.junit.Test

class OverlayBlockDetailsFormatterTest {
    @Test
    fun rowsShowTargetAttemptsReasonMessageAndLocalSource() {
        val target = TargetDisplayInfo(
            targetKey = TargetKey(TargetType.DOMAIN, "chess.com"),
            title = "Chess.com",
            subtitle = "Website"
        )
        val details = OverlayBlockDetails(todayAttemptCount = 4)

        val rows = OverlayBlockDetailsFormatter.rows(target, "Focus mode", details)

        assertThat(rows.map { it.label }).containsExactly(
            "Target",
            "Today",
            "Reason",
            "Message",
            "Source"
        ).inOrder()
        assertThat(rows.map { it.value }).containsExactly(
            "Chess.com",
            "Blocked 4 times",
            "Focus mode - Website",
            "Good call. Stay with the task.",
            "Local rule"
        ).inOrder()
    }
}
