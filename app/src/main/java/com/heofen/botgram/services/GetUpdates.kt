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
import com.heofen.botgram.R
import com.heofen.botgram.data.MediaManager
import com.heofen.botgram.data.local.TokenManager
import com.heofen.botgram.data.repository.ChatRepository
import com.heofen.botgram.data.repository.MessageRepository
import com.heofen.botgram.data.repository.UserRepository
import com.heofen.botgram.database.AppDatabase
import com.heofen.botgram.database.tables.Chat
import com.heofen.botgram.database.tables.User
import com.heofen.botgram.utils.determineMessageType
import com.heofen.botgram.utils.toDbMessage
import dev.inmo.tgbotapi.bot.ktor.KtorRequestsExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onChatMemberUpdated
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onEditedContentMessage
import dev.inmo.tgbotapi.extensions.utils.asTextContent
import dev.inmo.tgbotapi.extensions.utils.fromUserOrNull
import dev.inmo.tgbotapi.extensions.utils.usernameChatOrNull
import dev.inmo.tgbotapi.types.chat.ChannelChatImpl
import dev.inmo.tgbotapi.types.chat.ExtendedPrivateChat
import dev.inmo.tgbotapi.types.chat.GroupChatImpl
import dev.inmo.tgbotapi.types.chat.PrivateChatImpl
import dev.inmo.tgbotapi.types.chat.SupergroupChatImpl
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.utils.PreviewFeature
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.TelegramAPIUrlsKeeper
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
    private var httpClient: HttpClient? = null
    private var bot: KtorRequestsExecutor? = null
    private var pollingJob: Job? = null
    private var currentToken: String? = null


    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)

        val tokenManager = TokenManager(applicationContext)
        val token = tokenManager.getToken()

        if (token.isNullOrBlank()) {
            Log.e("GetUpdates", "Token not found. Stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }

        if (currentToken != token) {
            pollingJob?.cancel()
            pollingJob = null

            httpClient?.close()
            httpClient = HttpClient(OkHttp)
            bot = KtorRequestsExecutor(TelegramAPIUrlsKeeper(token), httpClient!!)
            currentToken = token
        }

        val activeBot = bot
        if (activeBot == null) {
            Log.e("GetUpdates", "Bot is not initialized. Stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }

        val mediaManager = MediaManager(applicationContext, activeBot)
        val db = AppDatabase.getDatabase(applicationContext)

        messageRepo = MessageRepository(db.messageDao(), activeBot, mediaManager)
        chatRepo = ChatRepository(db.chatDao(), mediaManager)
        userRepo = UserRepository(db.userDao(), mediaManager)

        if (pollingJob?.isActive != true) {
            pollingJob = serviceScope.launch {
                while (isActive) {
                    try {
                        Log.i("GetUpdates", "Launching Long Polling...")
                        startBot(activeBot)
                    } catch (e: Exception) {
                        Log.e("GetUpdates", "Bot crashed: ${e.message}. Restart from 5 sec...")
                        delay(5000)
                    }
                }
            }
        }

        return START_STICKY
    }

    private suspend fun startBot(bot: KtorRequestsExecutor) {
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
            val msg = message.toDbMessage(isOutgoing = false, readStatus = false)

            val chatExists = chatRepo.chatExists(msg.chatId)
            if (!chatExists) {
                val chat = mapToChat(message)
                chatRepo.insertChat(chat)
            } else {
                chatRepo.updateLastMessage(
                    chatId = msg.chatId,
                    type = msg.type,
                    text = msg.text,
                    time = msg.timestamp,
                    senderId = msg.senderId
                )
            }

            try {
                if (msg.senderId != null) {
                    val userExists = userRepo.userExists(msg.senderId)
                    if (!userExists) {
                        val user = mapToUser(message)
                        if (user != null) userRepo.insertUser(user)
                    }
                }
            } catch (e: Exception) {
                Log.e("User handle", "User handle err", e)
            }

            try {
                messageRepo.insertMessage(msg)

                serviceScope.launch {
                    messageRepo.ensureMediaDownloaded(msg)
                }
            } catch (e: Exception) {
                Log.e("msg handle", "msg handle err", e)
            }

            Log.i("Handle message", "Success msg handle")
        } catch (e: Exception) {
            Log.e("Handle message", "Error handling message", e)
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
            lastMessageSenderId = message.fromUserOrNull()?.user?.id?.chatId?.long,

            avatarFileId = null,
            avatarFileUniqueId = null,
            avatarLocalPath = null,
        )
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

        pollingJob?.cancel()
        pollingJob = null
        httpClient?.close()
        httpClient = null
        bot = null
        currentToken = null
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "get_updates_service"
    }
}
