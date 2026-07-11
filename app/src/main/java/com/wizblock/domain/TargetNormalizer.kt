package com.wizblock.domain

import com.wizblock.model.RuleKind
import com.wizblock.model.TargetKey
import com.wizblock.model.TargetType

class TargetNormalizer(
    private val domainNormalizer: DomainNormalizer
) {
    fun normalize(targetType: TargetType, rawValue: String): TargetKey? {
        val value = rawValue.trim()
        if (value.isBlank()) return null
        val normalizedValue = when (targetType) {
            TargetType.DOMAIN -> when (val normalized = domainNormalizer.normalize(value)) {
                is NormalizedDomainResult.Success -> normalized.domain
                is NormalizedDomainResult.Error -> return null
            }
            TargetType.APP_PACKAGE -> value
            TargetType.KEYWORD -> value.lowercase()
        }
        return TargetKey(targetType, normalizedValue)
    }

    fun normalize(ruleKind: RuleKind, rawValue: String): TargetKey? {
        return normalize(ruleKind.toTargetType(), rawValue)
    }
}

fun RuleKind.toTargetType(): TargetType {
    return when (this) {
        RuleKind.DOMAIN -> TargetType.DOMAIN
        RuleKind.APP_PACKAGE -> TargetType.APP_PACKAGE
        RuleKind.KEYWORD -> TargetType.KEYWORD
    }
}
