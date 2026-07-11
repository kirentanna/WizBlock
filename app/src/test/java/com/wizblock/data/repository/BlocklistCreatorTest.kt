package com.wizblock.data.repository

import com.google.common.truth.Truth.assertThat
import com.wizblock.model.MatchOperator
import com.wizblock.model.Profile
import com.wizblock.model.Rule
import com.wizblock.model.RuleAction
import com.wizblock.model.RuleKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BlocklistCreatorTest {
    @Test
    fun createBlocklist_savesSelectedTargetsIntoNamedBlocklist() = runTest {
        val profiles = FakeProfileRepository()
        val rules = FakeRuleRepository()
        val creator = BlocklistCreator(
            profileRepository = profiles,
            ruleRepository = rules,
            idFactory = { "evening-scroll" }
        )

        val result = creator.create(
            name = " Evening Scroll ",
            targets = listOf(
                BlocklistTargetDraft(RuleKind.APP_PACKAGE, "com.instagram.android"),
                BlocklistTargetDraft(RuleKind.APP_PACKAGE, "com.zhiliaoapp.musically"),
                BlocklistTargetDraft(RuleKind.DOMAIN, "youtube.com")
            )
        )

        assertThat(result).isEqualTo(CreateBlocklistResult.Success("evening-scroll"))
        assertThat(profiles.upserted.single().name).isEqualTo("Evening Scroll")
        assertThat(rules.added.map { it.profileId }).containsExactly(
            "evening-scroll",
            "evening-scroll",
            "evening-scroll"
        )
        assertThat(rules.added.map { it.operator }).containsExactly(
            MatchOperator.EXACT,
            MatchOperator.EXACT,
            MatchOperator.SUBDOMAIN
        )
    }

    @Test
    fun createBlocklist_rejectsBlankNameOrEmptyTargets() = runTest {
        val creator = BlocklistCreator(
            profileRepository = FakeProfileRepository(),
            ruleRepository = FakeRuleRepository(),
            idFactory = { "unused" }
        )

        assertThat(creator.create("", listOf(BlocklistTargetDraft(RuleKind.KEYWORD, "sports"))))
            .isEqualTo(CreateBlocklistResult.InvalidName)
        assertThat(creator.create("Study", emptyList()))
            .isEqualTo(CreateBlocklistResult.EmptySelection)
    }

    private class FakeProfileRepository : ProfileRepository {
        val upserted = mutableListOf<Profile>()
        override fun observeAll(): Flow<List<Profile>> = MutableStateFlow(emptyList())
        override suspend fun ensureDefaultProfile() = Unit
        override suspend fun setEnabled(id: String, enabled: Boolean) = Unit
        override suspend fun upsert(profile: Profile) {
            upserted += profile
        }
        override suspend fun delete(id: String) = Unit
    }

    private class FakeRuleRepository : RuleRepository {
        data class AddedRule(
            val kind: RuleKind,
            val operator: MatchOperator,
            val rawValue: String,
            val profileId: String
        )

        val added = mutableListOf<AddedRule>()
        override fun observeAll(): Flow<List<Rule>> = MutableStateFlow(emptyList())
        override fun observeEnabled(): Flow<List<Rule>> = MutableStateFlow(emptyList())

        override suspend fun add(
            kind: RuleKind,
            action: RuleAction,
            operator: MatchOperator,
            rawValue: String,
            profileId: String
        ): AddRuleResult {
            added += AddedRule(kind, operator, rawValue, profileId)
            return AddRuleResult.Success(
                Rule(
                    id = rawValue,
                    kind = kind,
                    action = action,
                    operator = operator,
                    value = rawValue,
                    profileId = profileId,
                    enabled = true,
                    createdAt = 1L,
                    updatedAt = 1L
                )
            )
        }

        override suspend fun setEnabled(id: String, enabled: Boolean) = Unit
        override suspend fun delete(id: String) = Unit
        override suspend fun deleteByProfileId(profileId: String) = Unit
    }
}
