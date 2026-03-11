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
import com.heofen.botgram.R
import com.heofen.botgram.data.MediaManager
import com.heofen.botgram.data.local.TokenManager
import com.heofen.botgram.data.remote.TelegramGateway
import com.heofen.botgram.data.remote.TelegramGatewayFactory
import com.heofen.botgram.data.remote.TelegramIncomingMessage
import com.heofen.botgram.data.repository.ChatRepository
import com.heofen.botgram.data.repository.MessageRepository
import com.heofen.botgram.data.repository.UserRepository
import com.heofen.botgram.database.AppDatabase
import com.heofen.botgram.utils.toDbChat
import com.heofen.botgram.utils.toDbMessage
import com.heofen.botgram.utils.toDbUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GetUpdates : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var messageRepo: MessageRepository
    private lateinit var chatRepo: ChatRepository
    private lateinit var userRepo: UserRepository
    private var gateway: TelegramGateway? = null
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

            gateway?.close()
            gateway = TelegramGatewayFactory.create(applicationContext, token)
            currentToken = token
        }

        val activeGateway = gateway
        if (activeGateway == null) {
            Log.e("GetUpdates", "Gateway is not initialized. Stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }

        val mediaManager = MediaManager(activeGateway)
        val db = AppDatabase.getDatabase(applicationContext)

        messageRepo = MessageRepository(db.messageDao(), activeGateway, mediaManager)
        chatRepo = ChatRepository(db.chatDao(), mediaManager)
        userRepo = UserRepository(db.userDao(), mediaManager)

        if (pollingJob?.isActive != true) {
            pollingJob = serviceScope.launch {
                while (isActive) {
                    try {
                        Log.i("GetUpdates", "Launching Long Polling...")
                        activeGateway.collectUpdates { message ->
                            handleMessage(message)
                        }
                    } catch (e: Exception) {
                        Log.e("GetUpdates", "Bot crashed: ${e.message}. Restart from 5 sec...")
                        delay(5000)
                    }
                }
            }
        }

        return START_STICKY
    }

    private suspend fun handleMessage(message: TelegramIncomingMessage) {
        try {
            val dbMessage = message.toDbMessage(isOutgoing = false, readStatus = false)
            chatRepo.upsertChat(message.chat.toDbChat())

            try {
                message.sender?.toDbUser()?.let { userRepo.upsertUser(it) }
            } catch (e: Exception) {
                Log.e("GetUpdates", "User handle err", e)
            }

            try {
                messageRepo.insertMessage(dbMessage)
                serviceScope.launch {
                    messageRepo.ensureMediaDownloaded(dbMessage)
                }
            } catch (e: Exception) {
                Log.e("GetUpdates", "Message handle err", e)
            }

            Log.i("GetUpdates", "Success msg handle")
        } catch (e: Exception) {
            Log.e("GetUpdates", "Error handling message", e)
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

        pollingJob?.cancel()
        pollingJob = null
        gateway?.close()
        gateway = null
        currentToken = null
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "get_updates_service"
    }
}
