package com.wizblock.ui

import com.wizblock.model.StrictModeState

enum class HomeProtectionVisualMode {
    Off,
    NormalActive,
    StrictActive
}

data class HomeProtectionVisualTreatment(
    val showWizBlockIcon: Boolean,
    val showLock: Boolean,
    val lockCenterXFraction: Float,
    val lockCenterYFraction: Float,
    val lockVisualCenterYFraction: Float
)

object HomeProtectionVisualPolicy {
    fun resolve(
        blockingEnabled: Boolean,
        strictModeState: StrictModeState,
        nowMs: Long
    ): HomeProtectionVisualMode {
        return when {
            strictModeState.active && strictModeState.remainingMs(nowMs) > 0L -> HomeProtectionVisualMode.StrictActive
            blockingEnabled -> HomeProtectionVisualMode.NormalActive
            else -> HomeProtectionVisualMode.Off
        }
    }

    fun treatmentFor(mode: HomeProtectionVisualMode): HomeProtectionVisualTreatment {
        val active = mode != HomeProtectionVisualMode.Off
        return HomeProtectionVisualTreatment(
            showWizBlockIcon = active,
            showLock = active,
            lockCenterXFraction = 0.5f,
            lockCenterYFraction = 0.80f,
            lockVisualCenterYFraction = 0.80f
        )
    }
}
