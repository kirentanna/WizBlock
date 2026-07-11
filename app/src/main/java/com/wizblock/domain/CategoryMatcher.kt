package com.wizblock.domain

import com.wizblock.model.CategoryPack

class CategoryMatcher(
    private val packs: List<CategoryPack>
) {
    val categoryPacks: List<CategoryPack> = packs.sortedBy { it.name.lowercase() }

    fun match(host: String?, addressText: String?, enabledCategoryIds: Set<String>): CategoryMatch? {
        if (enabledCategoryIds.isEmpty()) return null
        val normalizedHost = host.orEmpty().lowercase().trim().removePrefix("www.")
        val normalizedAddress = addressText.orEmpty().lowercase()
        if (normalizedHost.isBlank() && normalizedAddress.isBlank()) return null

        val enabled = packs.filter { it.id in enabledCategoryIds }
        return enabled.firstNotNullOfOrNull { pack ->
            val domain = pack.domains.firstOrNull { candidate ->
                val normalizedDomain = candidate.lowercase().trim().removePrefix("www.")
                normalizedHost == normalizedDomain ||
                    normalizedHost.endsWith(".$normalizedDomain") ||
                    normalizedAddress.contains(normalizedDomain)
            }
            domain?.let { CategoryMatch(pack.id, pack.name, normalizedHost.ifBlank { domain }) }
        }
    }
}

data class CategoryMatch(
    val categoryId: String,
    val categoryName: String,
    val targetValue: String
)
