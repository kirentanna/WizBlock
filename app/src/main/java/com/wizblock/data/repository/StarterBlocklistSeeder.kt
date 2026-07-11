package com.wizblock.data.repository

import com.wizblock.model.MatchOperator
import com.wizblock.model.Profile
import com.wizblock.model.ProfileMode
import com.wizblock.model.RuleAction
import com.wizblock.model.RuleKind

class StarterBlocklistSeeder(
    private val profileRepository: ProfileRepository,
    private val ruleRepository: RuleRepository,
    private val isSeeded: suspend () -> Boolean,
    private val markSeeded: suspend () -> Unit
) {
    suspend fun ensureSeeded() {
        if (isSeeded()) return

        STARTER_BLOCKLISTS.forEach { starter ->
            profileRepository.upsert(starter.toProfile())
            starter.apps.forEach {
                ruleRepository.add(RuleKind.APP_PACKAGE, RuleAction.BLOCK, MatchOperator.EXACT, it, starter.id)
            }
            starter.websites.forEach {
                ruleRepository.add(RuleKind.DOMAIN, RuleAction.BLOCK, MatchOperator.SUBDOMAIN, it, starter.id)
            }
            starter.keywords.forEach {
                ruleRepository.add(RuleKind.KEYWORD, RuleAction.BLOCK, MatchOperator.CONTAINS, it, starter.id)
            }
        }

        markSeeded()
    }

    private fun StarterBlocklist.toProfile(): Profile {
        return Profile(
            id = id,
            name = name,
            colorToken = colorToken,
            iconName = iconName,
            mode = ProfileMode.BLOCKLIST,
            enabled = false
        )
    }

    private data class StarterBlocklist(
        val id: String,
        val name: String,
        val colorToken: String,
        val iconName: String,
        val apps: List<String> = emptyList(),
        val websites: List<String> = emptyList(),
        val keywords: List<String> = emptyList()
    )

    private companion object {
        val STARTER_BLOCKLISTS = listOf(
            StarterBlocklist(
                id = "games",
                name = "Games",
                colorToken = "blue",
                iconName = "gamepad",
                apps = listOf("com.roblox.client", "com.supercell.clashofclans"),
                websites = listOf(
                    "lichess.org",
                    "chess.com"
                )
            ),
            StarterBlocklist(
                id = "shopping",
                name = "Shopping",
                colorToken = "blue",
                iconName = "cart",
                websites = listOf(
                    "amazon.com",
                    "ebay.com",
                    "shopee.com"
                )
            ),
            StarterBlocklist(
                id = "social",
                name = "Social",
                colorToken = "blue",
                iconName = "people",
                apps = listOf(
                    "com.instagram.android",
                    "com.zhiliaoapp.musically"
                ),
                websites = listOf(
                    "instagram.com",
                    "tiktok.com"
                ),
                keywords = listOf("reels", "for you")
            ),
            StarterBlocklist(
                id = "video",
                name = "Video",
                colorToken = "blue",
                iconName = "video",
                apps = listOf("com.google.android.youtube", "com.netflix.mediaclient"),
                websites = listOf(
                    "youtube.com",
                    "netflix.com"
                )
            )
        )
    }
}
