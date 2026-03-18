package com.heofen.botgram.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.heofen.botgram.ui.screens.chatlist.ChatListScreen
import com.heofen.botgram.ui.screens.chatlist.ChatListViewModel
import com.heofen.botgram.ui.screens.group.GroupScreen
import com.heofen.botgram.ui.screens.group.GroupViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/** Центральный Compose-граф навигации приложения. */
@Composable
fun BotgramNavHost(onLogOut: () -> Unit) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "chat_list"
    ) {
        composable("chat_list") {
            val viewModel: ChatListViewModel = koinViewModel()

            ChatListScreen(
                viewModel = viewModel,
                onChatClick = { chatId ->
                    navController.navigate("group/$chatId")
                },
                onLogOut = onLogOut
            )
        }

        composable(
            route = "group/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.LongType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getLong("chatId") ?: return@composable
            val viewModel: GroupViewModel = koinViewModel(
                parameters = { parametersOf(chatId) }
            )

            GroupScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
