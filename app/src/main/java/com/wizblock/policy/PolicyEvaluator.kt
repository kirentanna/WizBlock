package com.wizblock.policy

import com.wizblock.model.BlockReason
import com.wizblock.model.DEFAULT_PROFILE_ID
import com.wizblock.model.MatchOperator
import com.wizblock.model.ProfileMode
import com.wizblock.model.Rule
import com.wizblock.model.RuleAction
import com.wizblock.model.RuleKind
import com.wizblock.model.TargetType
import com.wizblock.model.UsageLimit

class PolicyEvaluator(
    private val scheduleEvaluator: ScheduleEvaluator
) {
    data class ResolvedTarget(
        val matchedRuleId: String,
        val targetType: TargetType,
        val targetValue: String,
        val schedules: List<com.wizblock.model.ScheduleWindow>,
        val usageLimit: UsageLimit?
    )

    fun resolveTarget(
        context: EnforcementContext,
        snapshot: PolicySnapshot
    ): ResolvedTarget? {
        val enabledProfileIds = enabledProfileIds(snapshot)
        val blockingMatch = snapshot.rules
            .filter { it.enabled && it.action == RuleAction.BLOCK }
            .filter { (it.profileId ?: DEFAULT_PROFILE_ID) in enabledProfileIds }
            .firstOrNull { matchesRule(it, context) }
            ?: return null

        val targetType = when (blockingMatch.kind) {
            RuleKind.DOMAIN -> TargetType.DOMAIN
            RuleKind.APP_PACKAGE -> TargetType.APP_PACKAGE
            RuleKind.KEYWORD -> TargetType.KEYWORD
        }
        val targetValue = blockingMatch.value
        val schedules = snapshot.schedules.filter {
            it.enabled &&
                it.targetType == targetType &&
                it.targetValue.equals(targetValue, ignoreCase = true) &&
                (it.profileId ?: DEFAULT_PROFILE_ID) in enabledProfileIds
        }
        val usageLimit = snapshot.usageLimits.firstOrNull {
            it.enabled &&
                it.targetType == targetType &&
                it.targetValue.equals(targetValue, ignoreCase = true) &&
                (it.profileId ?: DEFAULT_PROFILE_ID) in enabledProfileIds
        }
        return ResolvedTarget(
            matchedRuleId = blockingMatch.id,
            targetType = targetType,
            targetValue = targetValue,
            schedules = schedules,
            usageLimit = usageLimit
        )
    }

    fun evaluate(
        context: EnforcementContext,
        snapshot: PolicySnapshot,
        resolvedTarget: ResolvedTarget? = resolveTarget(context, snapshot),
        usageLimitExceeded: Boolean = false
    ): PolicyDecision {
        val quickBlockActive = snapshot.quickBlockUntilMs > context.nowMs
        if (!snapshot.blockingEnabled && !quickBlockActive) return PolicyDecision.Inactive

        val enabledProfileIds = enabledProfileIds(snapshot)
        val enabledRules = snapshot.rules.filter {
            it.enabled && (it.profileId ?: DEFAULT_PROFILE_ID) in enabledProfileIds
        }
        if (enabledRules.any { it.action == RuleAction.ALLOW && matchesRule(it, context) }) {
            return PolicyDecision.Allowed
        }

        val activeAllowlist = snapshot.profiles.any {
            it.enabled && it.mode == ProfileMode.ALLOWLIST && it.id in enabledProfileIds
        }
        if (activeAllowlist && enabledRules.none { it.action == RuleAction.ALLOW && matchesRule(it, context) }) {
            return PolicyDecision.Blocked(
                targetType = TargetType.APP_PACKAGE,
                targetValue = context.packageName,
                reason = BlockReason.RULE_BLOCK,
                matchedRuleId = null
            )
        }

        if (resolvedTarget != null) {
            val hasSchedules = resolvedTarget.schedules.isNotEmpty()
            val scheduleBlocksNow = resolvedTarget.schedules.any {
                scheduleEvaluator.isWindowActive(it, context.nowMs)
            }
            if (scheduleBlocksNow) {
                return PolicyDecision.Blocked(
                    targetType = resolvedTarget.targetType,
                    targetValue = resolvedTarget.targetValue,
                    reason = BlockReason.RULE_BLOCK,
                    matchedRuleId = resolvedTarget.matchedRuleId
                )
            }

            if (resolvedTarget.usageLimit != null && usageLimitExceeded) {
                return PolicyDecision.Blocked(
                    targetType = resolvedTarget.targetType,
                    targetValue = resolvedTarget.targetValue,
                    reason = BlockReason.USAGE_LIMIT_EXCEEDED,
                    matchedRuleId = null
                )
            }

            if (hasSchedules || resolvedTarget.usageLimit != null) {
                return PolicyDecision.Allowed
            }

            return PolicyDecision.Blocked(
                targetType = resolvedTarget.targetType,
                targetValue = resolvedTarget.targetValue,
                reason = BlockReason.RULE_BLOCK,
                matchedRuleId = resolvedTarget.matchedRuleId
            )
        }

        return PolicyDecision.NotMatched
    }

    private fun enabledProfileIds(snapshot: PolicySnapshot): Set<String> {
        val ids = snapshot.profiles.filter { it.enabled }.map { it.id }.toSet()
        return ids.ifEmpty { setOf(DEFAULT_PROFILE_ID) }
    }

    private fun matchesRule(rule: Rule, context: EnforcementContext): Boolean {
        return when (rule.kind) {
            RuleKind.DOMAIN -> matchesDomain(rule, context.host)
            RuleKind.APP_PACKAGE -> matchesValue(rule.operator, context.packageName, rule.value)
            RuleKind.KEYWORD -> {
                val addressText = context.addressText?.lowercase()?.trim().orEmpty()
                addressText.isNotBlank() && matchesValue(rule.operator, addressText, rule.value.lowercase())
            }
        }
    }

    private fun matchesDomain(rule: Rule, host: String?): Boolean {
        val normalizedHost = host?.lowercase()?.trim().orEmpty()
        if (normalizedHost.isBlank()) return false
        val ruleValue = rule.value.lowercase()
        return when (rule.operator) {
            MatchOperator.EXACT -> normalizedHost == ruleValue
            MatchOperator.SUBDOMAIN -> normalizedHost == ruleValue || normalizedHost.endsWith(".$ruleValue")
            MatchOperator.CONTAINS -> normalizedHost.contains(ruleValue)
        }
    }

    private fun matchesValue(operator: MatchOperator, actualValue: String, ruleValue: String): Boolean {
        return when (operator) {
            MatchOperator.EXACT -> actualValue.equals(ruleValue, ignoreCase = true)
            MatchOperator.SUBDOMAIN -> actualValue.equals(ruleValue, ignoreCase = true) ||
                actualValue.lowercase().endsWith(".${ruleValue.lowercase()}")
            MatchOperator.CONTAINS -> actualValue.lowercase().contains(ruleValue.lowercase())
        }
    }
}
