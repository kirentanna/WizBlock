package com.wizblock.ui

sealed interface HomeStartProtectionRequest {
    data object NormalNoLimit : HomeStartProtectionRequest
    data class TimedFocus(val durationMs: Long) : HomeStartProtectionRequest
    data class StrictTimer(val durationMs: Long) : HomeStartProtectionRequest
    data object InvalidStrictWithoutDuration : HomeStartProtectionRequest
}

object HomeStartProtectionPolicy {
    fun resolve(durationMs: Long?, strictMode: Boolean): HomeStartProtectionRequest {
        return when {
            strictMode && durationMs == null -> HomeStartProtectionRequest.InvalidStrictWithoutDuration
            strictMode -> HomeStartProtectionRequest.StrictTimer(durationMs!!.coerceAtLeast(60_000L))
            durationMs != null -> HomeStartProtectionRequest.TimedFocus(durationMs.coerceAtLeast(60_000L))
            else -> HomeStartProtectionRequest.NormalNoLimit
        }
    }
}
