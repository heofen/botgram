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
import com.heofen.botgram.di.appContainer
import com.heofen.botgram.services.GetUpdates
import com.heofen.botgram.ui.screens.chatlist.ChatListScreen
import com.heofen.botgram.ui.screens.chatlist.ChatListViewModel
import com.heofen.botgram.ui.screens.group.GroupScreen
import com.heofen.botgram.ui.screens.group.GroupViewModel
import com.heofen.botgram.ui.screens.login.LoginScreen
import com.heofen.botgram.ui.theme.BotgramTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = applicationContext.appContainer
        val tokenManager = appContainer.tokenManager
        val token = tokenManager.getToken()

        if (token.isNullOrBlank()) {
            setContent {
                LoginScreen(
                    onCheckToken = { inputToken ->
                        withContext(Dispatchers.IO) {
                            try {
                                val isValid = appContainer.tokenValidator.validate(inputToken)
                                if (isValid) {
                                    tokenManager.saveToken(inputToken)
                                    appContainer.clearSession()
                                    Log.i("Login", "Token is valid. Saved.")
                                } else {
                                    Log.w("Login", "Telegram API returned invalid token")
                                }
                                isValid
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

        val session = requireNotNull(appContainer.currentSession()) {
            "Session dependencies are unavailable without a saved token."
        }
        val chatRepository = session.chatRepository
        val messageRepository = session.messageRepository
        val userRepository = session.userRepository

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
                                    return ChatListViewModel(chatRepository, messageRepository) as T
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
                                appContainer.clearSession()
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
}
