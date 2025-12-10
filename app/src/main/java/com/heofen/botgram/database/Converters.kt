package com.heofen.botgram.database

import androidx.room.TypeConverter
import com.heofen.botgram.ChatType
import com.heofen.botgram.MessageType

class Converters {
    @TypeConverter
    fun fromChatType(value: ChatType): String = value.name

    @TypeConverter
    fun toChatType(value: String): ChatType = ChatType.valueOf(value)

    @TypeConverter
    fun fromMessageType(value: MessageType): String = value.name

    @TypeConverter
    fun toMessageType(value: String): MessageType = MessageType.valueOf(value)
}