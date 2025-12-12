package com.heofen.botgram.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.heofen.botgram.MessageType
import com.heofen.botgram.R
import com.heofen.botgram.data.repository.ChatRepository
import com.heofen.botgram.data.repository.MessageRepository
import com.heofen.botgram.database.AppDatabase
import com.heofen.botgram.database.tables.Message
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
import dev.inmo.tgbotapi.extensions.utils.asMediaGroupMessage
import dev.inmo.tgbotapi.extensions.utils.asPhotoContent
import dev.inmo.tgbotapi.extensions.utils.asTextContent
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.channel_chat_created
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.edit_date
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.media_group_id
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.reply_to_message
import dev.inmo.tgbotapi.extensions.utils.fromUserOrNull
import dev.inmo.tgbotapi.extensions.utils.messageContentOrNull
import dev.inmo.tgbotapi.extensions.utils.thumbedMediaFileOrNull
import dev.inmo.tgbotapi.types.files.AnimatedSticker
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
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
import korlibs.time.jvm.toDate
import kotlinx.coroutines.cancel
import java.lang.Exception

class GetUpdates : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var messageRepo: MessageRepository
    private lateinit var chatRepo: ChatRepository

    override fun onCreate() {
        super.onCreate()

        val db = AppDatabase.getDatabase(applicationContext)
        messageRepo = MessageRepository(db.messageDao())
        chatRepo = ChatRepository(db.chatDao())
    }


    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val botToken = "" //intent?.getStringExtra("BOT_TOKEN") ?: return START_NOT_STICKY

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        serviceScope.launch {
            try {
                startBot(botToken)
            } catch (e: Exception) {
                Log.e("GetUpdatesService", "Error starting bot: ${e.message}")
                stopSelf()
            }
        }

        return START_STICKY
        TODO("Сделать по человеческий получение токена")
    }

    private suspend fun startBot(token: String) {
        val bot = telegramBot(token)

        bot.buildBehaviourWithLongPolling {
            onContentMessage {
                TODO()
            }

            onEditedContentMessage {
                TODO()
            }

            onChatMemberUpdated {
                TODO()
            }
        }

    }

    private suspend fun handleMessage(message: ContentMessage<MessageContent>) {

    }


    // маппинг из ktg msg в энтити из приложения
    @OptIn(PreviewFeature::class, RiskFeature::class)
    private suspend fun mapToMessage(message: ContentMessage<MessageContent>): Message{
        return Message(
            messageId = message.messageId.long,
            chatId = message.chat.id.chatId.long,
            senderId = message.fromUserOrNull()?.user?.id?.chatId?.long,
            type = determineMessageType(message.content),
            timestamp = message.date.unixMillisLong,

            text = message.content.asTextContent()?.text,
            caption = message.content.asMediaGroupContent()?.text
                ?: message.content.asMediaContent()?.asTextContent()?.text,

            replyMsgId = message.reply_to_message?.messageId?.long,

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

            mediaGroupId = message.media_group_id.toString(),

            isOutgoing = false
        )
    }

    private fun extractThumbnailFileId(content: MessageContent): String? {
        return when(content) {
            is PhotoContent -> content.media.thumbedMediaFileOrNull()?.fileId?.toString()
            is VideoContent -> content.media.thumbnail?.fileId.toString()
            is DocumentContent -> content.media.thumbnail?.fileId.toString()
            is AudioContent -> content.media.thumbnail?.fileId.toString()
            is VideoNoteContent -> content.media.thumbnail?.fileId.toString()
            is StickerContent -> content.media.thumbnail?.fileId.toString()
            else -> null
        }
    }

    private fun extractMediaDuration(content: MessageContent): Long? {
        return when(content) {
            is VideoContent -> content.media.duration
            is VideoNoteContent -> content.media.duration
            is AudioContent -> content.media.duration
            is VoiceContent -> content.media.duration
            else -> null
        }
    }

    private fun extractMediaWidth(content: MessageContent): Int? {
        return when(content) {
            is PhotoContent -> content.mediaCollection.maxByOrNull { it.width }?.width
            is VideoContent -> content.media.width
            is VideoNoteContent -> content.media.width
            is StickerContent -> content.media.width
            else -> null
        }
    }

    private fun extractMediaHeight(content: MessageContent): Int? {
        return when(content) {
            is PhotoContent -> content.mediaCollection.maxByOrNull { it.width }?.height
            is VideoContent -> content.media.height
            is VideoNoteContent -> content.media.height
            is StickerContent -> content.media.height
            else -> null
        }
    }

    private fun extractFileId(content: MessageContent): String? {
        return when(content) {
            is PhotoContent -> content.mediaCollection.maxByOrNull { it.width }?.fileId.toString()
            is VideoContent -> content.media.fileId.toString()
            is AudioContent -> content.media.fileId.toString()
            is VoiceContent -> content.media.fileId.toString()
            is VideoNoteContent -> content.media.fileId.toString()
            is DocumentContent -> content.media.fileId.toString()
            is StickerContent -> content.media.fileId.toString()
            else -> null
        }
    }

    private fun extractFileUniqueId(content: MessageContent): String? {
        return when(content) {
            is PhotoContent -> content.mediaCollection.maxByOrNull { it.width }?.fileUniqueId.toString()
            is VideoContent -> content.media.fileUniqueId.toString()
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
            is PhotoContent -> content.mediaCollection.maxByOrNull { it.width }?.fileSize?.toLong()
            is VideoContent -> content.media.fileSize?.toLong()
            is AudioContent -> content.media.fileSize?.toLong()
            is VoiceContent -> content.media.fileSize?.toLong()
            is VideoNoteContent -> content.media.fileSize?.toLong()
            is DocumentContent -> content.media.fileSize?.toLong()
            is StickerContent -> content.media.fileSize?.toLong()
            else -> null
        }
    }


    private fun determineMessageType(content: MessageContent): MessageType {
        return when {
            content is TextContent -> MessageType.TEXT
            content is PhotoContent -> MessageType.PHOTO
            content is VideoContent -> MessageType.VIDEO
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