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
import com.heofen.botgram.data.local.TokenManager
import com.heofen.botgram.data.remote.TelegramUpdate
import com.heofen.botgram.data.sync.TelegramUpdateProcessor
import com.heofen.botgram.data.repository.ChatRepository
import com.heofen.botgram.data.repository.MessageRepository
import com.heofen.botgram.data.repository.UserRepository
import com.heofen.botgram.di.SessionContainer
import com.heofen.botgram.di.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class GetUpdates : Service() {
    private val tokenManager: TokenManager by inject()
    private val sessionManager: SessionManager by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var messageRepo: MessageRepository
    private lateinit var chatRepo: ChatRepository
    private lateinit var userRepo: UserRepository
    private lateinit var updateProcessor: TelegramUpdateProcessor
    private var session: SessionContainer? = null
    private var pollingJob: Job? = null
    private var currentToken: String? = null

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)

        val token = tokenManager.getToken()

        if (token.isNullOrBlank()) {
            Log.e("GetUpdates", "Token not found. Stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }

        if (currentToken != token) {
            pollingJob?.cancel()
            pollingJob = null

            session = sessionManager.currentSessionForToken(token)
            currentToken = token
        }

        val activeSession = session ?: sessionManager.currentSession()
        if (activeSession == null) {
            Log.e("GetUpdates", "Session dependencies are not initialized. Stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }

        session = activeSession
        messageRepo = activeSession.messageRepository
        chatRepo = activeSession.chatRepository
        userRepo = activeSession.userRepository
        updateProcessor = TelegramUpdateProcessor(
            chatStore = chatRepo,
            messageStore = messageRepo,
            userStore = userRepo
        )

        if (pollingJob?.isActive != true) {
            pollingJob = serviceScope.launch {
                while (isActive) {
                    try {
                        Log.i("GetUpdates", "Launching Long Polling...")
                        activeSession.gateway.collectUpdates { update ->
                            handleUpdate(update)
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

    private suspend fun handleUpdate(update: TelegramUpdate) {
        try {
            val storedMessage = updateProcessor.process(update)
            if (storedMessage != null) {
                serviceScope.launch {
                    messageRepo.ensureMediaDownloaded(storedMessage)
                }
            }
            Log.i("GetUpdates", "Success update handle")
        } catch (e: Exception) {
            Log.e("GetUpdates", "Error handling update", e)
            throw e
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
            .setSmallIcon(R.drawable.app_ic_foreground)
            .setOngoing(true)
            .setShowWhen(false)
            .setSound(null)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()

        pollingJob?.cancel()
        pollingJob = null
        session = null
        currentToken = null
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "get_updates_service"
    }
}
