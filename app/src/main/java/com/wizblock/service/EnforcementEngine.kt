package com.wizblock.service

import com.wizblock.browser.BrowserHeuristics
import com.wizblock.browser.ExtractionStatus
import com.wizblock.domain.CategoryMatcher
import com.wizblock.model.BlockReason
import com.wizblock.model.RuleAction
import com.wizblock.model.RuleKind
import com.wizblock.model.TargetKey
import com.wizblock.model.TargetType
import com.wizblock.model.UsageCounter
import com.wizblock.model.UsageLimit
import com.wizblock.policy.EnforcementContext
import com.wizblock.policy.PolicyDecision
import com.wizblock.policy.PolicyEvaluator
import com.wizblock.policy.PolicySnapshot

data class EnforcementInput(
    val packageName: String,
    val extraction: ExtractionStatus,
    val policySnapshot: PolicySnapshot,
    val usageCountersByTarget: Map<TargetKey, UsageCounter>,
    val nowMs: Long,
    val strictModeDecision: PolicyDecision.Blocked? = null
)

data class EnforcementOptions(
    val blockNewlyInstalledApps: Boolean = false,
    val blockUnsupportedBrowsers: Boolean = false,
    val enabledCategoryIds: Set<String> = emptySet()
)

class EnforcementEngine(
    private val policyEvaluator: PolicyEvaluator,
    private val isRecentlyInstalledNonSystemApp: (String) -> Boolean,
    private val categoryMatcher: CategoryMatcher? = null
) {
    fun evaluate(input: EnforcementInput, options: EnforcementOptions): PolicyDecision {
        input.strictModeDecision?.let { return it }

        val host = (input.extraction as? ExtractionStatus.Success)?.host
        val addressText = (input.extraction as? ExtractionStatus.Success)?.addressText
        val context = EnforcementContext(
            packageName = input.packageName,
            host = host,
            addressText = addressText,
            nowMs = input.nowMs
        )
        val resolvedTarget = policyEvaluator.resolveTarget(context, input.policySnapshot)
        val usageLimitExceeded = resolvedTarget?.usageLimit?.let { limit ->
            isUsageLimitExceeded(limit, input.usageCountersByTarget[TargetKey(limit.targetType, limit.targetValue)])
        } ?: false

        val ruleDecision = policyEvaluator.evaluate(
            context = context,
            snapshot = input.policySnapshot,
            resolvedTarget = resolvedTarget,
            usageLimitExceeded = usageLimitExceeded
        )
        if (ruleDecision is PolicyDecision.Blocked) return ruleDecision

        return evaluateOptions(input, options, host, addressText) ?: ruleDecision
    }

    fun resolvedAllowedUsageTarget(input: EnforcementInput, finalDecision: PolicyDecision): TargetKey? {
        if (finalDecision != PolicyDecision.Allowed) return null
        val host = (input.extraction as? ExtractionStatus.Success)?.host
        val addressText = (input.extraction as? ExtractionStatus.Success)?.addressText
        val context = EnforcementContext(
            packageName = input.packageName,
            host = host,
            addressText = addressText,
            nowMs = input.nowMs
        )
        val resolved = policyEvaluator.resolveTarget(context, input.policySnapshot) ?: return null
        if (resolved.usageLimit == null) return null
        return TargetKey(resolved.targetType, resolved.targetValue)
    }

    private fun evaluateOptions(
        input: EnforcementInput,
        options: EnforcementOptions,
        host: String?,
        addressText: String?
    ): PolicyDecision.Blocked? {
        val quickBlockActive = input.policySnapshot.quickBlockUntilMs > input.nowMs
        if (!input.policySnapshot.blockingEnabled && !quickBlockActive) return null

        if (options.blockNewlyInstalledApps && isRecentlyInstalledNonSystemApp(input.packageName)) {
            return blocked(TargetType.APP_PACKAGE, input.packageName)
        }

        if (
            options.blockUnsupportedBrowsers &&
            input.extraction is ExtractionStatus.UnsupportedBrowser &&
            shouldBlockUnsupportedBrowser(input.packageName, input.policySnapshot)
        ) {
            return blocked(TargetType.APP_PACKAGE, input.packageName)
        }

        val categoryMatch = categoryMatcher?.match(host, addressText, options.enabledCategoryIds)
        if (input.extraction is ExtractionStatus.Success && categoryMatch != null) {
            return PolicyDecision.Blocked(
                targetType = TargetType.DOMAIN,
                targetValue = categoryMatch.targetValue,
                reason = BlockReason.CATEGORY_BLOCK,
                matchedRuleId = null
            )
        }

        return null
    }

    private fun shouldBlockUnsupportedBrowser(packageName: String, snapshot: PolicySnapshot): Boolean {
        if (!BrowserHeuristics.isLikelyBrowser(packageName)) return false
        return snapshot.rules.any { rule ->
            rule.enabled &&
                rule.action == RuleAction.BLOCK &&
                (rule.kind == RuleKind.DOMAIN || rule.kind == RuleKind.KEYWORD)
        }
    }

    private fun isUsageLimitExceeded(limit: UsageLimit, counter: UsageCounter?): Boolean {
        if (counter == null) return false
        val minuteExceeded = limit.minutesPerDay > 0 && counter.usedSeconds >= limit.minutesPerDay * 60
        val opensExceeded = limit.opensPerDay > 0 && counter.openCount >= limit.opensPerDay
        return minuteExceeded || opensExceeded
    }

    private fun blocked(targetType: TargetType, targetValue: String): PolicyDecision.Blocked {
        return PolicyDecision.Blocked(
            targetType = targetType,
            targetValue = targetValue,
            reason = BlockReason.RULE_BLOCK,
            matchedRuleId = null
        )
    }

}
