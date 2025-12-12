package com.heofen.botgram.data.repository

import com.heofen.botgram.database.dao.UserDao
import com.heofen.botgram.database.tables.User
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {
    suspend fun getById(id: Long): User? = userDao.getById(id)

    suspend fun userExists(id: Long): Boolean = userDao.userExists(id)

    suspend fun insertUser(user: User) = userDao.insert(user)

    suspend fun upsertUser(user: User) = userDao.upsert(user)

    suspend fun updateAvatar(
        userId: Long,
        fileId: String?,
        fileUniqueId: String?,
        localPath: String?
    ) = userDao.updateAvatar(userId, fileId, fileUniqueId, localPath)

    suspend fun findCachedAvatar(fileUniqueId: String): User? =
        userDao.findByAvatarUniqueId(fileUniqueId)
}
