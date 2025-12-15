package com.heofen.botgram.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.heofen.botgram.ChatType
import com.heofen.botgram.MessageType
import com.heofen.botgram.R
import com.heofen.botgram.data.repository.ChatRepository
import com.heofen.botgram.data.repository.MessageRepository
import com.heofen.botgram.data.repository.UserRepository
import com.heofen.botgram.database.AppDatabase
import com.heofen.botgram.database.tables.Chat
import com.heofen.botgram.database.tables.Message
import com.heofen.botgram.database.tables.User
import dev.inmo.tgbotapi.bot.ktor.KtorRequestsExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onChatMemberUpdated
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onEditedContentMessage
import dev.inmo.tgbotapi.extensions.utils.asMediaContent
import dev.inmo.tgbotapi.extensions.utils.asMediaGroupContent
import dev.inmo.tgbotapi.extensions.utils.asTextContent
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.edit_date
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.media_group_id
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.reply_to_message
import dev.inmo.tgbotapi.extensions.utils.fromUserOrNull
import dev.inmo.tgbotapi.extensions.utils.thumbedMediaFileOrNull
import dev.inmo.tgbotapi.extensions.utils.usernameChatOrNull
import dev.inmo.tgbotapi.types.chat.ChannelChatImpl
import dev.inmo.tgbotapi.types.chat.ExtendedPrivateChat
import dev.inmo.tgbotapi.types.chat.GroupChatImpl
import dev.inmo.tgbotapi.types.chat.PrivateChatImpl
import dev.inmo.tgbotapi.types.chat.SupergroupChatImpl
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.AnimationContent
import dev.inmo.tgbotapi.types.message.content.AudioContent
import dev.inmo.tgbotapi.types.message.content.ContactContent
import dev.inmo.tgbotapi.types.message.content.DocumentContent
import dev.inmo.tgbotapi.types.message.content.LocationContent
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.message.content.PhotoContent
import dev.inmo.tgbotapi.types.message.content.StickerContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.message.content.VideoContent
import dev.inmo.tgbotapi.types.message.content.VideoNoteContent
import dev.inmo.tgbotapi.types.message.content.VoiceContent
import dev.inmo.tgbotapi.utils.PreviewFeature
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.TelegramAPIUrlsKeeper
import dev.inmo.tgbotapi.utils.extensions.threadIdOrNull
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.lang.Exception

class GetUpdates : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var messageRepo: MessageRepository
    private lateinit var chatRepo: ChatRepository
    private lateinit var userRepo: UserRepository

    override fun onCreate() {
        super.onCreate()

        val db = AppDatabase.getDatabase(applicationContext)
        messageRepo = MessageRepository(db.messageDao())
        chatRepo = ChatRepository(db.chatDao())
    }


    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val botToken = intent?.getStringExtra("BOT_TOKEN") ?: return START_NOT_STICKY

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)

        serviceScope.launch {
            while (isActive) {
                try {
                    Log.i("GetUpdates", "Launching Long Polling...")
                    startBot(botToken)
                } catch (e: Exception) {
                    Log.e("GetUpdates", "Bot crashed: ${e.message}. Restart from 5 sec...")
                    delay(5000)
                }
            }
        }

        return START_STICKY
    }

    private suspend fun startBot(token: String) {
        // НИКОГДА НЕ ТРОГАЙ КОД НИЖЕ. НЕ ПРИ КАКИХ ОБСТОЯТЕЛЬСТВАХ
        val client = HttpClient(OkHttp)

        val urlsKeeper = TelegramAPIUrlsKeeper(token)

        val bot = KtorRequestsExecutor(
            telegramAPIUrlsKeeper = urlsKeeper,
            client = client
        )


        bot.buildBehaviourWithLongPolling {
            onContentMessage { msg ->
                launch { handleMessage(msg) }
            }
            onEditedContentMessage {
                // TODO
            }
            onChatMemberUpdated {
                // TODO
            }
        }.join()
    }


    private suspend fun handleMessage(message: ContentMessage<MessageContent>) {
        try {
            val msg = mapToMessage(message)

            val chatExists = chatRepo.chatExists(msg.chatId)

            if (!chatExists) {
                val chat = mapToChat(message)
                chatRepo.insertChat(chat)
            } else {
                chatRepo.updateLastMessage(
                    chatId = msg.chatId,
                    type = msg.type,
                    text = msg.text,
                    time = msg.timestamp
                )
            }

            if (msg.senderId != null) {
                val userExists = userRepo.userExists(msg.senderId)
                if (!userExists) {
                    val user = mapToUser(message)
                    if (user != null) userRepo.insertUser(user)
                }
            }

            messageRepo.insertMessage(msg)

            Log.i("Handle message", "Success msg handle")
        } catch (e: kotlin.Exception) {
            Log.e("Handle message", "Error handling message: {}", e)
        }
    }

    @OptIn(PreviewFeature::class)
    private fun mapToUser(message: ContentMessage<MessageContent>): User? {
        val us = message.fromUserOrNull()?.user ?: return null

        return User(
            id = us.id.chatId.long,
            firstName = us.firstName,
            lastName = us.lastName,
            bio = null,

            avatarFileId = null,
            avatarFileUniqueId = null,
            avatarLocalPath = null,

            canWriteMsgToPm = when (message.chat) {
                is ExtendedPrivateChat -> true
                else -> false
            }
        )
    }

    // маппинг из ktg chat в энтити из приложения
    @OptIn(RiskFeature::class, PreviewFeature::class)
    private fun mapToChat(message: ContentMessage<MessageContent>): Chat {
        val chat = message.chat

        return Chat(
            id = chat.id.chatId.long,
            type = when {
                chat is PrivateChatImpl -> ChatType.PRIVATE
                chat is GroupChatImpl -> ChatType.GROUP
                chat is ChannelChatImpl -> ChatType.CHANNEL
                chat is SupergroupChatImpl -> ChatType.SUPERGROUP
                else -> ChatType.PRIVATE
            },
            title = when(chat) {
                is GroupChatImpl -> chat.title
                is SupergroupChatImpl -> chat.title
                is ChannelChatImpl -> chat.title
                else -> null
            },
            firstName = if (chat is PrivateChatImpl) {
                message.fromUserOrNull()?.user?.firstName
            } else null,
            lastName = if (chat is PrivateChatImpl) {
                message.fromUserOrNull()?.user?.lastName
            } else null,
            username = chat.usernameChatOrNull()?.toString(),

            lastMessageType = determineMessageType(message.content),
            lastMessageText = message.content.asTextContent()?.text,
            lastMessageTime = message.date.unixMillisLong,
            unreadCount = 0,

            avatarFileId = null,
            avatarFileUniqueId = null,
            avatarLocalPath = null,
        )
    }

    // маппинг из ktg msg в энтити из приложения
    @OptIn(PreviewFeature::class, RiskFeature::class)
    private fun mapToMessage(message: ContentMessage<MessageContent>): Message{
        return Message(
            messageId = message.messageId.long,
            chatId = message.chat.id.chatId.long,
            topicId = message.threadIdOrNull?.long,
            senderId = message.fromUserOrNull()?.user?.id?.chatId?.long,
            type = determineMessageType(message.content),
            timestamp = message.date.unixMillisLong,

            text = message.content.asTextContent()?.text,
            caption = message.content.asMediaGroupContent()?.text
                ?: message.content.asMediaContent()?.asTextContent()?.text,

            replyMsgId = message.reply_to_message?.messageId?.long,
            replyMsgTopicId = message.threadIdOrNull?.long,

            fileName = extractFileName(message.content),
            fileExtension = extractFileExtension(message.content),
            fileId = extractFileId(message.content),
            fileUniqueId = extractFileUniqueId(message.content),
            fileLocalPath = null,
            fileSize = extractFileSize(message.content),

            width = extractMediaWidth(message.content),
            height = extractMediaHeight(message.content),
            duration = extractMediaDuration(message.content),
            thumbnailFileId = extractThumbnailFileId(message.content),

            isEdited = message.edit_date != null,
            editedAt = message.edit_date?.asDate?.unixMillisLong,

            mediaGroupId = message.media_group_id?.toString(),

            isOutgoing = false
        )
    }

    private fun extractFileExtension(content: MessageContent): String? {
        return when(content) {
            is DocumentContent -> {
                content.media.fileName?.substringAfterLast('.', "")
                    ?: getMimeTypeExtension(content.media.mimeType?.raw)
            }
            is AudioContent -> {
                content.media.fileName?.substringAfterLast('.', "")
                    ?: "mp3"
            }

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
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            "video/mp4" -> "mp4"
            "video/mpeg" -> "mpeg"
            "audio/mpeg" -> "mp3"
            "audio/ogg" -> "ogg"
            "application/pdf" -> "pdf"
            "application/zip" -> "zip"
            "application/x-rar-compressed" -> "rar"
            "text/plain" -> "txt"
            else -> "bin"
        }
    }

    private fun extractFileName(content: MessageContent): String? {
        return when(content) {
            is DocumentContent -> content.media.fileName
            is AudioContent -> content.media.fileName

            is PhotoContent -> null
            is VideoContent -> null
            is VoiceContent -> null
            is StickerContent -> null
            is AnimationContent -> null
            is VideoNoteContent -> null
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


    private fun determineMessageType(content: MessageContent): MessageType {
        return when {
            content is TextContent -> MessageType.TEXT
            content is PhotoContent -> MessageType.PHOTO
            content is VideoContent -> MessageType.VIDEO
            content is AnimationContent -> MessageType.ANIMATION
            content is AudioContent -> MessageType.AUDIO
            content is VoiceContent -> MessageType.VOICE
            content is VideoNoteContent -> MessageType.VIDEO_NOTE
            content is DocumentContent -> MessageType.DOCUMENT
            content is StickerContent -> {
                when {
                    content.media.isAnimated -> MessageType.ANIMATED_STICKER
                    content.media.isVideo -> MessageType.VIDEO_STICKER
                    else -> MessageType.STICKER
                }
            }
            content is ContactContent -> MessageType.CONTACT
            content is LocationContent -> MessageType.LOCATION
            else -> MessageType.TEXT
        }
    }

    private fun createNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Get Updates Service",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Get Updates Service")
            .setContentText("Running...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setShowWhen(false)
            .setSound(null)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()

        serviceScope.cancel()
    }

    override fun onBind(intent: Intent): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "get_updates_service"
    }
}