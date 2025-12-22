package com.heofen.botgram.utils

import dev.inmo.tgbotapi.utils.PreviewFeature
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.extensions.threadIdOrNull
import com.heofen.botgram.MessageType
import com.heofen.botgram.database.tables.Message
import dev.inmo.tgbotapi.extensions.utils.*
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.edit_date
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.media_group_id
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.reply_to_message
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.*

@OptIn(PreviewFeature::class, RiskFeature::class)
fun ContentMessage<MessageContent>.toDbMessage(isOutgoing: Boolean = false): Message {
    return Message(
        messageId = this.messageId.long,
        chatId = this.chat.id.chatId.long,
        topicId = this.threadIdOrNull?.long,
        senderId = this.fromUserOrNull()?.user?.id?.chatId?.long,
        type = determineMessageType(this.content),
        timestamp = this.date.unixMillisLong,

        text = this.content.asTextContent()?.text,
        caption = this.content.asMediaGroupContent()?.text
            ?: this.content.asMediaContent()?.asTextContent()?.text,

        replyMsgId = this.reply_to_message?.messageId?.long,
        replyMsgTopicId = this.threadIdOrNull?.long,

        fileName = extractFileName(this.content),
        fileExtension = extractFileExtension(this.content),
        fileId = extractFileId(this.content),
        fileUniqueId = extractFileUniqueId(this.content),
        fileLocalPath = null,
        fileSize = extractFileSize(this.content),

        width = extractMediaWidth(this.content),
        height = extractMediaHeight(this.content),
        duration = extractMediaDuration(this.content),
        thumbnailFileId = extractThumbnailFileId(this.content),

        isEdited = this.edit_date != null,
        editedAt = this.edit_date?.asDate?.unixMillisLong,

        mediaGroupId = this.media_group_id?.toString(),

        isOutgoing = isOutgoing,
        readStatus = true
    )
}

private fun determineMessageType(content: MessageContent): MessageType {
    return when (content) {
        is TextContent -> MessageType.TEXT
        is PhotoContent -> MessageType.PHOTO
        is VideoContent -> MessageType.VIDEO
        is AnimationContent -> MessageType.ANIMATION
        is AudioContent -> MessageType.AUDIO
        is VoiceContent -> MessageType.VOICE
        is VideoNoteContent -> MessageType.VIDEO_NOTE
        is DocumentContent -> MessageType.DOCUMENT
        is StickerContent -> {
            when {
                content.media.isAnimated -> MessageType.ANIMATED_STICKER
                content.media.isVideo -> MessageType.VIDEO_STICKER
                else -> MessageType.STICKER
            }
        }
        is ContactContent -> MessageType.CONTACT
        is LocationContent -> MessageType.LOCATION
        else -> MessageType.TEXT
    }
}

private fun extractFileExtension(content: MessageContent): String? {
    return when(content) {
        is DocumentContent -> content.media.fileName?.substringAfterLast('.', "")
            ?: getMimeTypeExtension(content.media.mimeType?.raw)
        is AudioContent -> content.media.fileName?.substringAfterLast('.', "") ?: "mp3"
        is PhotoContent -> "jpg"
        is VideoContent -> "mp4"
        is AnimationContent -> "mp4"
        is VoiceContent -> "ogg"
        is VideoNoteContent -> "mp4"
        is StickerContent -> when {
            content.media.isAnimated -> "tgs"
            content.media.isVideo -> "webm"
            else -> "webp"
        }
        else -> null
    }
}

private fun getMimeTypeExtension(mimeType: String?): String {
    return when(mimeType) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "video/mp4" -> "mp4"
        "audio/mpeg" -> "mp3"
        "audio/ogg" -> "ogg"
        "application/pdf" -> "pdf"
        "text/plain" -> "txt"
        else -> "bin"
    }
}

private fun extractFileName(content: MessageContent): String? {
    return when(content) {
        is DocumentContent -> content.media.fileName
        is AudioContent -> content.media.fileName
        else -> null
    }
}

private fun extractThumbnailFileId(content: MessageContent): String? {
    return when(content) {
        is PhotoContent -> content.media.thumbedMediaFileOrNull()?.fileId?.fileId
        is VideoContent -> content.media.thumbnail?.fileId?.fileId
        is AnimationContent -> content.media.thumbnail?.fileId?.fileId
        is DocumentContent -> content.media.thumbnail?.fileId?.fileId
        is AudioContent -> content.media.thumbnail?.fileId?.fileId
        is VideoNoteContent -> content.media.thumbnail?.fileId?.fileId
        is StickerContent -> content.media.thumbnail?.fileId?.fileId
        else -> null
    }
}

private fun extractMediaDuration(content: MessageContent): Long? {
    return when(content) {
        is VideoContent -> content.media.duration
        is VideoNoteContent -> content.media.duration
        is AnimationContent -> content.media.duration
        is AudioContent -> content.media.duration
        is VoiceContent -> content.media.duration
        else -> null
    }
}

private fun extractMediaWidth(content: MessageContent): Int? {
    return when(content) {
        is PhotoContent -> content.mediaCollection.maxByOrNull { it.width }?.width
        is VideoContent -> content.media.width
        is AnimationContent -> content.media.width
        is VideoNoteContent -> content.media.width
        is StickerContent -> content.media.width
        else -> null
    }
}

private fun extractMediaHeight(content: MessageContent): Int? {
    return when(content) {
        is PhotoContent -> content.mediaCollection.maxByOrNull { it.width }?.height
        is VideoContent -> content.media.height
        is AnimationContent -> content.media.height
        is VideoNoteContent -> content.media.height
        is StickerContent -> content.media.height
        else -> null
    }
}

private fun extractFileId(content: MessageContent): String? {
    return when(content) {
        is PhotoContent -> content.mediaCollection.maxByOrNull { it.width }?.fileId?.fileId
        is VideoContent -> content.media.fileId.fileId
        is AnimationContent -> content.media.fileId.fileId
        is AudioContent -> content.media.fileId.fileId
        is VoiceContent -> content.media.fileId.fileId
        is VideoNoteContent -> content.media.fileId.fileId
        is DocumentContent -> content.media.fileId.fileId
        is StickerContent -> content.media.fileId.fileId
        else -> null
    }
}

private fun extractFileUniqueId(content: MessageContent): String? {
    return when(content) {
        is PhotoContent -> content.mediaCollection.maxByOrNull { it.width }?.fileUniqueId.toString()
        is VideoContent -> content.media.fileUniqueId.toString()
        is AnimationContent -> content.media.fileUniqueId.toString()
        is AudioContent -> content.media.fileUniqueId.toString()
        is VoiceContent -> content.media.fileUniqueId.toString()
        is VideoNoteContent -> content.media.fileUniqueId.toString()
        is DocumentContent -> content.media.fileUniqueId.toString()
        is StickerContent -> content.media.fileUniqueId.toString()
        else -> null
    }
}

private fun extractFileSize(content: MessageContent): Long? {
    return when(content) {
        is PhotoContent -> content.mediaCollection.maxByOrNull { it.width }?.fileSize
        is VideoContent -> content.media.fileSize
        is AnimationContent -> content.media.fileSize
        is AudioContent -> content.media.fileSize
        is VoiceContent -> content.media.fileSize
        is VideoNoteContent -> content.media.fileSize
        is DocumentContent -> content.media.fileSize
        is StickerContent -> content.media.fileSize
        else -> null
    }
}