package com.wizblock.data.repository

import com.wizblock.data.local.room.RuleDao
import com.wizblock.data.local.room.RuleEntity
import com.wizblock.domain.TargetNormalizer
import com.wizblock.domain.toTargetType
import com.wizblock.model.MatchOperator
import com.wizblock.model.DEFAULT_PROFILE_ID
import com.wizblock.model.Rule
import com.wizblock.model.RuleAction
import com.wizblock.model.RuleKind
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RuleRepositoryImpl(
    private val dao: RuleDao,
    private val targetNormalizer: TargetNormalizer
) : RuleRepository {
    override fun observeAll(): Flow<List<Rule>> = dao.observeAll().map { list ->
        list.map { it.toModel() }
    }

    override fun observeEnabled(): Flow<List<Rule>> = dao.observeEnabled().map { list ->
        list.map { it.toModel() }
    }

    override suspend fun add(
        kind: RuleKind,
        action: RuleAction,
        operator: MatchOperator,
        rawValue: String,
        profileId: String
    ): AddRuleResult {
        val normalizedValue = targetNormalizer.normalize(kind, rawValue)?.targetValue ?: return AddRuleResult.Invalid
        val adjustedOperator = when (kind) {
            RuleKind.DOMAIN -> operator
            RuleKind.APP_PACKAGE -> MatchOperator.EXACT
            RuleKind.KEYWORD -> MatchOperator.CONTAINS
        }

        val existing = dao.findByIdentity(
            kind = kind.name,
            action = action.name,
            operator = adjustedOperator.name,
            value = normalizedValue,
            profileId = profileId.ifBlank { DEFAULT_PROFILE_ID }
        )
        if (existing != null) return AddRuleResult.Duplicate

        val now = System.currentTimeMillis()
        val entity = RuleEntity(
            id = UUID.randomUUID().toString(),
            kind = kind.name,
            action = action.name,
            operator = adjustedOperator.name,
            value = normalizedValue,
            profileId = profileId.ifBlank { DEFAULT_PROFILE_ID },
            enabled = true,
            createdAt = now,
            updatedAt = now
        )
        dao.insert(entity)
        return AddRuleResult.Success(entity.toModel())
    }

    override suspend fun setEnabled(id: String, enabled: Boolean) {
        dao.setEnabled(id, enabled, System.currentTimeMillis())
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    override suspend fun deleteByProfileId(profileId: String) {
        dao.deleteByProfileId(profileId.ifBlank { DEFAULT_PROFILE_ID })
    }

}

private fun RuleEntity.toModel(): Rule {
    return Rule(
        id = id,
        kind = RuleKind.valueOf(kind),
        action = RuleAction.valueOf(action),
        operator = MatchOperator.valueOf(operator),
        value = value,
        profileId = profileId ?: DEFAULT_PROFILE_ID,
        enabled = enabled,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
