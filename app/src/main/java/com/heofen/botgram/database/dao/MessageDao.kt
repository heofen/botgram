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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<Message>)

    @Query("UPDATE messages SET isEdited = :isEdited, editedAt = :editedAt, text = :text WHERE chatId = :chatId AND messageId = :messageId")
    suspend fun updateMessage(chatId: Long, messageId: Long, text: String?, isEdited: Boolean, editedAt: Long?)

    @Query("SELECT * FROM messages WHERE fileUniqueId = :uniqueId AND fileLocalPath IS NOT NULL LIMIT 1")
    suspend fun findDownloadedByUniqueId(uniqueId: String): Message?
}