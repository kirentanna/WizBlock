package com.wizblock.data.repository

import com.wizblock.policy.PolicySnapshot
import kotlinx.coroutines.flow.Flow

interface PolicyRepository {
    val snapshot: Flow<PolicySnapshot>
}
