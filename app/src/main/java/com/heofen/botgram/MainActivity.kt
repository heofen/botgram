package com.heofen.botgram

import android.content.Intent
import android.os.Bundle
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
import com.heofen.botgram.data.repository.ChatRepository
import com.heofen.botgram.data.repository.MessageRepository
import com.heofen.botgram.data.repository.UserRepository
import com.heofen.botgram.database.AppDatabase
import com.heofen.botgram.services.GetUpdates
import com.heofen.botgram.ui.screens.chatlist.ChatListScreen
import com.heofen.botgram.ui.screens.chatlist.ChatListViewModel
import com.heofen.botgram.ui.screens.group.GroupScreen
import com.heofen.botgram.ui.screens.group.GroupViewModel
import com.heofen.botgram.ui.theme.BotgramTheme
import dev.inmo.tgbotapi.bot.ktor.KtorRequestsExecutor
import dev.inmo.tgbotapi.utils.TelegramAPIUrlsKeeper
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

class MainActivity : ComponentActivity() {

    companion object {
        const val BOT_TOKEN = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        startUpdateService()

        val database = AppDatabase.getDatabase(applicationContext)

        val httpClient = HttpClient(OkHttp)
        val bot = KtorRequestsExecutor(TelegramAPIUrlsKeeper(BOT_TOKEN), httpClient)
        val mediaManager = MediaManager(applicationContext, bot)

        val chatRepository = ChatRepository(database.chatDao(), mediaManager)
        val messageRepository = MessageRepository(database.messageDao())

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
        val intent = Intent(this, GetUpdates::class.java).apply {
            putExtra("BOT_TOKEN", BOT_TOKEN)
        }
        ContextCompat.startForegroundService(this, intent)
    }
}