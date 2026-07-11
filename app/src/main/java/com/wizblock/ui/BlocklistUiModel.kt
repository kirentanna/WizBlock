package com.wizblock.ui

import com.wizblock.data.repository.BlocklistTargetDraft
import com.wizblock.model.Profile
import com.wizblock.model.ProfileMode
import com.wizblock.model.Rule
import com.wizblock.model.RuleAction
import com.wizblock.model.RuleKind
import com.wizblock.model.DEFAULT_PROFILE_ID

data class BlocklistSummaryItem(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val appCount: Int,
    val websiteCount: Int,
    val keywordCount: Int
)

data class BlockRuleCounts(
    val appCount: Int,
    val websiteCount: Int,
    val keywordCount: Int
)

object BlocklistUiModel {
    fun summarizeBlocklists(
        profiles: List<Profile>,
        rules: List<Rule>
    ): List<BlocklistSummaryItem> {
        val blockRules = rules.filter { it.action == RuleAction.BLOCK }
        return profiles
            .filter { it.isSavedBlocklistProfile() }
            .sortedBy { it.name.lowercase() }
            .map { profile ->
                val profileRules = blockRules.filter { it.profileId == profile.id }
                BlocklistSummaryItem(
                    id = profile.id,
                    name = profile.name,
                    enabled = profile.enabled,
                    appCount = profileRules.count { it.kind == RuleKind.APP_PACKAGE },
                    websiteCount = profileRules.count { it.kind == RuleKind.DOMAIN },
                    keywordCount = profileRules.count { it.kind == RuleKind.KEYWORD }
                )
            }
    }

    fun blocklistNamesForTarget(
        targetKind: RuleKind,
        targetValue: String,
        profiles: List<Profile>,
        rules: List<Rule>
    ): List<String> {
        val profilesById = profiles
            .filter { it.isSavedBlocklistProfile() }
            .associateBy { it.id }
        return rules
            .filter {
                it.enabled &&
                    it.action == RuleAction.BLOCK &&
                    it.kind == targetKind &&
                    it.value.equals(targetValue, ignoreCase = true)
            }
            .mapNotNull { profilesById[it.profileId]?.name }
            .distinct()
            .sortedBy { it.lowercase() }
    }

    fun adHocBlockRules(rules: List<Rule>): List<Rule> {
        return rules.filter {
            it.action == RuleAction.BLOCK &&
                (it.profileId ?: DEFAULT_PROFILE_ID) == DEFAULT_PROFILE_ID
        }
    }

    fun saveableAdHocTargets(targets: List<BlockedTargetSettingsItem>): List<BlocklistTargetDraft> {
        return targets
            .filter { it.enabled }
            .map { BlocklistTargetDraft(it.targetType.toRuleKind(), it.targetValue) }
    }

    fun activeBlockCounts(
        profiles: List<Profile>,
        rules: List<Rule>
    ): BlockRuleCounts {
        val enabledProfileIds = profiles
            .filter { it.enabled }
            .map { it.id }
            .toSet() + DEFAULT_PROFILE_ID
        val activeRules = rules
            .filter {
                it.enabled &&
                    it.action == RuleAction.BLOCK &&
                    (it.profileId ?: DEFAULT_PROFILE_ID) in enabledProfileIds
            }
            .distinctBy { it.kind to it.value.lowercase() }
        return BlockRuleCounts(
            appCount = activeRules.count { it.kind == RuleKind.APP_PACKAGE },
            websiteCount = activeRules.count { it.kind == RuleKind.DOMAIN },
            keywordCount = activeRules.count { it.kind == RuleKind.KEYWORD }
        )
    }

    private fun Profile.isSavedBlocklistProfile(): Boolean {
        return mode == ProfileMode.BLOCKLIST && id != DEFAULT_PROFILE_ID
    }

    private fun com.wizblock.model.TargetType.toRuleKind(): RuleKind {
        return when (this) {
            com.wizblock.model.TargetType.APP_PACKAGE -> RuleKind.APP_PACKAGE
            com.wizblock.model.TargetType.DOMAIN -> RuleKind.DOMAIN
            com.wizblock.model.TargetType.KEYWORD -> RuleKind.KEYWORD
        }
    }
}
