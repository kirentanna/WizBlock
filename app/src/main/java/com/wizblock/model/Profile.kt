package com.wizblock.model

const val DEFAULT_PROFILE_ID = "default"

enum class ProfileMode {
    BLOCKLIST,
    ALLOWLIST
}

data class Profile(
    val id: String,
    val name: String,
    val colorToken: String,
    val iconName: String,
    val mode: ProfileMode,
    val enabled: Boolean
) {
    companion object {
        val Default = Profile(
            id = DEFAULT_PROFILE_ID,
            name = "Focus",
            colorToken = "blue",
            iconName = "shield",
            mode = ProfileMode.BLOCKLIST,
            enabled = true
        )
    }
}
