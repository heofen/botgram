package com.heofen.botgram.data.repository

import com.heofen.botgram.MessageType
import com.heofen.botgram.data.MediaManager
import com.heofen.botgram.database.dao.ChatDao
import com.heofen.botgram.database.tables.Chat
import kotlinx.coroutines.flow.Flow
import java.io.File

class ChatRepository(
    private val chatDao: ChatDao,
    private val mediaManager: MediaManager
) {
    fun getAllChats(): Flow<List<Chat>> = chatDao.getAllChats()

    suspend fun getById(id: Long): Chat? = chatDao.getById(id)

    suspend fun chatExists(id: Long): Boolean = chatDao.chatExists(id)

    suspend fun insertChat(chat: Chat) = chatDao.insert(chat)

    suspend fun upsertChat(chat: Chat) = chatDao.upsert(chat)

    suspend fun updateLastMessage(chatId: Long, type: MessageType, text: String?, time: Long, senderId: Long?) =
        chatDao.updateLastMessage(chatId, type, text, time, senderId)

    suspend fun updateAvatar(
        chatId: Long,
        fileId: String?,
        fileUniqueId: String?,
        localPath: String?,
    ) = chatDao.updateAvatar(chatId, fileId, fileUniqueId, localPath)

    suspend fun loadAvatarIfMissing(chatId: Long) {
        val chat = chatDao.getById(chatId) ?: return

        if (chat.avatarLocalPath != null) {
            if (File(chat.avatarLocalPath).exists()) return
        }

        val (fileId, localPath) = mediaManager.downloadChatAvatar(chatId)

        if (localPath != null) {
            chatDao.updateAvatar(chatId, fileId, null, localPath)
        }
    }

    fun searchChats(query: String): Flow<List<Chat>> = chatDao.searchChats(query)
}