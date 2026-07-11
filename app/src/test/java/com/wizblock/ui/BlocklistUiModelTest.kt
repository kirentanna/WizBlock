package com.wizblock.ui

import com.google.common.truth.Truth.assertThat
import com.wizblock.model.DEFAULT_PROFILE_ID
import com.wizblock.model.MatchOperator
import com.wizblock.model.Profile
import com.wizblock.model.ProfileMode
import com.wizblock.model.Rule
import com.wizblock.model.RuleAction
import com.wizblock.model.RuleKind
import com.wizblock.model.TargetType
import org.junit.Test

class BlocklistUiModelTest {
    @Test
    fun summarizeBlocklists_countsMixedRulesByNamedBlocklist() {
        val summaries = BlocklistUiModel.summarizeBlocklists(
            profiles = listOf(profile("evening", "Evening Scroll"), profile("reading", "Reading")),
            rules = listOf(
                rule("app-1", RuleKind.APP_PACKAGE, "com.instagram.android", "evening"),
                rule("app-2", RuleKind.APP_PACKAGE, "com.zhiliaoapp.musically", "evening"),
                rule("web-1", RuleKind.DOMAIN, "nytimes.com", "reading"),
                rule("web-2", RuleKind.DOMAIN, "bbc.com", "reading"),
                rule("keyword-1", RuleKind.KEYWORD, "headlines", "reading")
            )
        )

        assertThat(summaries.map { it.name }).containsExactly("Evening Scroll", "Reading").inOrder()
        assertThat(summaries.first { it.id == "evening" }.appCount).isEqualTo(2)
        assertThat(summaries.first { it.id == "evening" }.websiteCount).isEqualTo(0)
        assertThat(summaries.first { it.id == "reading" }.websiteCount).isEqualTo(2)
        assertThat(summaries.first { it.id == "reading" }.keywordCount).isEqualTo(1)
    }

    @Test
    fun summarizeBlocklists_excludesDefaultAdHocProfile() {
        val summaries = BlocklistUiModel.summarizeBlocklists(
            profiles = listOf(Profile.Default, profile("evening", "Evening Scroll")),
            rules = listOf(
                rule("ad-hoc", RuleKind.APP_PACKAGE, "com.android.chrome", DEFAULT_PROFILE_ID),
                rule("saved", RuleKind.APP_PACKAGE, "com.instagram.android", "evening")
            )
        )

        assertThat(summaries.map { it.id }).containsExactly("evening")
    }

    @Test
    fun blocklistNamesForTarget_returnsMembershipChipsForRows() {
        val chips = BlocklistUiModel.blocklistNamesForTarget(
            targetKind = RuleKind.APP_PACKAGE,
            targetValue = "com.instagram.android",
            profiles = listOf(profile("social", "Social"), profile("study", "Study")),
            rules = listOf(
                rule("r1", RuleKind.APP_PACKAGE, "com.instagram.android", "social"),
                rule("r2", RuleKind.APP_PACKAGE, "com.instagram.android", "study"),
                rule("r3", RuleKind.DOMAIN, "instagram.com", "social")
            )
        )

        assertThat(chips).containsExactly("Social", "Study").inOrder()
    }

    @Test
    fun blocklistNamesForTarget_ignoresDefaultAdHocProfile() {
        val chips = BlocklistUiModel.blocklistNamesForTarget(
            targetKind = RuleKind.APP_PACKAGE,
            targetValue = "com.instagram.android",
            profiles = listOf(Profile.Default, profile("social", "Social")),
            rules = listOf(
                rule("ad-hoc", RuleKind.APP_PACKAGE, "com.instagram.android", DEFAULT_PROFILE_ID),
                rule("saved", RuleKind.APP_PACKAGE, "com.instagram.android", "social")
            )
        )

        assertThat(chips).containsExactly("Social")
    }

    @Test
    fun adHocBlockRules_returnsOnlyDefaultProfileRules() {
        val adHocRules = BlocklistUiModel.adHocBlockRules(
            listOf(
                rule("ad-hoc-app", RuleKind.APP_PACKAGE, "com.instagram.android", DEFAULT_PROFILE_ID),
                rule("saved-app", RuleKind.APP_PACKAGE, "com.zhiliaoapp.musically", "evening"),
                rule("ad-hoc-site", RuleKind.DOMAIN, "youtube.com", null),
                rule("allow", RuleKind.DOMAIN, "work.com", DEFAULT_PROFILE_ID, action = RuleAction.ALLOW)
            )
        )

        assertThat(adHocRules.map { it.id }).containsExactly("ad-hoc-app", "ad-hoc-site").inOrder()
    }

    @Test
    fun saveableAdHocTargets_includesOnlyEnabledRows() {
        val saveableTargets = BlocklistUiModel.saveableAdHocTargets(
            listOf(
                blockedTarget(TargetType.APP_PACKAGE, "com.instagram.android", enabled = true),
                blockedTarget(TargetType.APP_PACKAGE, "com.android.chrome", enabled = false),
                blockedTarget(TargetType.DOMAIN, "youtube.com", enabled = true)
            )
        )

        assertThat(saveableTargets.map { it.value })
            .containsExactly("com.instagram.android", "youtube.com")
            .inOrder()
    }

    @Test
    fun activeBlockCounts_includesDefaultAndEnabledBlocklistsOnly() {
        val counts = BlocklistUiModel.activeBlockCounts(
            profiles = listOf(
                Profile.Default,
                profile("reading", "Reading", enabled = true),
                profile("social", "Social", enabled = false)
            ),
            rules = listOf(
                rule("ad-hoc-app", RuleKind.APP_PACKAGE, "com.android.chrome", DEFAULT_PROFILE_ID),
                rule("ad-hoc-site", RuleKind.DOMAIN, "news.com", DEFAULT_PROFILE_ID),
                rule("reading-site-1", RuleKind.DOMAIN, "nytimes.com", "reading"),
                rule("reading-site-2", RuleKind.DOMAIN, "bbc.com", "reading"),
                rule("reading-keyword", RuleKind.KEYWORD, "headlines", "reading"),
                rule("disabled-profile-app", RuleKind.APP_PACKAGE, "com.instagram.android", "social"),
                rule("disabled-rule", RuleKind.DOMAIN, "disabled.com", "reading", enabled = false)
            )
        )

        assertThat(counts.appCount).isEqualTo(1)
        assertThat(counts.websiteCount).isEqualTo(3)
        assertThat(counts.keywordCount).isEqualTo(1)
    }

    private fun profile(id: String, name: String, enabled: Boolean = true): Profile {
        return Profile(
            id = id,
            name = name,
            colorToken = "blue",
            iconName = "shield",
            mode = ProfileMode.BLOCKLIST,
            enabled = enabled
        )
    }

    private fun rule(
        id: String,
        kind: RuleKind,
        value: String,
        profileId: String?,
        action: RuleAction = RuleAction.BLOCK,
        enabled: Boolean = true
    ): Rule {
        return Rule(
            id = id,
            kind = kind,
            action = action,
            operator = if (kind == RuleKind.DOMAIN) MatchOperator.SUBDOMAIN else MatchOperator.EXACT,
            value = value,
            profileId = profileId,
            enabled = enabled,
            createdAt = 1L,
            updatedAt = 1L
        )
    }

    private fun blockedTarget(
        targetType: TargetType,
        targetValue: String,
        enabled: Boolean
    ): BlockedTargetSettingsItem {
        return BlockedTargetSettingsItem(
            enabled = enabled,
            targetType = targetType,
            targetValue = targetValue,
            title = targetValue,
            subtitle = "",
            schedule = null,
            usageLimit = null
        )
    }
}
