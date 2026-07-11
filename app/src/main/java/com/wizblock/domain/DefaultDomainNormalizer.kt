package com.wizblock.domain

import java.net.IDN
import java.net.URI

class DefaultDomainNormalizer : DomainNormalizer {
    override fun normalize(input: String): NormalizedDomainResult {
        val cleaned = input.trim().lowercase()
        if (cleaned.isEmpty()) {
            return NormalizedDomainResult.Error(ErrorReason.Empty)
        }

        val candidate = cleaned.substringBefore(' ')
        val host = extractHost(candidate) ?: return NormalizedDomainResult.Error(ErrorReason.InvalidHost)

        val normalized = host.removePrefix("www.")
        if (!HOST_REGEX.matches(normalized)) {
            return NormalizedDomainResult.Error(ErrorReason.InvalidHost)
        }

        return NormalizedDomainResult.Success(normalized)
    }

    private fun extractHost(candidate: String): String? {
        return if (candidate.contains("://")) {
            parseHost(candidate)
        } else {
            parseHost("https://$candidate") ?: parseHost(candidate)
        }
    }

    private fun parseHost(raw: String): String? {
        return try {
            val uri = URI(raw)
            val host = uri.host ?: fallbackHostFromRaw(raw) ?: return null
            val ascii = IDN.toASCII(host.lowercase().trim('.'))
            if (HOST_REGEX.matches(ascii)) ascii else null
        } catch (_: Exception) {
            val fallback = fallbackHostFromRaw(raw) ?: return null
            val ascii = runCatching { IDN.toASCII(fallback.lowercase().trim('.')) }.getOrNull() ?: return null
            if (HOST_REGEX.matches(ascii)) ascii else null
        }
    }

    private fun fallbackHostFromRaw(raw: String): String? {
        val candidate = raw
            .substringAfter("://", raw)
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .substringBefore(':')
            .trim()
        return candidate.ifBlank { null }
    }

    private companion object {
        val HOST_REGEX = Regex("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$")
    }
}
