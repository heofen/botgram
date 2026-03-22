package com.heofen.botgram.data.repository

import com.heofen.botgram.database.tables.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Тесты слияния входящего профиля пользователя с локальным состоянием. */
class UserRepositoryTest {
    @Test
    fun mergeIncomingUserWithStoredState_allowsUsernameRemoval() {
        val current = baseUser(
            username = "old_name",
            languageCode = "ru",
            avatarFileId = "old-file",
            avatarFileUniqueId = "old-unique",
            avatarLocalPath = "/tmp/old.jpg"
        )
        val incoming = baseUser(
            username = null,
            languageCode = null
        )

        val merged = mergeIncomingUserWithStoredState(
            incoming = incoming,
            current = current
        )

        assertNull(merged.username)
        assertEquals("ru", merged.languageCode)
        assertEquals("old-file", merged.avatarFileId)
        assertEquals("old-unique", merged.avatarFileUniqueId)
        assertEquals("/tmp/old.jpg", merged.avatarLocalPath)
    }

    private fun baseUser(
        username: String?,
        languageCode: String?,
        avatarFileId: String? = null,
        avatarFileUniqueId: String? = null,
        avatarLocalPath: String? = null
    ): User {
        return User(
            id = 10L,
            firstName = "Tester",
            lastName = "User",
            username = username,
            languageCode = languageCode,
            bio = null,
            avatarFileId = avatarFileId,
            avatarFileUniqueId = avatarFileUniqueId,
            avatarLocalPath = avatarLocalPath,
            canWriteMsgToPm = true
        )
    }
}
