package com.heofen.botgram.ui.components

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GifBox
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.heofen.botgram.ChatType
import com.heofen.botgram.MessageType
import com.heofen.botgram.database.tables.Chat
import com.heofen.botgram.database.tables.ChatListItem
import com.heofen.botgram.database.tables.Message
import com.heofen.botgram.ui.theme.BotgramTheme
import com.heofen.botgram.ui.theme.botgramHazeStyle
import com.heofen.botgram.utils.extensions.getInitials
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ChatAvatar(
    chat: Chat,
    modifier: Modifier = Modifier
) {
    val model = if (chat.avatarLocalPath != null) File(chat.avatarLocalPath) else null

    if (model != null && model.exists()) {
        AsyncImage(
            model = model,
            contentDescription = "Chat Avatar",
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = chat.getInitials(),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ChatListScreenBar(
    title: String,
    hazeState: HazeState,
    isSearchActive: Boolean = false,
    searchQuery: String = "",
    onQueryChange: (String) -> Unit = {},
    onSearchToggle: (Boolean) -> Unit = {},
    onMenuClick: () -> Unit = {},
) {
    val islandStyle = botgramHazeStyle()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .hazeEffect(state = hazeState, style = islandStyle)
                    .clickable {
                        if (isSearchActive) onSearchToggle(false) else onMenuClick()
                    }
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(50.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSearchActive) Icons.Default.ArrowBackIosNew else Icons.Default.Menu,
                    contentDescription = "Menu",
                    modifier = Modifier.size(if (isSearchActive) 24.dp else 30.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .then(if (isSearchActive) Modifier.fillMaxWidth() else Modifier.wrapContentWidth())
                        .clip(RoundedCornerShape(50))
                        .hazeEffect(state = hazeState, style = islandStyle)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(50.dp)
                        )
                        .clickable(enabled = !isSearchActive) {
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = if (isSearchActive) Alignment.CenterStart else Alignment.Center
                ) {
                    if (isSearchActive) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Start
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "Поиск...",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    } else {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .hazeEffect(state = hazeState, style = islandStyle)
                    .clickable {
                        if (isSearchActive) {
                            if (searchQuery.isNotEmpty()) onQueryChange("") else onSearchToggle(false)
                        } else {
                            onSearchToggle(true)
                        }
                    }
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(50.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSearchActive && searchQuery.isNotEmpty()) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ChatCell(
    item: ChatListItem,
    unreadedCount: Int = 0,
    onChatSellClick: (Long) -> Unit = {}
) {
    val chat = item.chat
    val lastMessage = item.lastMessage

    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp)
                )
                .fillMaxWidth()
                .clickable { onChatSellClick(chat.id) }
                .padding(8.dp)
        ) {
            ChatAvatar(
                chat = chat,
                modifier = Modifier.size(50.dp)
            )

            Column(
                modifier = Modifier
                    .padding(
                        horizontal = 8.dp,
                        vertical = 2.dp
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val title: String = when {
                        chat.title != null -> chat.title
                        else -> (chat.firstName ?: "") + " " + (chat.lastName ?: "")
                    }
                    Text(
                        text = title,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    val instant = Instant.ofEpochMilli(chat.lastMessageTime ?: 0)
                    val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())

                    val formatter = DateTimeFormatter.ofPattern("HH:mm")
                    val formattedDate = dateTime.format(formatter)

                    Text(
                        text = formattedDate,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (lastMessage != null && lastMessage.type.isInlinePreviewType()) {
                        InlineMediaPreview(
                            message = lastMessage,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .weight(1f)
                        )
                    } else if (chat.lastMessageType == MessageType.TEXT) {
                        val preview = chat.lastMessageText
                            ?: "⚠\uFE0F Тут явно что-то пошло не по плану"

                        Text(
                            text = preview,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Light,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        val preview: String = when (chat.lastMessageType) {
                            MessageType.PHOTO -> "Photo"
                            MessageType.VIDEO -> "Video"
                            MessageType.ANIMATION -> "GIF"
                            MessageType.AUDIO -> "Audio"
                            MessageType.VOICE -> "Voice message"
                            MessageType.VIDEO_NOTE -> "Video message"
                            MessageType.DOCUMENT -> "Document"
                            MessageType.STICKER -> "Sticker"
                            MessageType.ANIMATED_STICKER -> "Sticker"
                            MessageType.VIDEO_STICKER -> "Sticker"
                            MessageType.CONTACT -> "Contact"
                            MessageType.LOCATION -> "Location"
                            else -> "⚠\uFE0F Тут явно что-то пошло не по плану"
                        }
                        Text(
                            text = preview,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Light,
                            color = Color.Blue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineMediaPreview(
    message: Message,
    modifier: Modifier = Modifier
) {
    val file = message.fileLocalPath?.let(::File)
    val renderMode = remember(message.type, file?.path) {
        resolveAnimatedPreviewMode(message.type, file)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.32f))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (renderMode) {
            ChatInlinePreviewMode.GIF -> GifPreview(file)
            ChatInlinePreviewMode.VIDEO -> VideoPreview(file, forceAutoplay = true)
            ChatInlinePreviewMode.NONE -> Unit
        }

        val label = if (message.type == MessageType.ANIMATION) "GIF" else "Video"
        val previewText = message.caption?.takeIf { it.isNotBlank() }
            ?: message.fileName?.takeIf { it.isNotBlank() }
            ?: label

        Column(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = previewText,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun GifPreview(file: File?) {
    val context = LocalContext.current
    val inspectionMode = LocalInspectionMode.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    Box(
        modifier = Modifier
            .size(width = 88.dp, height = 60.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!inspectionMode && file != null && file.exists()) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(file)
                        .allowHardware(false)
                        .crossfade(true)
                        .build(),
                    imageLoader = imageLoader
                ),
                contentDescription = "GIF preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.GifBox,
                contentDescription = "GIF preview",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun VideoPreview(
    file: File?,
    forceAutoplay: Boolean = false
) {
    val inspectionMode = LocalInspectionMode.current
    val existingFile = file?.takeIf(File::exists)
    val shouldAutoplay = remember(existingFile?.absolutePath) {
        existingFile?.let { forceAutoplay || !videoHasAudio(it) } ?: false
    }

    Box(
        modifier = Modifier
            .size(width = 88.dp, height = 60.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!inspectionMode && existingFile != null) {
            InlineVideoPlayer(
                file = existingFile,
                autoplay = shouldAutoplay
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.18f))
        )
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Video preview",
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun InlineVideoPlayer(
    file: File,
    autoplay: Boolean
) {
    val context = LocalContext.current
    val fileUri = remember(file.absolutePath) { Uri.fromFile(file) }
    val exoPlayer = remember(file.absolutePath, autoplay) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            playWhenReady = autoplay
            setMediaItem(MediaItem.fromUri(fileUri))
            prepare()
        }
    }

    LaunchedEffect(exoPlayer, fileUri, autoplay) {
        exoPlayer.setMediaItem(MediaItem.fromUri(fileUri))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = autoplay
        if (autoplay) {
            exoPlayer.play()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (autoplay && playbackState == Player.STATE_READY) {
                    exoPlayer.playWhenReady = true
                    exoPlayer.play()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { viewContext ->
            TextureView(viewContext).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { textureView ->
            exoPlayer.setVideoTextureView(textureView)
        }
    )

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.clearVideoSurface()
        }
    }
}

private fun MessageType?.isInlinePreviewType(): Boolean =
    this == MessageType.VIDEO || this == MessageType.ANIMATION

private enum class ChatInlinePreviewMode {
    GIF,
    VIDEO,
    NONE
}

private fun resolveAnimatedPreviewMode(
    type: MessageType,
    file: File?
): ChatInlinePreviewMode {
    val extension = file?.extension?.lowercase()
    return when {
        type == MessageType.VIDEO -> ChatInlinePreviewMode.VIDEO
        type == MessageType.ANIMATION && extension in setOf("gif", "webp") -> ChatInlinePreviewMode.GIF
        type == MessageType.ANIMATION && file != null -> ChatInlinePreviewMode.VIDEO
        else -> ChatInlinePreviewMode.NONE
    }
}

private fun videoHasAudio(file: File): Boolean {
    val retriever = MediaMetadataRetriever()
    return runCatching {
        retriever.setDataSource(file.absolutePath)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"
    }.getOrDefault(false).also {
        retriever.release()
    }
}

@Composable
@Preview
fun ChatCellPreview() {
    BotgramTheme() {
        val item = ChatListItem(
            chat = Chat(
                id = 1,
                type = ChatType.PRIVATE,
                title = null,
                firstName = "Артем",
                lastName = "Крупный_калибр",
                username = null,
                lastMessageType = MessageType.TEXT,
                lastMessageText = "Как дела? Давно не виделись",
                lastMessageTime = System.currentTimeMillis(),
                lastMessageSenderId = null,
                avatarFileId = null,
                avatarFileUniqueId = null,
                avatarLocalPath = null
            ),
            lastMessage = null
        )
        ChatCell(item)
    }
}
