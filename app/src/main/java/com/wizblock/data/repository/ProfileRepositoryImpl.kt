package com.wizblock.data.repository

import com.wizblock.data.local.room.ProfileDao
import com.wizblock.data.local.room.ProfileEntity
import com.wizblock.model.Profile
import com.wizblock.model.ProfileMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepositoryImpl(
    private val dao: ProfileDao
) : ProfileRepository {
    override fun observeAll(): Flow<List<Profile>> {
        return dao.observeAll().map { entities ->
            entities.map { it.toModel() }.ifEmpty { listOf(Profile.Default) }
        }
    }

    override suspend fun ensureDefaultProfile() {
        dao.upsert(Profile.Default.toEntity())
    }

    override suspend fun setEnabled(id: String, enabled: Boolean) {
        dao.setEnabled(id, enabled)
    }

    override suspend fun upsert(profile: Profile) {
        dao.upsert(profile.toEntity())
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }
}

private fun ProfileEntity.toModel(): Profile {
    return Profile(
        id = id,
        name = name,
        colorToken = colorToken,
        iconName = iconName,
        mode = runCatching { ProfileMode.valueOf(mode) }.getOrDefault(ProfileMode.BLOCKLIST),
        enabled = enabled
    )
}

private fun Profile.toEntity(): ProfileEntity {
    return ProfileEntity(
        id = id,
        name = name,
        colorToken = colorToken,
        iconName = iconName,
        mode = mode.name,
        enabled = enabled
    )
}
