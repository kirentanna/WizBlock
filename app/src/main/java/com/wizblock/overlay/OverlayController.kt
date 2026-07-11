package com.wizblock.overlay

import com.wizblock.model.TargetDisplayInfo

data class OverlayBlockDetails(
    val todayAttemptCount: Int? = null
)

interface OverlayController {
    fun setGoBackHandler(handler: (() -> Unit)?)
    fun showBlocked(target: TargetDisplayInfo, reason: String, details: OverlayBlockDetails = OverlayBlockDetails())
    fun hide(reason: String)
}
