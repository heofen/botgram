package com.heofen.botgram.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.heofen.botgram.MainActivity
import com.heofen.botgram.MessageType
import com.heofen.botgram.R
import com.heofen.botgram.data.local.ActiveChatTracker
import com.heofen.botgram.data.local.NotificationPreferences
import com.heofen.botgram.database.tables.Chat
import com.heofen.botgram.database.tables.Message
import com.heofen.botgram.database.tables.User

/**
 * Публикует системные уведомления о новых входящих сообщениях.
 */
class MessageNotifier(
    private val context: Context,
    private val preferences: NotificationPreferences,
    private val activeChatTracker: ActiveChatTracker
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        ensureChannel()
    }

    /** Показывает уведомление об одном входящем сообщении. */
    fun notify(message: Message, chat: Chat?, sender: User?) {
        if (message.isOutgoing) return
        if (preferences.isMuted(message.chatId)) return
        if (activeChatTracker.current() == message.chatId) return
        if (!hasPostPermission()) return

        val title = chat?.let(::resolveChatTitle)
            ?: sender?.let(::resolveSenderName)
            ?: context.getString(R.string.notification_message_default_sender)
        val body = resolveMessagePreview(message, sender, isPersonalChat = chat?.firstName != null || chat == null)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            message.chatId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_ic_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setWhen(message.timestamp)
            .setShowWhen(true)
            .setContentIntent(contentIntent)

        runCatching {
            notificationManager.notify(NOTIFICATION_TAG, message.chatId.hashCode(), builder.build())
        }
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_message_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_message_channel_description)
            enableVibration(true)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun hasPostPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun resolveChatTitle(chat: Chat): String? {
        chat.title?.takeIf { it.isNotBlank() }?.let { return it }
        val full = listOfNotNull(chat.firstName, chat.lastName)
            .joinToString(" ")
            .takeIf { it.isNotBlank() }
        if (full != null) return full
        return chat.username?.takeIf { it.isNotBlank() }?.let { "@$it" }
    }

    private fun resolveSenderName(sender: User): String {
        val full = listOfNotNull(sender.firstName, sender.lastName)
            .joinToString(" ")
            .takeIf { it.isNotBlank() }
        if (full != null) return full
        return sender.username?.takeIf { it.isNotBlank() }?.let { "@$it" }
            ?: context.getString(R.string.notification_message_default_sender)
    }

    private fun resolveMessagePreview(
        message: Message,
        sender: User?,
        isPersonalChat: Boolean
    ): String {
        val body = when (message.type) {
            MessageType.TEXT -> message.text.orEmpty()
            MessageType.PHOTO -> withCaption(R.string.notification_message_type_photo, message.caption)
            MessageType.VIDEO -> withCaption(R.string.notification_message_type_video, message.caption)
            MessageType.ANIMATION -> withCaption(R.string.notification_message_type_animation, message.caption)
            MessageType.AUDIO -> withCaption(R.string.notification_message_type_audio, message.caption)
            MessageType.VOICE -> context.getString(R.string.notification_message_type_voice)
            MessageType.VIDEO_NOTE -> context.getString(R.string.notification_message_type_video_note)
            MessageType.DOCUMENT -> withCaption(R.string.notification_message_type_document, message.caption)
            MessageType.STICKER,
            MessageType.ANIMATED_STICKER,
            MessageType.VIDEO_STICKER -> context.getString(R.string.notification_message_type_sticker)
            MessageType.CONTACT -> context.getString(R.string.notification_message_type_contact)
            MessageType.LOCATION -> context.getString(R.string.notification_message_type_location)
        }

        // В групповых чатах добавляем имя отправителя, чтобы было понятно, кто написал.
        val showSenderPrefix = !isPersonalChat && sender != null
        return if (showSenderPrefix) {
            val senderName = resolveSenderName(sender!!)
            if (body.isBlank()) senderName else "$senderName: $body"
        } else {
            body
        }
    }

    private fun withCaption(typeRes: Int, caption: String?): String {
        val base = context.getString(typeRes)
        val trimmedCaption = caption?.trim().orEmpty()
        return if (trimmedCaption.isNotEmpty()) "$base · $trimmedCaption" else base
    }

    companion object {
        private const val CHANNEL_ID = "messages_channel"
        private const val NOTIFICATION_TAG = "message"
    }
}
