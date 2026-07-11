package com.wizblock.browser

import java.net.URI

object UrlTextParser {
    fun extractHost(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.trim().substringBefore(' ').trim()
        if (cleaned.isEmpty()) return null

        val candidates = if (cleaned.contains("://")) {
            listOf(cleaned)
        } else {
            listOf("https://$cleaned", cleaned)
        }

        candidates.forEach { candidate ->
            parseUriHost(candidate)?.let { return it }
            parseRegexHost(candidate)?.let { return it }
        }
        return null
    }

    private fun parseUriHost(raw: String): String? {
        val host = try {
            URI(raw).host?.lowercase()?.trim('.')?.removePrefix("www.")
        } catch (_: Exception) {
            null
        }
        return host?.takeIf { isLikelyHost(it) }
    }

    private fun parseRegexHost(raw: String): String? {
        return HOST_REGEX.find(raw.lowercase())?.value?.removePrefix("www.")?.takeIf { isLikelyHost(it) }
    }

    private fun isLikelyHost(host: String): Boolean {
        if (!host.contains('.')) return false
        return HOST_PATTERN.matches(host)
    }

    private val HOST_REGEX = Regex("(?:https?://)?(?:www\\.)?[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+")
    private val HOST_PATTERN = Regex("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$")
}
