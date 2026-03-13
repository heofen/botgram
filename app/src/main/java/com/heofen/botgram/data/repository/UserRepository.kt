package com.heofen.botgram.data.repository

import com.heofen.botgram.data.MediaManager
import com.heofen.botgram.data.sync.UserSyncStore
import com.heofen.botgram.database.dao.UserDao
import com.heofen.botgram.database.tables.User
import java.io.File

class UserRepository(
    private val userDao: UserDao,
    private val mediaManager: MediaManager
) : UserSyncStore {
    suspend fun getById(id: Long): User? = userDao.getById(id)

    suspend fun userExists(id: Long): Boolean = userDao.userExists(id)

    suspend fun insertUser(user: User) = userDao.insert(user)

    override suspend fun upsertUser(user: User) = userDao.upsert(user.mergeStoredAvatar())

    suspend fun updateAvatar(
        userId: Long,
        fileId: String?,
        fileUniqueId: String?,
        localPath: String?
    ) = userDao.updateAvatar(userId, fileId, fileUniqueId, localPath)

    suspend fun findCachedAvatar(fileUniqueId: String): User? =
        userDao.findByAvatarUniqueId(fileUniqueId)

    suspend fun loadAvatarIfMissing(userId: Long) {
        val user = userDao.getById(userId) ?: return

        if (user.avatarLocalPath != null) {
            val file = File(user.avatarLocalPath)
            if (file.exists()) return
        }

        val avatar = mediaManager.downloadUserAvatar(userId) ?: return

        if (avatar.localPath != null) {
            userDao.updateAvatar(userId, avatar.fileId, avatar.fileUniqueId, avatar.localPath)
        }
    }

    private suspend fun User.mergeStoredAvatar(): User {
        val current = userDao.getById(id) ?: return this
        return copy(
            avatarFileId = avatarFileId ?: current.avatarFileId,
            avatarFileUniqueId = avatarFileUniqueId ?: current.avatarFileUniqueId,
            avatarLocalPath = avatarLocalPath ?: current.avatarLocalPath
        )
    }
}
