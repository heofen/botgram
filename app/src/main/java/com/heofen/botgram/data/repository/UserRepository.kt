package com.heofen.botgram.data.repository

import com.heofen.botgram.data.MediaManager
import com.heofen.botgram.data.remote.AvatarFetchResult
import com.heofen.botgram.data.remote.PublicProfileBioResult
import com.heofen.botgram.data.remote.TelegramGateway
import com.heofen.botgram.data.sync.UserSyncStore
import com.heofen.botgram.database.dao.UserDao
import com.heofen.botgram.database.tables.User
import java.io.File
import kotlinx.coroutines.flow.Flow

/** Репозиторий пользователей и их аватаров. */
class UserRepository(
    private val userDao: UserDao,
    private val mediaManager: MediaManager,
    private val gateway: TelegramGateway
) : UserSyncStore {
    fun observeById(id: Long): Flow<User?> = userDao.observeById(id)

    suspend fun getById(id: Long): User? = userDao.getById(id)

    suspend fun userExists(id: Long): Boolean = userDao.userExists(id)

    suspend fun insertUser(user: User) = userDao.insert(user)

    override suspend fun upsertUser(user: User) = userDao.upsert(mergeIncomingUserWithStoredState(user, userDao.getById(user.id)))

    suspend fun updateAvatar(
        userId: Long,
        fileId: String?,
        fileUniqueId: String?,
        localPath: String?
    ) = userDao.updateAvatar(userId, fileId, fileUniqueId, localPath)

    suspend fun findCachedAvatar(fileUniqueId: String): User? =
        userDao.findByAvatarUniqueId(fileUniqueId)

    /** Докачивает аватар пользователя, если локального файла ещё нет. */
    suspend fun loadAvatarIfMissing(userId: Long) {
        val user = userDao.getById(userId) ?: return

        if (user.avatarLocalPath != null) {
            val file = File(user.avatarLocalPath)
            if (file.exists()) return
        }

        refreshAvatar(userId)
    }

    /** Синхронизирует актуальный аватар пользователя с Telegram. */
    override suspend fun refreshAvatar(userId: Long): AvatarFetchResult? {
        val current = userDao.getById(userId) ?: return null
        val avatar = mediaManager.downloadUserAvatar(userId) ?: return null
        return applyFetchedAvatar(userId = userId, current = current, avatar = avatar)
    }

    /** Подтягивает публичное описание пользователя со страницы `t.me/<username>`. */
    suspend fun refreshBio(userId: Long, username: String?) {
        if (!userDao.userExists(userId)) return

        val normalizedUsername = username
            ?.removePrefix("@")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: userDao.getById(userId)?.username?.takeIf { it.isNotBlank() }
            ?: return

        when (val result = gateway.fetchUserBio(normalizedUsername)) {
            is PublicProfileBioResult.Success -> userDao.updateBio(userId, result.bio)
            PublicProfileBioResult.Failure -> Unit
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
}

/** Сливает свежий профиль пользователя с локально ценными полями. */
internal fun mergeIncomingUserWithStoredState(
    incoming: User,
    current: User?
): User {
    if (current == null) return incoming

    return incoming.copy(
        languageCode = incoming.languageCode ?: current.languageCode,
        bio = incoming.bio ?: current.bio,
        avatarFileId = incoming.avatarFileId ?: current.avatarFileId,
        avatarFileUniqueId = incoming.avatarFileUniqueId ?: current.avatarFileUniqueId,
        avatarLocalPath = incoming.avatarLocalPath ?: current.avatarLocalPath
    )
}
