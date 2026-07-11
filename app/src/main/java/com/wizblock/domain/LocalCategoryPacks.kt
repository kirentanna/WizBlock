package com.wizblock.domain

import android.content.Context
import com.wizblock.model.CategoryPack

object LocalCategoryPacks {
    fun load(context: Context): List<CategoryPack> {
        return DEFAULT_PACKS.map { seed ->
            val assetDomains = runCatching {
                context.assets.open("categories/${seed.id}.txt").bufferedReader().useLines { lines ->
                    lines.map { it.trim() }
                        .filter { it.isNotBlank() && !it.startsWith("#") }
                        .toList()
                }
            }.getOrDefault(seed.domains)
            seed.copy(domains = assetDomains.distinct())
        }
    }

    private val DEFAULT_PACKS = listOf(
        CategoryPack(
            id = "games",
            name = "Games",
            description = "Game sites and play communities",
            domains = listOf("lichess.org", "chess.com", "roblox.com", "miniclip.com", "crazygames.com")
        ),
        CategoryPack(
            id = "shopping",
            name = "Shopping",
            description = "Marketplaces and deal browsing",
            domains = listOf("amazon.com", "ebay.com", "etsy.com", "carousell.com", "shopee.com")
        ),
        CategoryPack(
            id = "social",
            name = "Social",
            description = "Social networks and feeds",
            domains = listOf("facebook.com", "instagram.com", "tiktok.com", "x.com", "twitter.com")
        ),
        CategoryPack(
            id = "video",
            name = "Video",
            description = "Video and streaming sites",
            domains = listOf("youtube.com", "netflix.com", "twitch.tv", "vimeo.com", "dailymotion.com")
        )
    )
}
