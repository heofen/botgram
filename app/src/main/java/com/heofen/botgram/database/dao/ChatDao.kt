package com.heofen.botgram.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.heofen.botgram.MessageType
import com.heofen.botgram.database.tables.Chat
import com.heofen.botgram.database.tables.ChatListItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query(
        """
        SELECT
            c.*,
            m.messageId AS lastMessage_messageId,
            m.chatId AS lastMessage_chatId,
            m.topicId AS lastMessage_topicId,
            m.senderId AS lastMessage_senderId,
            m.type AS lastMessage_type,
            m.timestamp AS lastMessage_timestamp,
            m.text AS lastMessage_text,
            m.caption AS lastMessage_caption,
            m.replyMsgId AS lastMessage_replyMsgId,
            m.replyMsgTopicId AS lastMessage_replyMsgTopicId,
            m.fileName AS lastMessage_fileName,
            m.fileExtension AS lastMessage_fileExtension,
            m.fileId AS lastMessage_fileId,
            m.fileUniqueId AS lastMessage_fileUniqueId,
            m.fileLocalPath AS lastMessage_fileLocalPath,
            m.fileSize AS lastMessage_fileSize,
            m.width AS lastMessage_width,
            m.height AS lastMessage_height,
            m.duration AS lastMessage_duration,
            m.thumbnailFileId AS lastMessage_thumbnailFileId,
            m.isEdited AS lastMessage_isEdited,
            m.editedAt AS lastMessage_editedAt,
            m.mediaGroupId AS lastMessage_mediaGroupId,
            m.readStatus AS lastMessage_readStatus,
            m.isOutgoing AS lastMessage_isOutgoing
        FROM chats c
        LEFT JOIN messages m
            ON m.chatId = c.id
            AND m.messageId = (
                SELECT lm.messageId
                FROM messages lm
                WHERE lm.chatId = c.id
                ORDER BY lm.timestamp DESC, lm.messageId DESC
                LIMIT 1
            )
        ORDER BY c.lastMessageTime DESC
        """
    )
    fun getAllChatListItems(): Flow<List<ChatListItem>>

    @Query("SELECT * FROM chats WHERE id = :id")
    fun observeById(id: Long): Flow<Chat?>

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

    @Query(
        """
        SELECT
            c.*,
            m.messageId AS lastMessage_messageId,
            m.chatId AS lastMessage_chatId,
            m.topicId AS lastMessage_topicId,
            m.senderId AS lastMessage_senderId,
            m.type AS lastMessage_type,
            m.timestamp AS lastMessage_timestamp,
            m.text AS lastMessage_text,
            m.caption AS lastMessage_caption,
            m.replyMsgId AS lastMessage_replyMsgId,
            m.replyMsgTopicId AS lastMessage_replyMsgTopicId,
            m.fileName AS lastMessage_fileName,
            m.fileExtension AS lastMessage_fileExtension,
            m.fileId AS lastMessage_fileId,
            m.fileUniqueId AS lastMessage_fileUniqueId,
            m.fileLocalPath AS lastMessage_fileLocalPath,
            m.fileSize AS lastMessage_fileSize,
            m.width AS lastMessage_width,
            m.height AS lastMessage_height,
            m.duration AS lastMessage_duration,
            m.thumbnailFileId AS lastMessage_thumbnailFileId,
            m.isEdited AS lastMessage_isEdited,
            m.editedAt AS lastMessage_editedAt,
            m.mediaGroupId AS lastMessage_mediaGroupId,
            m.readStatus AS lastMessage_readStatus,
            m.isOutgoing AS lastMessage_isOutgoing
        FROM chats c
        LEFT JOIN messages m
            ON m.chatId = c.id
            AND m.messageId = (
                SELECT lm.messageId
                FROM messages lm
                WHERE lm.chatId = c.id
                ORDER BY lm.timestamp DESC, lm.messageId DESC
                LIMIT 1
            )
        WHERE c.title LIKE '%' || :query || '%'
            OR c.firstName LIKE '%' || :query || '%'
            OR c.lastName LIKE '%' || :query || '%'
        ORDER BY c.lastMessageTime DESC
        """
    )
    fun searchChatListItems(query: String): Flow<List<ChatListItem>>
}
