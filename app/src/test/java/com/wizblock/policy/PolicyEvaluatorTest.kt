package com.wizblock.policy

import com.google.common.truth.Truth.assertThat
import com.wizblock.model.BlockReason
import com.wizblock.model.MatchOperator
import com.wizblock.model.Rule
import com.wizblock.model.RuleAction
import com.wizblock.model.RuleKind
import com.wizblock.model.TargetType
import com.wizblock.model.UsageLimit
import org.junit.Test

class PolicyEvaluatorTest {
    private val evaluator = PolicyEvaluator(ScheduleEvaluator())

    @Test
    fun evaluate_allowWinsOverBlock() {
        val now = System.currentTimeMillis()
        val snapshot = PolicySnapshot(
            blockingEnabled = true,
            quickBlockUntilMs = now + 10_000,
            rules = listOf(
                rule("allow", RuleAction.ALLOW, RuleKind.DOMAIN, MatchOperator.EXACT, "example.com"),
                rule("block", RuleAction.BLOCK, RuleKind.DOMAIN, MatchOperator.EXACT, "example.com")
            ),
            schedules = emptyList(),
            usageLimits = emptyList()
        )

        val decision = evaluator.evaluate(
            context = EnforcementContext(
                packageName = "com.android.chrome",
                host = "example.com",
                addressText = "https://example.com",
                nowMs = now
            ),
            snapshot = snapshot
        )

        assertThat(decision).isEqualTo(PolicyDecision.Allowed)
    }

    @Test
    fun evaluate_keywordContainsBlocks() {
        val now = System.currentTimeMillis()
        val snapshot = PolicySnapshot(
            blockingEnabled = true,
            quickBlockUntilMs = now + 10_000,
            rules = listOf(
                rule("keyword", RuleAction.BLOCK, RuleKind.KEYWORD, MatchOperator.CONTAINS, "sports")
            ),
            schedules = emptyList(),
            usageLimits = emptyList()
        )

        val decision = evaluator.evaluate(
            context = EnforcementContext(
                packageName = "com.android.chrome",
                host = "news.example.com",
                addressText = "https://news.example.com/sports/today",
                nowMs = now
            ),
            snapshot = snapshot
        )

        assertThat(decision).isInstanceOf(PolicyDecision.Blocked::class.java)
        val blocked = decision as PolicyDecision.Blocked
        assertThat(blocked.reason).isEqualTo(BlockReason.RULE_BLOCK)
        assertThat(blocked.targetType).isEqualTo(TargetType.KEYWORD)
    }

    @Test
    fun evaluate_usageLimitExceededBlocksWhenNoRuleMatch() {
        val now = System.currentTimeMillis()
        val snapshot = PolicySnapshot(
            blockingEnabled = true,
            quickBlockUntilMs = now + 10_000,
            rules = listOf(
                rule("app-rule", RuleAction.BLOCK, RuleKind.APP_PACKAGE, MatchOperator.EXACT, "com.instagram.android")
            ),
            schedules = emptyList(),
            usageLimits = listOf(
                UsageLimit(
                    id = "l1",
                    targetType = TargetType.APP_PACKAGE,
                    targetValue = "com.instagram.android",
                    minutesPerDay = 30,
                    opensPerDay = 10,
                    enabled = true,
                    createdAt = now,
                    updatedAt = now
                )
            )
        )

        val decision = evaluator.evaluate(
            context = EnforcementContext(
                packageName = "com.instagram.android",
                host = null,
                addressText = null,
                nowMs = now
            ),
            snapshot = snapshot,
            resolvedTarget = evaluator.resolveTarget(
                context = EnforcementContext(
                    packageName = "com.instagram.android",
                    host = null,
                    addressText = null,
                    nowMs = now
                ),
                snapshot = snapshot
            ),
            usageLimitExceeded = true
        )

        assertThat(decision).isEqualTo(
            PolicyDecision.Blocked(
                targetType = TargetType.APP_PACKAGE,
                targetValue = "com.instagram.android",
                reason = BlockReason.USAGE_LIMIT_EXCEEDED,
                matchedRuleId = null
            )
        )
    }

    @Test
    fun evaluate_usageLimitAllowsUntilExceeded() {
        val now = System.currentTimeMillis()
        val snapshot = PolicySnapshot(
            blockingEnabled = true,
            quickBlockUntilMs = now + 10_000,
            rules = listOf(
                rule("domain-rule", RuleAction.BLOCK, RuleKind.DOMAIN, MatchOperator.SUBDOMAIN, "lichess.org")
            ),
            schedules = emptyList(),
            usageLimits = listOf(
                UsageLimit(
                    id = "l2",
                    targetType = TargetType.DOMAIN,
                    targetValue = "lichess.org",
                    minutesPerDay = 15,
                    opensPerDay = 0,
                    enabled = true,
                    createdAt = now,
                    updatedAt = now
                )
            )
        )

        val decision = evaluator.evaluate(
            context = EnforcementContext(
                packageName = "com.opera.browser",
                host = "lichess.org",
                addressText = "https://lichess.org",
                nowMs = now
            ),
            snapshot = snapshot,
            resolvedTarget = evaluator.resolveTarget(
                context = EnforcementContext(
                    packageName = "com.opera.browser",
                    host = "lichess.org",
                    addressText = "https://lichess.org",
                    nowMs = now
                ),
                snapshot = snapshot
            ),
            usageLimitExceeded = false
        )

        assertThat(decision).isEqualTo(PolicyDecision.Allowed)
    }

    private fun rule(
        id: String,
        action: RuleAction,
        kind: RuleKind,
        operator: MatchOperator,
        value: String
    ): Rule {
        val now = System.currentTimeMillis()
        return Rule(
            id = id,
            kind = kind,
            action = action,
            operator = operator,
            value = value,
            enabled = true,
            createdAt = now,
            updatedAt = now
        )
    }
}
