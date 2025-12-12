package com.heofen.botgram.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.heofen.botgram.database.tables.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getChatMessages(chatId: Long): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE mediaGroupId = :groupId ORDER BY messageId ASC")
    fun getMediaGroup(groupId: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND messageId = :messageId")
    suspend fun getMessage(chatId: Long, messageId: Long): Message?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<Message>)

    @Query("""
        UPDATE messages 
        SET isEdited = :isEdited, 
            editedAt = :editedAt, 
            text = :text,
            caption = :caption
        WHERE chatId = :chatId AND messageId = :messageId
    """)
    suspend fun updateMessage(
        chatId: Long,
        messageId: Long,
        text: String?,
        caption: String?,
        isEdited: Boolean,
        editedAt: Long?
    )

    @Query("UPDATE messages SET fileLocalPath = :localPath WHERE chatId = :chatId AND messageId = :messageId")
    suspend fun updateFilePath(chatId: Long, messageId: Long, localPath: String)

    @Query("SELECT * FROM messages WHERE fileUniqueId = :uniqueId AND fileLocalPath IS NOT NULL LIMIT 1")
    suspend fun findDownloadedByUniqueId(uniqueId: String): Message?

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessage(chatId: Long): Message?

    @Query("DELETE FROM messages WHERE chatId = :chatId AND timestamp < :beforeTimestamp")
    suspend fun deleteOldMessages(chatId: Long, beforeTimestamp: Long)
}
