package com.heofen.botgram.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.heofen.botgram.R
import com.heofen.botgram.data.repository.ChatRepository
import com.heofen.botgram.data.repository.MessageRepository
import com.heofen.botgram.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onChatMemberUpdated
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onEditedContentMessage
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