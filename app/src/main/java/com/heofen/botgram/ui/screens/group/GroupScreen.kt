package com.heofen.botgram.ui.screens.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.heofen.botgram.ChatType
import com.heofen.botgram.database.tables.User
import com.heofen.botgram.ui.components.GroupScreenBar
import com.heofen.botgram.ui.components.MessageInput
import com.heofen.botgram.ui.components.MsgBubble
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun GroupScreen(viewModel: GroupViewModel, onBackClick: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val hazeState = remember { HazeState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            val isPersonalChat = uiState.chat?.type == ChatType.PRIVATE
            val layoutDirection = LocalLayoutDirection.current

            val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
            val topContentPadding = statusBarPadding.calculateTopPadding() + 64.dp

            val bottomInputPadding = 70.dp

            LazyColumn(
                modifier = Modifier
                    .hazeSource(state = hazeState)
                    .imePadding()
                    .fillMaxSize(),
                reverseLayout = true,
                contentPadding = PaddingValues(
                    top = topContentPadding + 8.dp,
                    bottom = bottomInputPadding + 36.dp ,
                    start = statusBarPadding.calculateStartPadding(layoutDirection),
                    end = statusBarPadding.calculateEndPadding(layoutDirection)
                )
            ) {

                items(
                    items = uiState.messages.reversed(),
                    key = { it.chatId to it.messageId }
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

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(
                modifier = Modifier
                    .height(8.dp)
            )
        }

        uiState.chat?.let { chat ->
            GroupScreenBar(
                chat = chat,
                hazeState = hazeState,
                onBackClick = onBackClick
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 16.dp, start = 8.dp, end = 8.dp)
        ) {
            MessageInput(
                text = uiState.messageText,
                hazeState = hazeState,
                onTextChange = viewModel::onMessageChange,
                onSendClick = viewModel::sendMessage
            )
        }
    }
}