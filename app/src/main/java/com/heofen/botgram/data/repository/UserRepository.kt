package com.heofen.botgram.data.repository

import com.heofen.botgram.data.MediaManager
import com.heofen.botgram.data.remote.AvatarFetchResult
import com.heofen.botgram.data.remote.PublicProfileBioResult
import com.heofen.botgram.data.remote.TelegramGateway
import com.heofen.botgram.data.sync.UserSyncStore
import com.heofen.botgram.database.dao.UserDao
import com.heofen.botgram.database.tables.User
import kotlinx.coroutines.flow.Flow
import java.io.File

/** Репозиторий пользователей. */
class UserRepository(
    private val userDao: UserDao,
    private val mediaManager: MediaManager,
    private val gateway: TelegramGateway
) : UserSyncStore {
    /** Наблюдает за изменениями конкретного пользователя. */
    fun observeById(id: Long): Flow<User?> = userDao.observeById(id)

    suspend fun getById(id: Long): User? = userDao.getById(id)

    suspend fun userExists(id: Long): Boolean = userDao.userExists(id)

    suspend fun insertUser(user: User) = userDao.insert(user)

    override suspend fun upsertUser(user: User) = userDao.upsert(user.mergeStoredState())

    /** Докачивает аватар пользователя, если он ещё не сохранён локально. */
    suspend fun loadAvatarIfMissing(userId: Long) {
        val user = userDao.getById(userId) ?: return

        if (user.avatarLocalPath != null) {
            if (File(user.avatarLocalPath).exists()) return
        }

        refreshAvatar(userId)
    }

    /** Синхронизирует актуальный аватар пользователя с Telegram. */
    override suspend fun refreshAvatar(userId: Long): AvatarFetchResult? {
        val current = userDao.getById(userId) ?: return null
        val avatar = mediaManager.downloadUserAvatar(userId) ?: return null
        return applyFetchedAvatar(userId = userId, current = current, avatar = avatar)
    }

    /** Синхронизирует актуальное био пользователя. */
    suspend fun refreshBio(userId: Long, username: String) {
        val result = gateway.fetchUserBio(username)
        if (result is PublicProfileBioResult.Success) {
            userDao.updateBio(userId, result.bio)
        }
    }

    private suspend fun applyFetchedAvatar(
        userId: Long,
        current: User,
        avatar: AvatarFetchResult
    ): AvatarFetchResult {
        return when (avatar) {
            is AvatarFetchResult.Available -> {
                val localPath = avatar.localPath
                    ?: current.avatarLocalPath?.takeIf { current.avatarFileUniqueId == avatar.fileUniqueId }
                userDao.updateAvatar(userId, avatar.fileId, avatar.fileUniqueId, localPath)
                AvatarFetchResult.Available(
                    fileId = avatar.fileId,
                    fileUniqueId = avatar.fileUniqueId,
                    localPath = localPath
                )
            }

            AvatarFetchResult.Missing -> {
                userDao.updateAvatar(userId, null, null, null)
                AvatarFetchResult.Missing
            }
        }
    }

    /** Сохраняет локально ценные поля, если сервер прислал неполную версию пользователя. */
    private suspend fun User.mergeStoredState(): User {
        val current = userDao.getById(id) ?: return this
        return copy(
            avatarFileId = avatarFileId ?: current.avatarFileId,
            avatarFileUniqueId = avatarFileUniqueId ?: current.avatarFileUniqueId,
            avatarLocalPath = avatarLocalPath ?: current.avatarLocalPath,
            bio = bio ?: current.bio
        )
    }
}
