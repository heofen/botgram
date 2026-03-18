package com.heofen.botgram.data.repository

import com.heofen.botgram.MessageType
import com.heofen.botgram.database.tables.Message
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Тесты объединения удалённого сообщения с локальным состоянием. */
class MessageRepositoryTest {
    @Test
    fun mergeRemoteMessageWithLocalState_preservesLocalFlagsAndFilePathForSameMedia() {
        val existing = baseMessage(
            fileId = "old-file",
            fileUniqueId = "shared-media",
            fileLocalPath = "/tmp/media.jpg",
            readStatus = true,
            isOutgoing = true
        )
        val incoming = baseMessage(
            text = "edited",
            fileId = "new-file",
            fileUniqueId = "shared-media",
            fileLocalPath = null,
            isEdited = true,
            editedAt = 2_000L,
            readStatus = false,
            isOutgoing = false
        )

        val merged = mergeRemoteMessageWithLocalState(
            incoming = incoming,
            existing = existing
        )

        assertEquals("/tmp/media.jpg", merged.fileLocalPath)
        assertEquals("new-file", merged.fileId)
        assertEquals(true, merged.readStatus)
        assertEquals(true, merged.isOutgoing)
        assertEquals("edited", merged.text)
        assertEquals(2_000L, merged.editedAt)
    }

    @Test
    fun mergeRemoteMessageWithLocalState_dropsLocalFilePathWhenMediaChanges() {
        val existing = baseMessage(
            fileUniqueId = "old-media",
            fileLocalPath = "/tmp/old.jpg"
        )
        val incoming = baseMessage(
            fileUniqueId = "new-media",
            fileLocalPath = null
        )

        val merged = mergeRemoteMessageWithLocalState(
            incoming = incoming,
            existing = existing
        )

        assertNull(merged.fileLocalPath)
    }

    /** Создаёт минимальную тестовую сущность сообщения. */
    private fun baseMessage(
        text: String? = "hello",
        fileId: String? = null,
        fileUniqueId: String? = null,
        fileLocalPath: String? = null,
        isEdited: Boolean = false,
        editedAt: Long? = null,
        readStatus: Boolean = false,
        isOutgoing: Boolean = false
    ): Message {
        return Message(
            messageId = 10L,
            chatId = 20L,
            topicId = null,
            senderId = 30L,
            type = MessageType.TEXT,
            timestamp = 1_000L,
            text = text,
            caption = null,
            replyMsgId = null,
            replyMsgTopicId = null,
            fileName = null,
            fileExtension = "jpg",
            fileId = fileId,
            fileUniqueId = fileUniqueId,
            fileLocalPath = fileLocalPath,
            fileSize = null,
            width = null,
            height = null,
            duration = null,
            thumbnailFileId = null,
            latitude = null,
            longitude = null,
            isEdited = isEdited,
            editedAt = editedAt,
            mediaGroupId = null,
            readStatus = readStatus,
            isOutgoing = isOutgoing
        )
    }
}
