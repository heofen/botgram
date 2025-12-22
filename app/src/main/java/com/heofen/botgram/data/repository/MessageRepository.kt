package com.heofen.botgram.data.repository

import android.util.Log
import com.heofen.botgram.MessageType
import com.heofen.botgram.database.dao.MessageDao
import com.heofen.botgram.database.tables.Message
import com.heofen.botgram.utils.toDbMessage
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.PhotoContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.toChatId
import kotlinx.coroutines.flow.Flow

class MessageRepository(
    private val messageDao: MessageDao,
    private val bot: TelegramBot
) {
    fun getChatMessages(chatId: Long): Flow<List<Message>> =
        messageDao.getChatMessages(chatId)

    fun getMediaGroup(groupId: String): Flow<List<Message>> =
        messageDao.getMediaGroup(groupId)

    suspend fun getMessage(chatId: Long, messageId: Long): Message? =
        messageDao.getMessage(chatId, messageId)

    suspend fun insertMessage(message: Message) =
        messageDao.insert(message)

    suspend fun insertAll(messages: List<Message>) =
        messageDao.insertAll(messages)

    suspend fun updateMessage(
        chatId: Long,
        messageId: Long,
        text: String?,
        caption: String?,
        isEdited: Boolean,
        editedAt: Long?
    ) = messageDao.updateMessage(chatId, messageId, text, caption, isEdited, editedAt)

    suspend fun updateFilePath(chatId: Long, messageId: Long, localPath: String) =
        messageDao.updateFilePath(chatId, messageId, localPath)

    suspend fun findCachedMedia(fileUniqueId: String): Message? =
        messageDao.findDownloadedByUniqueId(fileUniqueId)

    suspend fun getLastMessage(chatId: Long): Message? =
        messageDao.getLastMessage(chatId)

    suspend fun deleteOldMessages(chatId: Long, beforeTimestamp: Long) =
        messageDao.deleteOldMessages(chatId, beforeTimestamp)

    suspend fun fileExists(fileUniqueId: String): Boolean =
        messageDao.fileExists(fileUniqueId)

    suspend fun sendTextMessage(chatId: Long, text: String) {
        try {
            val sentMessage = bot.sendMessage(
                chatId = ChatId(RawChatId(chatId)),
                text = text
            )

            val dbMessage = sentMessage.toDbMessage(isOutgoing = true)

            messageDao.insert(dbMessage)

            Log.i("MessageRepository", "Message sent and saved: ${dbMessage.messageId}")

        } catch (e: Exception) {
            Log.e("MessageRepository", "Error sending message: ${e.message}")
//            throw e
        }
    }

}
