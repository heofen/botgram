package com.heofen.botgram.ui.screens.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.heofen.botgram.ChatType
import com.heofen.botgram.database.tables.User
import com.heofen.botgram.ui.components.GroupScreenBar
import com.heofen.botgram.ui.components.MsgBubble
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun GroupScreen(
    viewModel: GroupViewModel,
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val hazeState = remember { HazeState() }

    Scaffold(
        topBar = {
            uiState.chat?.let { chat ->
                GroupScreenBar(
                    chat = chat,
                    hazeState = hazeState,
                    onBackClick = onBackClick
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.chat == null -> {
                    Text(text = "Чат не найден", modifier = Modifier.align(Alignment.Center))
                }
                uiState.messages.isEmpty() -> { // Добавим проверку на пустоту
                    Text(text = "Нет сообщений", modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    val isPersonalChat = uiState.chat?.type == ChatType.PRIVATE

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding(),
                        contentPadding = innerPadding,
                        reverseLayout = true
                    ) {
                        items(
                            items = uiState.messages.reversed(),
                            key = { "${it.chatId}_${it.messageId}" }
                        ) { message ->
                            val senderId = message.senderId
                            val foundSender = senderId?.let { uiState.users[it] }

                            val displaySender = foundSender ?: User(
                                id = senderId ?: 0L,
                                firstName = "Unknown",
                                lastName = null,
                                bio = null,
                                avatarFileId = null,
                                avatarFileUniqueId = null,
                                avatarLocalPath = null
                            )

                            MsgBubble(
                                msg = message,
                                sender = displaySender,
                                isPersonalMsg = isPersonalChat
                            )
                        }
                    }
                }
            }
        }
    }
}