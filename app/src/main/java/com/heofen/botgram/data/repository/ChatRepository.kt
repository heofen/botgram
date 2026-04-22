package com.heofen.botgram.data.repository

import com.heofen.botgram.MessageType
import com.heofen.botgram.data.MediaManager
import com.heofen.botgram.data.remote.AvatarFetchResult
import com.heofen.botgram.data.remote.TelegramGateway
import com.heofen.botgram.data.sync.ChatSyncStore
import com.heofen.botgram.database.dao.ChatDao
import com.heofen.botgram.database.tables.Chat
import com.heofen.botgram.database.tables.ChatListItem
import com.heofen.botgram.utils.toDbChat
import kotlinx.coroutines.flow.Flow
import java.io.File

/** Репозиторий чатов и списка диалогов. */
class ChatRepository(
    private val chatDao: ChatDao,
    private val mediaManager: MediaManager,
    private val gateway: TelegramGateway
) : ChatSyncStore {
    /** Возвращает поток списка чатов для главного экрана. */
    fun getAllChats(): Flow<List<ChatListItem>> = chatDao.getAllChatListItems()

    /** Наблюдает за изменениями конкретного чата. */
    fun observeById(id: Long): Flow<Chat?> = chatDao.observeById(id)

    suspend fun getById(id: Long): Chat? = chatDao.getById(id)

    suspend fun chatExists(id: Long): Boolean = chatDao.chatExists(id)

    suspend fun insertChat(chat: Chat) = chatDao.insert(chat)

    override suspend fun upsertChat(chat: Chat) = chatDao.upsert(chat.mergeStoredState())

    override suspend fun updateLastMessage(chatId: Long, type: MessageType?, text: String?, time: Long?, senderId: Long?) =
        chatDao.updateLastMessage(chatId, type, text, time, senderId)

    override suspend fun updateAvatar(
        chatId: Long,
        fileId: String?,
        fileUniqueId: String?,
        localPath: String?,
    ) = chatDao.updateAvatar(chatId, fileId, fileUniqueId, localPath)

    /** Докачивает аватар чата, если он ещё не сохранён локально. */
    suspend fun loadAvatarIfMissing(chatId: Long) {
        val chat = chatDao.getById(chatId) ?: return

        if (chat.avatarLocalPath != null) {
            if (File(chat.avatarLocalPath).exists()) return
        }

        refreshAvatar(chatId)
    }

    /** Ищет чаты по названию и имени собеседника. */
    fun searchChats(query: String): Flow<List<ChatListItem>> = chatDao.searchChatListItems(query)

    /** Синхронизирует актуальный аватар чата с Telegram. */
    override suspend fun refreshAvatar(chatId: Long): AvatarFetchResult? {
        val current = chatDao.getById(chatId) ?: return null
        val avatar = mediaManager.downloadChatAvatar(chatId) ?: return null
        return applyFetchedAvatar(chatId = chatId, current = current, avatar = avatar)
    }

    /** Синхронизирует полную информацию о чате (включая описание). */
    suspend fun refreshChatInfo(chatId: Long) {
        val remoteChat = gateway.getChat(chatId) ?: return
        chatDao.updateDescription(chatId, remoteChat.description)
    }

    private suspend fun applyFetchedAvatar(
        chatId: Long,
        current: Chat,
        avatar: AvatarFetchResult
    ): AvatarFetchResult {
        return when (avatar) {
            is AvatarFetchResult.Available -> {
                val localPath = avatar.localPath
                    ?: current.avatarLocalPath?.takeIf { current.avatarFileUniqueId == avatar.fileUniqueId }
                chatDao.updateAvatar(chatId, avatar.fileId, avatar.fileUniqueId, localPath)
                AvatarFetchResult.Available(
                    fileId = avatar.fileId,
                    fileUniqueId = avatar.fileUniqueId,
                    localPath = localPath
                )
            }

            AvatarFetchResult.Missing -> {
                chatDao.updateAvatar(chatId, null, null, null)
                AvatarFetchResult.Missing
            }
        }
    }

    /** Сохраняет локально ценные поля, если сервер прислал неполную версию чата. */
    private suspend fun Chat.mergeStoredState(): Chat {
        val current = chatDao.getById(id) ?: return this
        return copy(
            lastMessageType = current.lastMessageType,
            lastMessageText = current.lastMessageText,
            lastMessageTime = current.lastMessageTime,
            lastMessageSenderId = current.lastMessageSenderId,
            avatarFileId = avatarFileId ?: current.avatarFileId,
            avatarFileUniqueId = avatarFileUniqueId ?: current.avatarFileUniqueId,
            avatarLocalPath = avatarLocalPath ?: current.avatarLocalPath,
            description = description ?: current.description
        )
    }
}
