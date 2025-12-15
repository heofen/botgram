package com.heofen.botgram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heofen.botgram.data.repository.ChatRepository
import com.heofen.botgram.database.AppDatabase
import com.heofen.botgram.ui.screens.chatlist.ChatListScreen
import com.heofen.botgram.ui.screens.chatlist.ChatListViewModel
import com.heofen.botgram.ui.theme.BotgramTheme
import android.content.Intent
import androidx.core.content.ContextCompat
import com.heofen.botgram.services.GetUpdates

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        startUpdateService()

        val database = AppDatabase.getDatabase(applicationContext)
        val chatRepository = ChatRepository(database.chatDao())

        setContent {
            BotgramTheme {
                val viewModel: ChatListViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ChatListViewModel(chatRepository) as T
                        }
                    }
                )
                ChatListScreen(viewModel = viewModel)
            }
        }
    }

    private fun startUpdateService() {
        val intent = Intent(this, GetUpdates::class.java).apply {
            putExtra("BOT_TOKEN", "")
        }

        ContextCompat.startForegroundService(this, intent)
    }
}
