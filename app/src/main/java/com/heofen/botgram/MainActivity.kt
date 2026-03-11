package com.heofen.botgram

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.heofen.botgram.data.MediaManager
import com.heofen.botgram.data.local.TokenManager
import com.heofen.botgram.data.remote.TelegramGateway
import com.heofen.botgram.data.remote.TelegramGatewayFactory
import com.heofen.botgram.data.repository.ChatRepository
import com.heofen.botgram.data.repository.MessageRepository
import com.heofen.botgram.data.repository.UserRepository
import com.heofen.botgram.database.AppDatabase
import com.heofen.botgram.services.GetUpdates
import com.heofen.botgram.ui.screens.chatlist.ChatListScreen
import com.heofen.botgram.ui.screens.chatlist.ChatListViewModel
import com.heofen.botgram.ui.screens.group.GroupScreen
import com.heofen.botgram.ui.screens.group.GroupViewModel
import com.heofen.botgram.ui.screens.login.LoginScreen
import com.heofen.botgram.ui.theme.BotgramTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private var gateway: TelegramGateway? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tokenManager = TokenManager(applicationContext)
        val token = tokenManager.getToken()

        if (token.isNullOrBlank()) {
            setContent {
                LoginScreen(
                    onCheckToken = { inputToken ->
                        withContext(Dispatchers.IO) {
                            try {
                                val client = OkHttpClient()
                                val request = Request.Builder()
                                    .url("https://api.telegram.org/bot$inputToken/getMe")
                                    .build()

                                client.newCall(request).execute().use { response ->
                                    if (!response.isSuccessful) return@withContext false

                                    val responseBody = response.body?.string() ?: return@withContext false
                                    val json = JSONObject(responseBody)
                                    val isOk = json.optBoolean("ok", false)

                                    if (isOk) {
                                        tokenManager.saveToken(inputToken)
                                        Log.i("Login", "Token is valid. Saved.")
                                        true
                                    } else {
                                        Log.w("Login", "Telegram API returned ok: false")
                                        false
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("Login", "Network error: ${e.message}")
                                false
                            }
                        }
                    },
                    onLoginSuccess = {
                        val restartIntent = intent
                        finish()
                        startActivity(restartIntent)
                    }
                )
            }
            return
        }

        startUpdateService()

        val database = AppDatabase.getDatabase(applicationContext)
        gateway = TelegramGatewayFactory.create(applicationContext, token)
        val activeGateway = requireNotNull(gateway)
        val mediaManager = MediaManager(activeGateway)

        val chatRepository = ChatRepository(database.chatDao(), mediaManager)
        val messageRepository = MessageRepository(database.messageDao(), activeGateway, mediaManager)
        val userRepository = UserRepository(database.userDao(), mediaManager)

        setContent {
            BotgramTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "chat_list"
                ) {
                    composable("chat_list") {
                        val viewModel: ChatListViewModel = viewModel(
                            factory = object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    return ChatListViewModel(chatRepository) as T
                                }
                            }
                        )

                        ChatListScreen(
                            viewModel = viewModel,
                            onChatClick = { chatId ->
                                navController.navigate("group/$chatId")
                            },
                            onLogOut = {
                                tokenManager.clearToken()
                                val stopIntent = Intent(applicationContext, GetUpdates::class.java)
                                stopService(stopIntent)
                                finish()
                                startActivity(intent)
                            }
                        )
                    }

                    composable(
                        route = "group/{chatId}",
                        arguments = listOf(navArgument("chatId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val chatId = backStackEntry.arguments?.getLong("chatId") ?: return@composable

                        val groupViewModel: GroupViewModel = viewModel(
                            factory = object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    return GroupViewModel(
                                        chatId = chatId,
                                        chatRepository = chatRepository,
                                        messageRepository = messageRepository,
                                        userRepository = userRepository
                                    ) as T
                                }
                            }
                        )

                        GroupScreen(
                            viewModel = groupViewModel,
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun startUpdateService() {
        val intent = Intent(this, GetUpdates::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    override fun onDestroy() {
        gateway?.close()
        gateway = null
        super.onDestroy()
    }
}
