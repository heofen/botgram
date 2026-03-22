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
import com.heofen.botgram.ui.screens.profile.ProfileScreen
import com.heofen.botgram.ui.screens.profile.ProfileTarget
import com.heofen.botgram.ui.screens.profile.ProfileViewModel
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
                onBackClick = { navController.popBackStack() },
                onChatProfileClick = { profileChatId ->
                    navController.navigate("profile/chat/$profileChatId")
                },
                onUserProfileClick = { userId ->
                    navController.navigate("profile/user/$userId")
                }
            )
        }

        composable(
            route = "profile/chat/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.LongType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getLong("chatId") ?: return@composable
            val viewModel: ProfileViewModel = koinViewModel(
                parameters = { parametersOf(ProfileTarget.CHAT, chatId) }
            )

            ProfileScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = "profile/user/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.LongType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getLong("userId") ?: return@composable
            val viewModel: ProfileViewModel = koinViewModel(
                parameters = { parametersOf(ProfileTarget.USER, userId) }
            )

            ProfileScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
