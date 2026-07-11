package com.wizblock.ui

data class HomeStrictModeControlState(
    val display: HomeStrictModeControlDisplay,
    val strictSelected: Boolean,
    val action: HomeStrictModeControlAction
)

enum class HomeStrictModeControlDisplay {
    Selectable,
    DurationRequired,
    UpgradeTimedFocus,
    UpgradeSelectedDuration,
    Locked
}

enum class HomeStrictModeControlAction {
    SetLocalSelection,
    UpgradeTimedFocus,
    UpgradeSelectedDuration,
    ShowDurationRequired,
    None
}

object HomeStrictModeControlPolicy {
    fun resolve(
        strictModeActive: Boolean,
        selectedDurationMs: Long?,
        blockingEnabled: Boolean,
        focusUntilMs: Long,
        nowMs: Long,
        localStrictModeSelected: Boolean
    ): HomeStrictModeControlState {
        val timedFocusRunning = blockingEnabled && !strictModeActive && focusUntilMs > nowMs
        return when {
            strictModeActive -> HomeStrictModeControlState(
                display = HomeStrictModeControlDisplay.Locked,
                strictSelected = true,
                action = HomeStrictModeControlAction.None
            )

            timedFocusRunning -> HomeStrictModeControlState(
                display = HomeStrictModeControlDisplay.UpgradeTimedFocus,
                strictSelected = false,
                action = HomeStrictModeControlAction.UpgradeTimedFocus
            )

            blockingEnabled && selectedDurationMs != null -> HomeStrictModeControlState(
                display = HomeStrictModeControlDisplay.UpgradeSelectedDuration,
                strictSelected = false,
                action = HomeStrictModeControlAction.UpgradeSelectedDuration
            )

            selectedDurationMs != null && !blockingEnabled -> HomeStrictModeControlState(
                display = HomeStrictModeControlDisplay.Selectable,
                strictSelected = localStrictModeSelected,
                action = HomeStrictModeControlAction.SetLocalSelection
            )

            else -> HomeStrictModeControlState(
                display = HomeStrictModeControlDisplay.DurationRequired,
                strictSelected = false,
                action = HomeStrictModeControlAction.ShowDurationRequired
            )
        }
    }
}
