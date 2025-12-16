package com.heofen.botgram.ui.screens.chatlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.heofen.botgram.ui.components.ChatCell
import com.heofen.botgram.ui.components.ChatListScreenBar
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel,
    onChatClick: (Long) -> Unit
) {
    val chats by viewModel.chatListState.collectAsState(initial = emptyList())
    val hazeState = remember { HazeState() }

    Scaffold(
        topBar = {
            ChatListScreenBar(
                title = "Botgram",
                hazeState = hazeState,
                onMenuClick = { /* TODO: открыть меню */ },
                onSearchClick = { /* TODO: поиск */ }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding
            ) {
                items(chats, key = { it.id }) { chat ->
                    ChatCell(
                        chat = chat,
                        onChatSellClick = {
                            onChatClick(chat.id)
                        }
                    )
                }
            }
        }
    }
}