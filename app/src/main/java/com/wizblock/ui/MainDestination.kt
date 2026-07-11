package com.wizblock.ui

data class NavigationDestination(val route: String)

object MainDestination {
    val ONBOARDING = NavigationDestination("onboarding")
    val HOME = NavigationDestination("home")
    val PERMISSION_STATUS = NavigationDestination("permissions")
    val RECENT_HISTORY = NavigationDestination("history")
    val LOCK = NavigationDestination("lock")
    val BLOCKLIST_PATTERN = NavigationDestination("blocklist/{tab}")
    val BLOCKLISTS = NavigationDestination("blocklist/DOMAIN")

    const val BLOCKLIST_PREFIX = "blocklist"

    val primaryTabs = listOf(HOME, BLOCKLISTS, LOCK)
}
