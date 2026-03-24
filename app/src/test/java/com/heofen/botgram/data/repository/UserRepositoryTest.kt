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
            bio = "Stored bio",
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
        assertEquals("Stored bio", merged.bio)
        assertEquals("old-file", merged.avatarFileId)
        assertEquals("old-unique", merged.avatarFileUniqueId)
        assertEquals("/tmp/old.jpg", merged.avatarLocalPath)
    }

    @Test
    fun mergeIncomingUserWithStoredState_usesNewBioWhenPresent() {
        val current = baseUser(
            username = "tester",
            languageCode = "ru",
            bio = "Old bio"
        )
        val incoming = baseUser(
            username = "tester",
            languageCode = null,
            bio = "New bio"
        )

        val merged = mergeIncomingUserWithStoredState(
            incoming = incoming,
            current = current
        )

        assertEquals("New bio", merged.bio)
    }

    private fun baseUser(
        username: String?,
        languageCode: String?,
        bio: String? = null,
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
            bio = bio,
            avatarFileId = avatarFileId,
            avatarFileUniqueId = avatarFileUniqueId,
            avatarLocalPath = avatarLocalPath,
            canWriteMsgToPm = true
        )
    }
}
