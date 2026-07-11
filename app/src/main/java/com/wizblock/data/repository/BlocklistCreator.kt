package com.wizblock.data.repository

import com.wizblock.model.MatchOperator
import com.wizblock.model.Profile
import com.wizblock.model.ProfileMode
import com.wizblock.model.RuleAction
import com.wizblock.model.RuleKind
import java.util.UUID

data class BlocklistTargetDraft(
    val kind: RuleKind,
    val value: String
)

sealed interface CreateBlocklistResult {
    data class Success(val id: String) : CreateBlocklistResult
    data object InvalidName : CreateBlocklistResult
    data object EmptySelection : CreateBlocklistResult
}

class BlocklistCreator(
    private val profileRepository: ProfileRepository,
    private val ruleRepository: RuleRepository,
    private val idFactory: () -> String = { UUID.randomUUID().toString() }
) {
    suspend fun create(name: String, targets: List<BlocklistTargetDraft>): CreateBlocklistResult {
        val safeName = name.trim()
        if (safeName.isBlank()) return CreateBlocklistResult.InvalidName
        if (targets.isEmpty()) return CreateBlocklistResult.EmptySelection

        val id = idFactory()
        profileRepository.upsert(
            Profile(
                id = id,
                name = safeName,
                colorToken = "blue",
                iconName = "shield",
                mode = ProfileMode.BLOCKLIST,
                enabled = true
            )
        )
        targets.distinctBy { it.kind to it.value.lowercase() }.forEach { target ->
            ruleRepository.add(
                kind = target.kind,
                action = RuleAction.BLOCK,
                operator = target.kind.defaultOperator(),
                rawValue = target.value,
                profileId = id
            )
        }
        return CreateBlocklistResult.Success(id)
    }

    private fun RuleKind.defaultOperator(): MatchOperator {
        return when (this) {
            RuleKind.APP_PACKAGE -> MatchOperator.EXACT
            RuleKind.DOMAIN -> MatchOperator.SUBDOMAIN
            RuleKind.KEYWORD -> MatchOperator.CONTAINS
        }
    }
}
