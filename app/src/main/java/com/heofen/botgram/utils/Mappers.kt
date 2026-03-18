package com.heofen.botgram.utils

import com.heofen.botgram.data.remote.TelegramChat
import com.heofen.botgram.data.remote.TelegramIncomingMessage
import com.heofen.botgram.data.remote.TelegramUser
import com.heofen.botgram.database.tables.Chat
import com.heofen.botgram.database.tables.Message
import com.heofen.botgram.database.tables.User

fun TelegramIncomingMessage.toDbMessage(
    isOutgoing: Boolean = false,
    readStatus: Boolean = false
): Message {
    return Message(
        messageId = messageId,
        chatId = chatId,
        topicId = topicId,
        senderId = senderId,
        type = type,
        timestamp = timestamp,
        text = text,
        caption = caption,
        replyMsgId = replyMsgId,
        replyMsgTopicId = replyMsgTopicId,
        fileName = fileName,
        fileExtension = fileExtension,
        fileId = fileId,
        fileUniqueId = fileUniqueId,
        fileLocalPath = null,
        fileSize = fileSize,
        width = width,
        height = height,
        duration = duration,
        thumbnailFileId = thumbnailFileId,
        latitude = latitude,
        longitude = longitude,
        isEdited = isEdited,
        editedAt = editedAt,
        mediaGroupId = mediaGroupId,
        readStatus = readStatus,
        isOutgoing = isOutgoing
    )
}

fun TelegramChat.toDbChat(): Chat {
    return Chat(
        id = id,
        type = type,
        title = title,
        firstName = firstName,
        lastName = lastName,
        username = username,
        lastMessageType = lastMessageType,
        lastMessageText = lastMessageText,
        lastMessageTime = lastMessageTime,
        lastMessageSenderId = lastMessageSenderId,
        avatarFileId = null,
        avatarFileUniqueId = null,
        avatarLocalPath = null
    )
}

fun TelegramUser.toDbUser(): User {
    return User(
        id = id,
        firstName = firstName,
        lastName = lastName,
        bio = bio,
        avatarFileId = null,
        avatarFileUniqueId = null,
        avatarLocalPath = null,
        canWriteMsgToPm = canWriteMsgToPm
    )
}
