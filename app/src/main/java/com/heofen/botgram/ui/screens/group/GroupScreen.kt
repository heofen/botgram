package com.heofen.botgram.ui.screens.group

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heofen.botgram.ChatType
import com.heofen.botgram.R
import com.heofen.botgram.database.tables.Message
import com.heofen.botgram.ui.components.GroupScreenBar
import com.heofen.botgram.ui.components.MessageDateDivider
import com.heofen.botgram.ui.components.MessageInput
import com.heofen.botgram.ui.components.MsgBubble
import com.heofen.botgram.ui.components.MsgBubbleClusterPosition
import com.heofen.botgram.ui.theme.BotgramTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.io.File
import java.io.IOException
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GroupScreen(viewModel: GroupViewModel, onBackClick: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val hazeState = remember { HazeState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val horizontalContentPadding = 12.dp
    var actionMessage by remember { mutableStateOf<Message?>(null) }
    var deleteMessage by remember { mutableStateOf<Message?>(null) }
    var composerHeightPx by remember { mutableStateOf(0) }
    val messagesById = remember(uiState.messages) {
        uiState.messages.associateBy { it.messageId }
    }
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult

        coroutineScope.launch {
            val preparedMedia = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    prepareVisualMediaForUpload(
                        contentResolver = context.contentResolver,
                        cacheDir = context.cacheDir,
                        uri = uri
                    )
                }
            }
            viewModel.addPendingMedia(
                preparedMedia.map {
                    ComposerMediaItem(
                        localPath = it.localPath,
                        mimeType = it.mimeType,
                        fileName = it.fileName
                    )
                }
            )
        }
    }
    val selectedReplyMessage = uiState.replyToMessageId?.let(messagesById::get)
    val selectedReplySender = selectedReplyMessage?.senderId?.let { uiState.users[it] }

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
            val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()
            val topContentPadding = statusBarPadding.calculateTopPadding() + 64.dp
            val composerHeight = with(density) { composerHeightPx.toDp() }
            val bottomInputPadding = maxOf(
                70.dp,
                composerHeight + navigationBarPadding.calculateBottomPadding() + 16.dp
            )

            LazyColumn(
                modifier = Modifier
                    .hazeSource(state = hazeState)
                    .imePadding()
                    .fillMaxSize(),
                reverseLayout = true,
                contentPadding = PaddingValues(
                    top = topContentPadding + 8.dp,
                    bottom = bottomInputPadding + 36.dp,
                    start = statusBarPadding.calculateStartPadding(layoutDirection) + horizontalContentPadding,
                    end = statusBarPadding.calculateEndPadding(layoutDirection) + horizontalContentPadding
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

                    MsgBubble(
                        msg = message,
                        sender = foundSender,
                        replyToMessage = replyToMessage,
                        replySender = replyToSender,
                        modifier = replySwipeModifier(
                            onReply = { viewModel.selectReplyMessage(message) }
                        ),
                        isPersonalMsg = isPersonalChat,
                        showAvatar = showAvatar,
                        showSenderName = showSenderName,
                        clusterPosition = clusterPosition,
                        onClick = { actionMessage = message }
                    )

                    Spacer(modifier = Modifier.height(itemSpacing))

                    if (showDateHeader) {
                        MessageDateDivider(timestamp = message.timestamp)
                    }
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
                .padding(bottom = 16.dp, start = horizontalContentPadding, end = horizontalContentPadding)
        ) {
            MessageInput(
                modifier = Modifier.onSizeChanged { composerHeightPx = it.height },
                text = uiState.messageText,
                hazeState = hazeState,
                replyMessage = selectedReplyMessage,
                replySender = selectedReplySender,
                pendingMedia = uiState.pendingMedia,
                onTextChange = viewModel::onMessageChange,
                onAttachClick = {
                    mediaPickerLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageAndVideo
                        )
                    )
                },
                onSendClick = viewModel::sendMessage,
                onRemovePendingMedia = viewModel::removePendingMedia,
                onCancelReply = viewModel::clearReplyMessage
            )
        }

        if (actionMessage != null) {
            MessageActionsSheet(
                onDismissRequest = { actionMessage = null },
                onDeleteClick = {
                    deleteMessage = actionMessage
                    actionMessage = null
                }
            )
        }

        if (deleteMessage != null) {
            DeleteMessageDialog(
                onDismissRequest = { deleteMessage = null },
                onDeleteForMe = {
                    deleteMessage?.let(viewModel::deleteMessageForMe)
                    deleteMessage = null
                },
                onDeleteForEveryone = {
                    deleteMessage?.let(viewModel::deleteMessageForEveryone)
                    deleteMessage = null
                }
            )
        }
    }
}

private data class PreparedVisualMedia(
    val localPath: String,
    val mimeType: String,
    val fileName: String
)

private fun prepareVisualMediaForUpload(
    contentResolver: ContentResolver,
    cacheDir: File,
    uri: Uri
): PreparedVisualMedia? {
    val mimeType = contentResolver.getType(uri)?.takeIf {
        it.startsWith("image/") || it.startsWith("video/")
    } ?: return null
    val displayName = contentResolver.queryDisplayName(uri)
    val extension = displayName
        ?.substringAfterLast('.', "")
        ?.takeIf { it.isNotBlank() }
        ?: mimeType.substringAfter('/', "").substringBefore(';')
    val uploadsDir = File(cacheDir, "uploads").apply { mkdirs() }
    val targetFile = File(
        uploadsDir,
        "upload_${System.nanoTime()}.${extension.ifBlank { "bin" }}"
    )

    contentResolver.openInputStream(uri)?.use { input ->
        targetFile.outputStream().use { output ->
            input.copyTo(output)
        }
    } ?: throw IOException("Unable to open selected media")

    return PreparedVisualMedia(
        localPath = targetFile.absolutePath,
        mimeType = mimeType,
        fileName = displayName ?: targetFile.name
    )
}

private fun ContentResolver.queryDisplayName(uri: Uri): String? {
    return query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        cursor.getStringOrNull(OpenableColumns.DISPLAY_NAME)
    }
}

private fun Cursor.getStringOrNull(columnName: String): String? {
    val index = getColumnIndex(columnName)
    if (index == -1 || isNull(index)) return null
    return getString(index)
}

@Composable
private fun replySwipeModifier(onReply: () -> Unit): Modifier {
    val density = LocalDensity.current
    val triggerDistancePx = with(density) { 72.dp.toPx() }
    val maxOffsetPx = with(density) { 96.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    return Modifier
        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
        .pointerInput(onReply) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { change, dragAmount ->
                    val nextOffset = (offsetX.value + dragAmount).coerceIn(0f, maxOffsetPx)
                    change.consume()
                    coroutineScope.launch {
                        offsetX.snapTo(nextOffset)
                    }
                },
                onDragEnd = {
                    val shouldReply = offsetX.value >= triggerDistancePx
                    coroutineScope.launch {
                        offsetX.animateTo(0f)
                    }
                    if (shouldReply) {
                        onReply()
                    }
                },
                onDragCancel = {
                    coroutineScope.launch {
                        offsetX.animateTo(0f)
                    }
                }
            )
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageActionsSheet(
    onDismissRequest: () -> Unit,
    onDeleteClick: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Text(
            text = stringResource(R.string.message_actions_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        ListItem(
            headlineContent = {
                Text(text = stringResource(R.string.message_action_delete))
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onDeleteClick)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DeleteMessageDialog(
    onDismissRequest: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(R.string.message_delete_title))
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.0f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.message_delete_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = onDeleteForMe,
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Text(
                                text = stringResource(R.string.message_delete_for_me),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        TextButton(
                            onClick = onDeleteForEveryone,
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Text(
                                text = stringResource(R.string.message_delete_for_everyone),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    TextButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Text(
                            text = stringResource(R.string.message_delete_cancel),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        dismissButton = {},
        confirmButton = {}
    )
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

@Preview(showBackground = true, backgroundColor = 0xFFF3F5F7)
@Composable
private fun MessageActionsSheetPreview() {
    BotgramTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            MessageActionsSheet(
                onDismissRequest = {},
                onDeleteClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F5F7)
@Composable
private fun DeleteMessageDialogPreview() {
    BotgramTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            DeleteMessageDialog(
                onDismissRequest = {},
                onDeleteForMe = {},
                onDeleteForEveryone = {}
            )
        }
    }
}
