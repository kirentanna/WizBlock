package com.wizblock.data.repository

import com.wizblock.model.MatchOperator
import com.wizblock.model.Rule
import com.wizblock.model.RuleAction
import com.wizblock.model.RuleKind
import kotlinx.coroutines.flow.Flow

sealed interface AddRuleResult {
    data class Success(val rule: Rule) : AddRuleResult
    data object Duplicate : AddRuleResult
    data object Invalid : AddRuleResult
}

interface RuleRepository {
    fun observeAll(): Flow<List<Rule>>
    fun observeEnabled(): Flow<List<Rule>>
    suspend fun add(
        kind: RuleKind,
        action: RuleAction,
        operator: MatchOperator,
        rawValue: String,
        profileId: String = com.wizblock.model.DEFAULT_PROFILE_ID
    ): AddRuleResult

    suspend fun setEnabled(id: String, enabled: Boolean)
    suspend fun delete(id: String)
    suspend fun deleteByProfileId(profileId: String)
}
