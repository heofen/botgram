package com.heofen.botgram.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.heofen.botgram.MessageType
import com.heofen.botgram.database.tables.Chat
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY lastMessageTime DESC")
    fun getAllChats(): Flow<List<Chat>>

    @Query("SELECT * FROM chats WHERE id = :id")
    suspend fun getById(id: Long): Chat?

    @Query("SELECT EXISTS(SELECT * FROM chats WHERE id = :chatId)")
    suspend fun chatExists(chatId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chat: Chat)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(chat: Chat)

    @Query("""
        UPDATE chats 
        SET lastMessageType = :type,
            lastMessageText = :text, 
            lastMessageTime = :time,
            lastMessageSenderId = :senderId
        WHERE id = :chatId
    """)
    suspend fun updateLastMessage(chatId: Long, type: MessageType, text: String?, time: Long, senderId: Long?)


//    @Query("UPDATE chats SET isMuted = :isMuted WHERE id = :chatId")
//    suspend fun updateMuted(chatId: Long, isMuted: Boolean)

    @Query("""
        UPDATE chats 
        SET avatarFileId = :fileId,
            avatarFileUniqueId = :fileUniqueId,
            avatarLocalPath = :localPath
    WHERE id = :chatId
    """)
    suspend fun updateAvatar(
        chatId: Long,
        fileId: String?,
        fileUniqueId: String?,
        localPath: String?,
    )

    @Query("SELECT * FROM chats WHERE title LIKE '%' || :query || '%' OR firstName LIKE '%' || :query || '%' OR lastName LIKE '%' || :query || '%' ORDER BY lastMessageTime DESC")
    fun searchChats(query: String): Flow<List<Chat>>
}
