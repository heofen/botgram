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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.heofen.botgram.ChatType
import com.heofen.botgram.database.tables.Message
import com.heofen.botgram.ui.components.GroupScreenBar
import com.heofen.botgram.ui.components.MessageDateDivider
import com.heofen.botgram.ui.components.MessageInput
import com.heofen.botgram.ui.components.MsgBubble
import com.heofen.botgram.ui.components.MsgBubbleClusterPosition
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

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
            val messagesById = remember(uiState.messages) {
                uiState.messages.associateBy { it.messageId }
            }

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

                itemsIndexed(
                    items = uiState.messages,
                    key = { _, it -> it.chatId to it.messageId }
                ) { index, message ->
                    val olderMessage = uiState.messages.getOrNull(index + 1)
                    val newerMessage = uiState.messages.getOrNull(index - 1)
                    val isGroupedWithOlder = olderMessage?.let { shouldClusterMessages(message, it) } == true
                    val isGroupedWithNewer = newerMessage?.let { shouldClusterMessages(message, it) } == true
                    val clusterPosition = when {
                        isGroupedWithOlder && isGroupedWithNewer -> MsgBubbleClusterPosition.Middle
                        isGroupedWithOlder -> MsgBubbleClusterPosition.Bottom
                        isGroupedWithNewer -> MsgBubbleClusterPosition.Top
                        else -> MsgBubbleClusterPosition.Single
                    }

                    val senderId = message.senderId
                    val foundSender = senderId?.let { uiState.users[it] }
                    val replyToMessage = message.replyMsgId?.let { replyId ->
                        messagesById[replyId]
                    }
                    val replyToSender = replyToMessage?.senderId?.let { uiState.users[it] }
                    val showDateHeader = olderMessage == null || !isSameCalendarDay(message, olderMessage)
                    val showAvatar = !isPersonalChat && !message.isOutgoing && !isGroupedWithNewer
                    val showSenderName = !isPersonalChat && !message.isOutgoing && !isGroupedWithOlder
                    val itemSpacing = if (isGroupedWithNewer) 2.dp else 12.dp

                    if (showDateHeader) {
                        MessageDateDivider(timestamp = message.timestamp)
                    }

                    MsgBubble(
                        msg = message,
                        sender = foundSender,
                        replyToMessage = replyToMessage,
                        replySender = replyToSender,
                        isPersonalMsg = isPersonalChat,
                        showAvatar = showAvatar,
                        showSenderName = showSenderName,
                        clusterPosition = clusterPosition
                    )

                    Spacer(modifier = Modifier.height(itemSpacing))
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

private const val MESSAGE_CLUSTER_WINDOW_MS = 5 * 60 * 1000L

private fun shouldClusterMessages(current: Message, neighbour: Message): Boolean {
    if (current.isOutgoing != neighbour.isOutgoing) return false
    if (current.senderId != neighbour.senderId) return false
    if (!isSameCalendarDay(current, neighbour)) return false

    return abs(current.timestamp - neighbour.timestamp) <= MESSAGE_CLUSTER_WINDOW_MS
}

private fun isSameCalendarDay(first: Message, second: Message): Boolean =
    messageDay(first.timestamp) == messageDay(second.timestamp)

private fun messageDay(timestamp: Long): LocalDate =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
