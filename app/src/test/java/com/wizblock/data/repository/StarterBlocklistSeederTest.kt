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

class StarterBlocklistSeederTest {
    @Test
    fun ensureSeeded_createsEditableStarterBlocklistsOnce() = runTest {
        var seeded = false
        val profiles = FakeProfileRepository()
        val rules = FakeRuleRepository()
        val seeder = StarterBlocklistSeeder(
            profileRepository = profiles,
            ruleRepository = rules,
            isSeeded = { seeded },
            markSeeded = { seeded = true }
        )

        seeder.ensureSeeded()
        seeder.ensureSeeded()

        assertThat(seeded).isTrue()
        assertThat(profiles.upserted.map { it.id }).containsExactly("games", "shopping", "social", "video").inOrder()
        assertThat(profiles.upserted.map { it.enabled }.distinct()).containsExactly(false)
        rules.added
            .groupBy { it.profileId to it.kind }
            .values
            .forEach { starterRulesForKind ->
                assertThat(starterRulesForKind.size).isAtMost(3)
            }
        assertThat(rules.added.filter { it.profileId == "social" }.map { it.rawValue })
            .containsAtLeast("instagram.com", "com.instagram.android")
        assertThat(rules.added.map { it.profileId }.toSet()).doesNotContain("default")
        assertThat(rules.added.size).isEqualTo(rules.added.distinctBy { Triple(it.kind, it.rawValue, it.profileId) }.size)
    }

    @Test
    fun ensureSeeded_doesNotRecreateUserEditedStarterListsAfterFlagIsSet() = runTest {
        val profiles = FakeProfileRepository()
        val rules = FakeRuleRepository()
        val seeder = StarterBlocklistSeeder(
            profileRepository = profiles,
            ruleRepository = rules,
            isSeeded = { true },
            markSeeded = { error("Already seeded runs must not mark again") }
        )

        seeder.ensureSeeded()

        assertThat(profiles.upserted).isEmpty()
        assertThat(rules.added).isEmpty()
    }

    private class FakeProfileRepository : ProfileRepository {
        val upserted = mutableListOf<Profile>()
        private val profiles = MutableStateFlow<List<Profile>>(emptyList())

        override fun observeAll(): Flow<List<Profile>> = profiles
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
            val action: RuleAction,
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
            added += AddedRule(kind, action, operator, rawValue, profileId)
            return AddRuleResult.Success(
                Rule(
                    id = "${profileId}_${rawValue}",
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
