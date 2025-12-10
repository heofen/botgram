package com.heofen.botgram.data.repository

import com.heofen.botgram.database.dao.ChatDao
import com.heofen.botgram.database.tables.Chat
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {
    fun getAllChats(): Flow<List<Chat>> = chatDao.getAllChats()

    suspend fun insertChat(chat: Chat) = chatDao.insert(chat)

    suspend fun updateLastMessage(chatId: Long, text: String?, time: Long) =
        chatDao.updateLastMessage(chatId, text, time)
}