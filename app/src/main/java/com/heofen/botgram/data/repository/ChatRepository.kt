package com.heofen.botgram.data.repository

import com.heofen.botgram.MessageType
import com.heofen.botgram.data.MediaManager
import com.heofen.botgram.data.sync.ChatSyncStore
import com.heofen.botgram.database.dao.ChatDao
import com.heofen.botgram.database.tables.Chat
import com.heofen.botgram.database.tables.ChatListItem
import kotlinx.coroutines.flow.Flow
import java.io.File

class ChatRepository(
    private val chatDao: ChatDao,
    private val mediaManager: MediaManager
) : ChatSyncStore {
    fun getAllChats(): Flow<List<ChatListItem>> = chatDao.getAllChatListItems()

    fun observeById(id: Long): Flow<Chat?> = chatDao.observeById(id)

    suspend fun getById(id: Long): Chat? = chatDao.getById(id)

    suspend fun chatExists(id: Long): Boolean = chatDao.chatExists(id)

    suspend fun insertChat(chat: Chat) = chatDao.insert(chat)

    override suspend fun upsertChat(chat: Chat) = chatDao.upsert(chat.mergeStoredState())

    override suspend fun updateLastMessage(chatId: Long, type: MessageType?, text: String?, time: Long?, senderId: Long?) =
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

        val avatar = mediaManager.downloadChatAvatar(chatId) ?: return

        if (avatar.localPath != null) {
            chatDao.updateAvatar(chatId, avatar.fileId, avatar.fileUniqueId, avatar.localPath)
        }
    }

    fun searchChats(query: String): Flow<List<ChatListItem>> = chatDao.searchChatListItems(query)

    private suspend fun Chat.mergeStoredState(): Chat {
        val current = chatDao.getById(id) ?: return this
        return copy(
            lastMessageType = current.lastMessageType,
            lastMessageText = current.lastMessageText,
            lastMessageTime = current.lastMessageTime,
            lastMessageSenderId = current.lastMessageSenderId,
            avatarFileId = avatarFileId ?: current.avatarFileId,
            avatarFileUniqueId = avatarFileUniqueId ?: current.avatarFileUniqueId,
            avatarLocalPath = avatarLocalPath ?: current.avatarLocalPath
        )
    }
}
