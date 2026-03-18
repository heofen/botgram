package com.heofen.botgram.data.sync

import com.heofen.botgram.MessageType
import com.heofen.botgram.data.remote.TelegramIncomingMessage
import com.heofen.botgram.data.remote.TelegramUpdate
import com.heofen.botgram.database.tables.Chat
import com.heofen.botgram.database.tables.Message
import com.heofen.botgram.database.tables.User
import com.heofen.botgram.utils.toDbChat
import com.heofen.botgram.utils.toDbUser

/** Контракт хранилища чатов для процессора синхронизации. */
interface ChatSyncStore {
    suspend fun upsertChat(chat: Chat)

    suspend fun updateLastMessage(
        chatId: Long,
        type: MessageType?,
        text: String?,
        time: Long?,
        senderId: Long?
    )
}

/** Контракт хранилища сообщений для процессора синхронизации. */
interface MessageSyncStore {
    suspend fun upsertRemoteMessage(
        message: TelegramIncomingMessage,
        isOutgoing: Boolean,
        readStatus: Boolean
    ): Message

    suspend fun getLastMessage(chatId: Long): Message?
}

/** Контракт хранилища пользователей для процессора синхронизации. */
interface UserSyncStore {
    suspend fun upsertUser(user: User)
}

/** Обрабатывает входящие обновления и раскладывает их по локальным таблицам. */
class TelegramUpdateProcessor(
    private val chatStore: ChatSyncStore,
    private val messageStore: MessageSyncStore,
    private val userStore: UserSyncStore
) {
    /** Обрабатывает одно обновление и возвращает сохранённое сообщение, если оно есть. */
    suspend fun process(update: TelegramUpdate): Message? {
        return when (update) {
            is TelegramUpdate.NewMessage -> processMessage(update.message)
            is TelegramUpdate.EditedMessage -> processMessage(update.message)
            is TelegramUpdate.Unsupported -> null
        }
    }

    /** Сохраняет сообщение и связанные с ним сущности. */
    private suspend fun processMessage(message: TelegramIncomingMessage): Message {
        chatStore.upsertChat(message.chat.toDbChat())
        val sender = message.sender?.toDbUser()
        if (sender != null) {
            userStore.upsertUser(sender)
        }

        val storedMessage = messageStore.upsertRemoteMessage(
            message = message,
            isOutgoing = false,
            readStatus = false
        )

        refreshLastMessage(chatId = storedMessage.chatId)
        return storedMessage
    }

    /** Обновляет поля последнего сообщения у чата после синхронизации. */
    private suspend fun refreshLastMessage(chatId: Long) {
        val lastMessage = messageStore.getLastMessage(chatId)
        chatStore.updateLastMessage(
            chatId = chatId,
            type = lastMessage?.type,
            text = lastMessage?.text ?: lastMessage?.caption,
            time = lastMessage?.timestamp,
            senderId = lastMessage?.senderId
        )
    }
}
