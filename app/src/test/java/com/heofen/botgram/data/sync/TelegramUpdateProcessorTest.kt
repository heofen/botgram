package com.heofen.botgram.data.sync

import com.heofen.botgram.ChatType
import com.heofen.botgram.MessageType
import com.heofen.botgram.data.remote.TelegramChat
import com.heofen.botgram.data.remote.TelegramIncomingMessage
import com.heofen.botgram.data.remote.TelegramUpdate
import com.heofen.botgram.data.remote.TelegramUser
import com.heofen.botgram.database.tables.Chat
import com.heofen.botgram.database.tables.Message
import com.heofen.botgram.database.tables.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Тесты процессора входящих обновлений Telegram. */
class TelegramUpdateProcessorTest {
    @Test
    fun process_unsupportedUpdate_isNoOp() {
        val chatStore = FakeChatStore()
        val messageStore = FakeMessageStore()
        val userStore = FakeUserStore()
        val processor = TelegramUpdateProcessor(chatStore, messageStore, userStore)

        val result = processor.process(TelegramUpdate.Unsupported(updateId = 1L))

        assertNull(result)
        assertEquals(0, chatStore.upsertedChats.size)
        assertEquals(0, messageStore.messages.size)
        assertEquals(0, userStore.upsertedUsers.size)
    }

    @Test
    fun process_editedOldMessage_keepsPreviewBoundToActualLatestMessage() {
        val chatStore = FakeChatStore()
        val messageStore = FakeMessageStore().apply {
            messages[20L to 2L] = dbMessage(
                messageId = 2L,
                timestamp = 2_000L,
                text = "latest"
            )
        }
        val userStore = FakeUserStore()
        val processor = TelegramUpdateProcessor(chatStore, messageStore, userStore)

        val result = processor.process(
            TelegramUpdate.EditedMessage(
                updateId = 5L,
                message = incomingMessage(
                    messageId = 1L,
                    timestamp = 1_000L,
                    text = "edited old"
                )
            )
        )

        assertNotNull(result)
        assertEquals("latest", chatStore.lastPreviewText)
        assertEquals(2_000L, chatStore.lastPreviewTime)
        assertEquals("edited old", messageStore.messages[20L to 1L]?.text)
    }

    private fun incomingMessage(
        messageId: Long,
        timestamp: Long,
        text: String
    ): TelegramIncomingMessage {
        return TelegramIncomingMessage(
            messageId = messageId,
            chatId = 20L,
            topicId = null,
            senderId = 30L,
            type = MessageType.TEXT,
            timestamp = timestamp,
            text = text,
            caption = null,
            replyMsgId = null,
            replyMsgTopicId = null,
            fileName = null,
            fileExtension = null,
            fileId = null,
            fileUniqueId = null,
            fileSize = null,
            width = null,
            height = null,
            duration = null,
            thumbnailFileId = null,
            latitude = null,
            longitude = null,
            isEdited = true,
            editedAt = timestamp + 500L,
            mediaGroupId = null,
            chat = TelegramChat(
                id = 20L,
                type = ChatType.PRIVATE,
                title = null,
                firstName = "Bot",
                lastName = null,
                username = null,
                lastMessageType = MessageType.TEXT,
                lastMessageText = text,
                lastMessageTime = timestamp,
                lastMessageSenderId = 30L
            ),
            sender = TelegramUser(
                id = 30L,
                firstName = "Tester",
                lastName = null,
                username = "tester",
                languageCode = "ru",
                bio = null,
                canWriteMsgToPm = true
            )
        )
    }

    private fun dbMessage(
        messageId: Long,
        timestamp: Long,
        text: String
    ): Message {
        return Message(
            messageId = messageId,
            chatId = 20L,
            topicId = null,
            senderId = 30L,
            type = MessageType.TEXT,
            timestamp = timestamp,
            text = text,
            caption = null,
            replyMsgId = null,
            replyMsgTopicId = null,
            fileName = null,
            fileExtension = null,
            fileId = null,
            fileUniqueId = null,
            fileLocalPath = null,
            fileSize = null,
            width = null,
            height = null,
            duration = null,
            thumbnailFileId = null,
            latitude = null,
            longitude = null,
            isEdited = false,
            editedAt = null,
            mediaGroupId = null,
            readStatus = false,
            isOutgoing = false
        )
    }
}

/** Фейковое хранилище чатов для unit-тестов. */
private class FakeChatStore : ChatSyncStore {
    val upsertedChats = mutableListOf<Chat>()
    var lastPreviewText: String? = null
    var lastPreviewTime: Long? = null

    override suspend fun upsertChat(chat: Chat) {
        upsertedChats += chat
    }

    override suspend fun updateLastMessage(
        chatId: Long,
        type: MessageType?,
        text: String?,
        time: Long?,
        senderId: Long?
    ) {
        lastPreviewText = text
        lastPreviewTime = time
    }
}

/** Фейковое хранилище сообщений для unit-тестов. */
private class FakeMessageStore : MessageSyncStore {
    val messages = linkedMapOf<Pair<Long, Long>, Message>()

    override suspend fun upsertRemoteMessage(
        message: TelegramIncomingMessage,
        isOutgoing: Boolean,
        readStatus: Boolean
    ): Message {
        val merged = Message(
            messageId = message.messageId,
            chatId = message.chatId,
            topicId = message.topicId,
            senderId = message.senderId,
            type = message.type,
            timestamp = message.timestamp,
            text = message.text,
            caption = message.caption,
            replyMsgId = message.replyMsgId,
            replyMsgTopicId = message.replyMsgTopicId,
            fileName = message.fileName,
            fileExtension = message.fileExtension,
            fileId = message.fileId,
            fileUniqueId = message.fileUniqueId,
            fileLocalPath = null,
            fileSize = message.fileSize,
            width = message.width,
            height = message.height,
            duration = message.duration,
            thumbnailFileId = message.thumbnailFileId,
            latitude = message.latitude,
            longitude = message.longitude,
            isEdited = message.isEdited,
            editedAt = message.editedAt,
            mediaGroupId = message.mediaGroupId,
            readStatus = readStatus,
            isOutgoing = isOutgoing
        )
        messages[message.chatId to message.messageId] = merged
        return merged
    }

    override suspend fun getLastMessage(chatId: Long): Message? {
        return messages.values
            .filter { it.chatId == chatId }
            .maxWithOrNull(compareBy<Message> { it.timestamp }.thenBy { it.messageId })
    }
}

/** Фейковое хранилище пользователей для unit-тестов. */
private class FakeUserStore : UserSyncStore {
    val upsertedUsers = mutableListOf<User>()

    override suspend fun upsertUser(user: User) {
        upsertedUsers += user
    }
}
