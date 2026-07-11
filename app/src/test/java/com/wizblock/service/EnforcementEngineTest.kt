package com.wizblock.service

import com.google.common.truth.Truth.assertThat
import com.wizblock.browser.ExtractionStatus
import com.wizblock.domain.CategoryMatcher
import com.wizblock.model.BlockReason
import com.wizblock.model.CategoryPack
import com.wizblock.model.MatchOperator
import com.wizblock.model.Rule
import com.wizblock.model.RuleAction
import com.wizblock.model.RuleKind
import com.wizblock.model.TargetKey
import com.wizblock.model.TargetType
import com.wizblock.model.UsageCounter
import com.wizblock.model.UsageLimit
import com.wizblock.policy.PolicyDecision
import com.wizblock.policy.PolicyEvaluator
import com.wizblock.policy.PolicySnapshot
import com.wizblock.policy.ScheduleEvaluator
import org.junit.Test

class EnforcementEngineTest {
    private val engine = EnforcementEngine(
        policyEvaluator = PolicyEvaluator(ScheduleEvaluator()),
        isRecentlyInstalledNonSystemApp = { false },
        categoryMatcher = CategoryMatcher(
            listOf(
                CategoryPack(
                    id = "games",
                    name = "Games",
                    description = "Game sites",
                    domains = listOf("game-example.com")
                )
            )
        )
    )

    @Test
    fun evaluate_usageLimitExceededBlocksResolvedTarget() {
        val now = System.currentTimeMillis()
        val packageName = "com.instagram.android"
        val decision = engine.evaluate(
            input = EnforcementInput(
                packageName = packageName,
                extraction = ExtractionStatus.UnsupportedBrowser,
                policySnapshot = snapshot(
                    rules = listOf(rule("app", RuleKind.APP_PACKAGE, MatchOperator.EXACT, packageName)),
                    usageLimits = listOf(
                        UsageLimit(
                            id = "limit",
                            targetType = TargetType.APP_PACKAGE,
                            targetValue = packageName,
                            minutesPerDay = 0,
                            opensPerDay = 3,
                            enabled = true,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                ),
                usageCountersByTarget = mapOf(
                    TargetKey(TargetType.APP_PACKAGE, packageName) to UsageCounter(
                        id = "counter",
                        localDate = "2026-06-26",
                        targetType = TargetType.APP_PACKAGE,
                        targetValue = packageName,
                        usedSeconds = 0,
                        openCount = 3,
                        updatedAt = now
                    )
                ),
                nowMs = now
            ),
            options = EnforcementOptions()
        )

        assertThat(decision).isEqualTo(
            PolicyDecision.Blocked(
                targetType = TargetType.APP_PACKAGE,
                targetValue = packageName,
                reason = BlockReason.USAGE_LIMIT_EXCEEDED,
                matchedRuleId = null
            )
        )
    }

    @Test
    fun evaluate_unsupportedBrowserOptionBlocksBrowserWhenWebsiteRulesExist() {
        val now = System.currentTimeMillis()
        val decision = engine.evaluate(
            input = EnforcementInput(
                packageName = "com.unknown.browser",
                extraction = ExtractionStatus.UnsupportedBrowser,
                policySnapshot = snapshot(
                    rules = listOf(rule("domain", RuleKind.DOMAIN, MatchOperator.SUBDOMAIN, "example.com"))
                ),
                usageCountersByTarget = emptyMap(),
                nowMs = now
            ),
            options = EnforcementOptions(blockUnsupportedBrowsers = true)
        )

        assertThat(decision).isEqualTo(
            PolicyDecision.Blocked(
                targetType = TargetType.APP_PACKAGE,
                targetValue = "com.unknown.browser",
                reason = BlockReason.RULE_BLOCK,
                matchedRuleId = null
            )
        )
    }

    private fun snapshot(
        rules: List<Rule> = emptyList(),
        usageLimits: List<UsageLimit> = emptyList()
    ): PolicySnapshot {
        return PolicySnapshot(
            blockingEnabled = true,
            quickBlockUntilMs = 0L,
            rules = rules,
            schedules = emptyList(),
            usageLimits = usageLimits
        )
    }

    private fun rule(
        id: String,
        kind: RuleKind,
        operator: MatchOperator,
        value: String
    ): Rule {
        val now = System.currentTimeMillis()
        return Rule(
            id = id,
            kind = kind,
            action = RuleAction.BLOCK,
            operator = operator,
            value = value,
            enabled = true,
            createdAt = now,
            updatedAt = now
        )
    }
}
