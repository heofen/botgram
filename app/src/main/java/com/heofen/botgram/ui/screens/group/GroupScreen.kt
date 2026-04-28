package com.heofen.botgram.ui.screens.group

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material3.*
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.*
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
import com.heofen.botgram.ui.components.AttachmentGalleryItem
import com.heofen.botgram.ui.components.AttachmentSheet
import com.heofen.botgram.ui.components.GroupScreenBar
import com.heofen.botgram.ui.components.MessageDateDivider
import com.heofen.botgram.ui.components.MessageInput
import com.heofen.botgram.ui.components.MediaGroupBubble
import com.heofen.botgram.ui.components.MsgBubble
import com.heofen.botgram.ui.components.MsgBubbleClusterPosition
import com.heofen.botgram.ui.components.rememberVideoNotePlaybackState
import com.heofen.botgram.ui.components.rememberVoiceMessagePlaybackState
import androidx.compose.ui.platform.LocalConfiguration
import com.heofen.botgram.ui.theme.BotgramTheme
import com.heofen.botgram.ui.theme.botgramBackdropSource
import com.heofen.botgram.ui.theme.rememberBotgramBackdrop
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Экран переписки: история сообщений, поле ввода и действия над сообщениями. */
@Composable
fun GroupScreen(
    viewModel: GroupViewModel,
    onBackClick: () -> Unit,
    onChatProfileClick: (Long) -> Unit,
    onUserProfileClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val backdrop = rememberBotgramBackdrop()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val voicePlaybackState = rememberVoiceMessagePlaybackState()
    val videoNotePlaybackState = rememberVideoNotePlaybackState()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val horizontalContentPadding = 12.dp
    val bubbleAvailableWidth = remember(configuration.screenWidthDp, horizontalContentPadding) {
        (configuration.screenWidthDp.dp - horizontalContentPadding * 2).coerceAtLeast(0.dp)
    }
    var actionMessage by remember { mutableStateOf<Message?>(null) }
    var deleteMessage by remember { mutableStateOf<Message?>(null) }
    var attachmentSheetVisible by remember { mutableStateOf(false) }
    var composerHeightPx by remember { mutableStateOf(0) }
    var hasMediaPermission by remember { mutableStateOf(context.hasMediaAccessPermission()) }
    val selectedReplyItem = remember(uiState.renderItems, uiState.replyToMessageId) {
        val targetId = uiState.replyToMessageId ?: return@remember null
        uiState.renderItems.firstOrNull { it.message.messageId == targetId }
    }
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        coroutineScope.launch {
            val preparedDocument = runCatching {
                withContext(Dispatchers.IO) {
                    prepareFileForUpload(
                        contentResolver = context.contentResolver,
                        cacheDir = context.cacheDir,
                        uri = uri
                    )
                }
            }.getOrNull()

            if (preparedDocument == null) {
                context.showUploadToast(R.string.document_prepare_failed)
                return@launch
            }

            val sent = viewModel.sendDocument(
                localPath = preparedDocument.localPath,
                mimeType = preparedDocument.mimeType
            )
            context.showUploadToast(
                if (sent) R.string.document_sent_success else R.string.document_send_failed
            )
        }
    }
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasMediaPermission = context.hasMediaAccessPermission()
        if (!hasMediaPermission) {
            context.showUploadToast(R.string.media_permission_denied)
        }
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.any { it.value }) {
            coroutineScope.launch {
                context.sendCurrentLocationOrNotify(viewModel)
            }
        } else {
            context.showLocationToast(R.string.location_permission_denied)
        }
    }
    val selectedReplyMessage = selectedReplyItem?.message
    val selectedReplySender = selectedReplyItem?.sender

    DisposableEffect(voicePlaybackState) {
        onDispose {
            voicePlaybackState.release()
        }
    }
    DisposableEffect(videoNotePlaybackState) {
        onDispose {
            videoNotePlaybackState.release()
        }
    }

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
                    .botgramBackdropSource(backdrop)
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
                    items = uiState.renderItems,
                    key = { _, item -> item.message.chatId to item.message.messageId }
                ) { _, item ->
                    val message = item.message
                    val isGroupedWithOlder = item.clusterPosition == MsgBubbleClusterPosition.Bottom ||
                        item.clusterPosition == MsgBubbleClusterPosition.Middle
                    val isGroupedWithNewer = item.clusterPosition == MsgBubbleClusterPosition.Top ||
                        item.clusterPosition == MsgBubbleClusterPosition.Middle
                    val showAvatar = !isPersonalChat && !message.isOutgoing && !isGroupedWithNewer
                    val showSenderName = !isPersonalChat && !message.isOutgoing && !isGroupedWithOlder
                    val itemSpacing = if (isGroupedWithOlder) 2.dp else 12.dp

                    val swipeModifier = replySwipeModifier(
                        onReply = { viewModel.selectReplyMessage(message) }
                    )
                    val avatarClick = item.sender?.id?.let { userId ->
                        { onUserProfileClick(userId) }
                    }
                    // Свайп по пузырю вправо выбирает сообщение для ответа.
                    if (item.mediaGroupMessages != null) {
                        MediaGroupBubble(
                            messages = item.mediaGroupMessages,
                            sender = item.sender,
                            availableWidth = bubbleAvailableWidth,
                            modifier = swipeModifier,
                            isPersonalMsg = isPersonalChat,
                            showAvatar = showAvatar,
                            showSenderName = showSenderName,
                            clusterPosition = item.clusterPosition,
                            onAvatarClick = avatarClick,
                            onClick = { actionMessage = message }
                        )
                    } else {
                        MsgBubble(
                            msg = message,
                            sender = item.sender,
                            availableWidth = bubbleAvailableWidth,
                            replyToMessage = item.replyToMessage,
                            replySender = item.replySender,
                            modifier = swipeModifier,
                            isPersonalMsg = isPersonalChat,
                            showAvatar = showAvatar,
                            showSenderName = showSenderName,
                            clusterPosition = item.clusterPosition,
                            voicePlaybackState = voicePlaybackState,
                            videoNotePlaybackState = videoNotePlaybackState,
                            onAvatarClick = avatarClick,
                            onClick = { actionMessage = message },
                            sendStatus = item.sendStatus
                        )
                    }

                    Spacer(modifier = Modifier.height(itemSpacing))

                    if (item.showDateHeader) {
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
                backdrop = backdrop,
                onBackClick = onBackClick,
                onAvatarClick = {
                    onChatProfileClick(chat.id)
                }
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
                backdrop = backdrop,
                replyMessage = selectedReplyMessage,
                replySender = selectedReplySender,
                pendingMedia = uiState.pendingMedia,
                onTextChange = viewModel::onMessageChange,
                onAttachmentClick = {
                    hasMediaPermission = context.hasMediaAccessPermission()
                    attachmentSheetVisible = true
                },
                onSendClick = viewModel::sendMessage,
                onRemovePendingMedia = viewModel::removePendingMedia,
                onCancelReply = viewModel::clearReplyMessage
            )
        }

        if (attachmentSheetVisible) {
            AttachmentSheet(
                backdrop = backdrop,
                hasMediaPermission = hasMediaPermission,
                onDismissRequest = { attachmentSheetVisible = false },
                onGrantMediaPermissionClick = {
                    mediaPermissionLauncher.launch(context.requiredMediaPermissions())
                },
                onMediaClick = { item ->
                    attachmentSheetVisible = false
                    coroutineScope.launch {
                        context.addPendingMediaFromGalleryItem(
                            item = item,
                            viewModel = viewModel
                        )
                    }
                },
                onFileClick = {
                    attachmentSheetVisible = false
                    documentPickerLauncher.launch(arrayOf("*/*"))
                },
                onLocationClick = {
                    attachmentSheetVisible = false
                    if (context.hasAnyLocationPermission()) {
                        coroutineScope.launch {
                            context.sendCurrentLocationOrNotify(viewModel)
                        }
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }
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

/** Проверяет, выданы ли приложению какие-либо разрешения на определение местоположения. */
private fun Context.hasAnyLocationPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

/** Показывает короткий toast с результатом работы геолокации. */
private fun Context.showLocationToast(messageResId: Int) {
    Toast.makeText(this, getString(messageResId), Toast.LENGTH_SHORT).show()
}

/** Показывает короткий toast для операций с файлами. */
private fun Context.showUploadToast(messageResId: Int) {
    Toast.makeText(this, getString(messageResId), Toast.LENGTH_SHORT).show()
}

/** Подготавливает файл из галереи и добавляет его в pending-список композера. */
private suspend fun Context.addPendingMediaFromGalleryItem(
    item: AttachmentGalleryItem,
    viewModel: GroupViewModel
) {
    val preparedMedia = runCatching {
        withContext(Dispatchers.IO) {
            prepareVisualMediaForUpload(
                contentResolver = contentResolver,
                cacheDir = cacheDir,
                uri = item.contentUri
            )
        }
    }.getOrNull()

    if (preparedMedia == null) {
        showUploadToast(R.string.media_prepare_failed)
        return
    }

    viewModel.addPendingMedia(
        listOf(
            ComposerMediaItem(
                localPath = preparedMedia.localPath,
                mimeType = preparedMedia.mimeType,
                fileName = preparedMedia.fileName
            )
        )
    )
}

/** Разрешает геолокацию, отправляет координаты и сообщает пользователю результат. */
private suspend fun Context.sendCurrentLocationOrNotify(viewModel: GroupViewModel) {
    val location = resolveCurrentLocation()
    if (location == null) {
        showLocationToast(R.string.location_unavailable)
        return
    }

    Log.d(
        "GroupScreen",
        "Resolved location lat=${location.latitude}, lon=${location.longitude}"
    )
    showLocationToast(R.string.location_resolved_sending)

    val sent = viewModel.sendLocation(
        latitude = location.latitude,
        longitude = location.longitude
    )
    if (sent) {
        showLocationToast(R.string.location_sent_success)
    } else {
        showLocationToast(R.string.location_send_failed)
    }
}

/** Возвращает runtime-permissions, достаточные для чтения фото и видео из MediaStore. */
private fun Context.requiredMediaPermissions(): Array<String> {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )

        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

/** Проверяет, может ли приложение показать локальную галерею внутри шторки вложений. */
private fun Context.hasMediaAccessPermission(): Boolean {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ) == PackageManager.PERMISSION_GRANTED
        }

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
        }

        else -> {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}

/** Пытается получить наиболее актуальную геолокацию пользователя. */
@SuppressLint("MissingPermission")
private suspend fun Context.resolveCurrentLocation(): Location? {
    val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = buildList<String> {
        if (runCatching { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)) {
            add(LocationManager.GPS_PROVIDER)
        }
        if (runCatching { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)) {
            add(LocationManager.NETWORK_PROVIDER)
        }
    }

    if (providers.isEmpty()) {
        return null
    }

    providers.asSequence()
        .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time }
        ?.let { return it }

    providers.forEach { provider ->
        withTimeoutOrNull(4_000L) {
            locationManager.awaitCurrentLocationCompat(
                provider = provider,
                context = this@resolveCurrentLocation
            )
        }?.let { return it }
    }

    return null
}

/** Совместимый способ получить текущее местоположение через `LocationManagerCompat`. */
@SuppressLint("MissingPermission")
private suspend fun LocationManager.awaitCurrentLocationCompat(
    provider: String,
    context: Context
): Location? = suspendCancellableCoroutine { continuation ->
    val cancellationSignal = CancellationSignal()
    continuation.invokeOnCancellation { cancellationSignal.cancel() }

    runCatching {
        LocationManagerCompat.getCurrentLocation(
            this,
            provider,
            cancellationSignal,
            context.mainExecutor
        ) { location ->
            if (continuation.isActive) {
                continuation.resume(location)
            }
        }
    }.onFailure {
        if (continuation.isActive) {
            continuation.resume(null)
        }
    }
}

/** Локальная модель подготовленного файла для отправки фото или видео. */
private data class PreparedVisualMedia(
    val localPath: String,
    val mimeType: String,
    val fileName: String
)

/** Локальная модель подготовленного файла для отправки документом. */
private data class PreparedUploadFile(
    val localPath: String,
    val mimeType: String
)

/** Копирует выбранный медиафайл во внутренний cache, чтобы безопасно отправить его позже. */
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

/** Копирует выбранный файл во внутренний cache, чтобы безопасно отправить его позже. */
private fun prepareFileForUpload(
    contentResolver: ContentResolver,
    cacheDir: File,
    uri: Uri
): PreparedUploadFile? {
    val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
    val displayName = contentResolver.queryDisplayName(uri)
    val extension = displayName
        ?.substringAfterLast('.', "")
        ?.takeIf { it.isNotBlank() }
        ?: mimeType.substringAfter('/', "").substringBefore(';').takeIf { it.isNotBlank() }
        ?: "bin"
    val safeBaseName = displayName
        ?.substringBeforeLast('.', displayName)
        ?.replace(Regex("""[\\/:*?"<>|]"""), "_")
        ?.takeIf { it.isNotBlank() }
        ?: "file"
    val uploadsDir = File(cacheDir, "uploads").apply { mkdirs() }
    val targetFile = File(
        uploadsDir,
        "${safeBaseName}_${System.nanoTime()}.${extension}"
    )

    contentResolver.openInputStream(uri)?.use { input ->
        targetFile.outputStream().use { output ->
            input.copyTo(output)
        }
    } ?: throw IOException("Unable to open selected file")

    return PreparedUploadFile(
        localPath = targetFile.absolutePath,
        mimeType = mimeType
    )
}

/** Возвращает отображаемое имя файла для выбранного URI. */
private fun ContentResolver.queryDisplayName(uri: Uri): String? {
    return query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        cursor.getStringOrNull(OpenableColumns.DISPLAY_NAME)
    }
}

/** Безопасно читает строковое значение колонки курсора. */
private fun Cursor.getStringOrNull(columnName: String): String? {
    val index = getColumnIndex(columnName)
    if (index == -1 || isNull(index)) return null
    return getString(index)
}

/** Добавляет модификатор свайпа вправо для выбора ответа на сообщение. */
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

/** Нижняя шторка с действиями над выбранным сообщением. */
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

/** Диалог удаления сообщения с выбором локального или серверного удаления. */
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
