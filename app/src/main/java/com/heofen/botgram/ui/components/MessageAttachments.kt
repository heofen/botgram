package com.heofen.botgram.ui.components

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.heofen.botgram.MessageType
import com.heofen.botgram.database.tables.Message
import java.io.File
import java.util.Locale

/** Отрисовывает фото-сообщение или fallback, если файл ещё не скачан. */
@Composable
fun MediaMessage(
    msg: Message,
    icon: ImageVector,
    label: String,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    showMetaOverlay: Boolean = false
) {
    val file = msg.fileLocalPath?.let(::File)
    val imageAspectRatio = remember(msg.width, msg.height) {
        mediaAspectRatio(msg.width, msg.height)
    }

    MediaFrame(
        file = file,
        mimeType = "image/*",
        aspectRatio = imageAspectRatio,
        shape = shape,
        content = {
            if (file.existsOnDisk()) {
                AsyncImage(
                    model = file,
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                MediaPlaceholder(
                    icon = icon,
                    label = label
                )
            }
        },
        bottomStartContent = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            )
        },
        bottomEndContent = {
            if (showMetaOverlay) {
                MediaMetaBadge(
                    msg = msg,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                )
            }
        }
    )
}

/** Отображает видеосообщение-кружок. */
@Composable
fun VideoNoteMessage(msg: Message) {
    val context = LocalContext.current
    val file = msg.fileLocalPath?.let(::File)

    Box(
        modifier = Modifier
            .size(216.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = file.existsOnDisk()) {
                openMessageFile(context, file, "video/*")
            },
        contentAlignment = Alignment.Center
    ) {
        if (file.existsOnDisk()) {
            AsyncImage(
                model = file,
                contentDescription = "Video note",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f))
            )
        }

        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play",
            tint = Color.White,
            modifier = Modifier.size(52.dp)
        )
    }
}

/** Отображает стикер или заглушку, если файл ещё недоступен. */
@Composable
fun StickerMessage(msg: Message) {
    val file = msg.fileLocalPath?.let(::File)

    Box(
        modifier = Modifier.size(188.dp),
        contentAlignment = Alignment.Center
    ) {
        if (file.existsOnDisk()) {
            AsyncImage(
                model = file,
                contentDescription = "Sticker",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = "Sticker placeholder",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Отображает карточку аудиофайла. */
@Composable
fun AudioMessage(
    msg: Message,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val file = msg.fileLocalPath?.let(::File)

    AttachmentCard(
        modifier = modifier,
        enabled = file.existsOnDisk(),
        onClick = { openMessageFile(context, file, "audio/*") }
    ) {
        AttachmentIconBox(
            backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        ) {
            Icon(
                imageVector = Icons.Default.Audiotrack,
                contentDescription = "Audio",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        AttachmentInfo(
            title = msg.fileName ?: "Audio",
            subtitle = listOfNotNull(
                formatDuration(msg.duration).takeIf { it.isNotBlank() },
                formatFileSize(msg.fileSize).takeIf { it.isNotBlank() }
            ).joinToString(" • ")
        )
    }
}

/** Отображает голосовое сообщение в виде карточки с псевдо-волной. */
@Composable
fun VoiceMessage(
    msg: Message,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val file = msg.fileLocalPath?.let(::File)

    AttachmentCard(
        modifier = modifier,
        enabled = file.existsOnDisk(),
        onClick = { openMessageFile(context, file, "audio/*") }
    ) {
        AttachmentIconBox(
            size = 40.dp,
            backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play voice message",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.widthIn(max = 190.dp)) {
            VoiceWaveform(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatDuration(msg.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Декоративная звуковая волна для карточки голосового сообщения. */
@Composable
private fun VoiceWaveform(
    color: Color,
    modifier: Modifier = Modifier
) {
    val barHeights = listOf(10, 18, 12, 20, 14, 16, 8, 18, 12, 16, 9, 14)

    Row(
        modifier = modifier.height(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        barHeights.forEach { height ->
            Box(
                modifier = Modifier
                    .padding(end = 2.dp)
                    .width(3.dp)
                    .height(height.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(color.copy(alpha = 0.58f))
            )
        }
    }
}

/** Отображает документ и открывает его внешним приложением. */
@Composable
fun DocumentMessage(
    msg: Message,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val file = msg.fileLocalPath?.let(::File)
    val extensionLabel = remember(msg.fileName, msg.fileExtension) {
        documentExtensionLabel(msg)
    }
    val sizeLabel = remember(msg.fileSize) {
        formatFileSize(msg.fileSize).takeIf { it.isNotBlank() }
    }

    val statusLabel = remember(file?.path) {
        if (file.existsOnDisk()) "Tap to open" else "Unavailable"
    }
    val footerLabel = remember(sizeLabel, statusLabel) {
        listOfNotNull(sizeLabel, statusLabel).joinToString(" • ")
    }
    val accentColor = if (msg.isOutgoing) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    val iconSurface = if (msg.isOutgoing) {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    }
    val pillColor = accentColor.copy(alpha = 0.12f)
    val metaColor = accentColor.copy(alpha = 0.72f)

    Row(
        modifier = modifier
            .then(
                if (file.existsOnDisk()) {
                    Modifier.clickable { openMessageFile(context, file, "*/*") }
                } else {
                    Modifier
                }
            )
            .widthIn(min = 220.dp, max = 320.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = "File",
                tint = accentColor,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = msg.fileName ?: "Document",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp
                ),
                color = accentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                extensionLabel?.let {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(pillColor)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor
                        )
                    }
                }
                if (footerLabel.isNotBlank()) {
                    Text(
                        text = footerLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = metaColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Отображает контакт как компактную карточку. */
@Composable
fun ContactMessage(
    msg: Message,
    modifier: Modifier = Modifier
) {
    AttachmentCard(
        modifier = modifier
    ) {
        AttachmentIconBox(
            backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ContactPage,
                contentDescription = "Contact",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        AttachmentInfo(
            title = "Contact",
            subtitle = msg.text ?: "Shared contact"
        )
    }
}

/** Отображает сообщение с геолокацией и позволяет открыть координаты на карте. */
@Composable
fun LocationMessage(msg: Message) {
    val context = LocalContext.current
    val latitude = msg.latitude
    val longitude = msg.longitude
    val coordinatesLabel = remember(latitude, longitude) {
        if (latitude == null || longitude == null) {
            null
        } else {
            String.format(Locale.US, "%.6f, %.6f", latitude, longitude)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.24f))
            .clickable(enabled = latitude != null && longitude != null) {
                openLocationOnMap(context, latitude, longitude)
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        }
        Text(
            text = "Location",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 12.dp)
        )
        Text(
            text = coordinatesLabel ?: "Coordinates are unavailable.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
        )
    }
}

/** Открывает геолокацию во внешнем картографическом приложении или браузере. */
private fun openLocationOnMap(context: Context, latitude: Double?, longitude: Double?) {
    if (latitude == null || longitude == null) return

    val intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    runCatching { context.startActivity(intent) }
}

/** Форматирует размер файла в удобочитаемый вид. */
fun formatFileSize(size: Long?): String {
    if (size == null) return ""
    val kb = size / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1 -> String.format(Locale.US, "%.1f KB", kb)
        else -> "$size B"
    }
}

/** Форматирует длительность медиа в `MM:SS`. */
fun formatDuration(seconds: Long?): String {
    if (seconds == null) return ""
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}

/** Отображает видео или анимацию с inline-превью, если это возможно. */
@Composable
fun VideoMessage(
    msg: Message,
    label: String = "Video",
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    showMetaOverlay: Boolean = false
) {
    val file = msg.fileLocalPath?.let(::File)
    val duration = formatDuration(msg.duration).takeIf { it.isNotBlank() }
    val renderMode = remember(msg.type, file?.path) {
        resolveAnimatedPreviewMode(msg.type, file)
    }
    val videoAspectRatio = remember(msg.width, msg.height) {
        mediaAspectRatio(msg.width, msg.height)
    }

    MediaFrame(
        file = file,
        mimeType = "video/*",
        aspectRatio = videoAspectRatio,
        shape = shape,
        content = {
            when {
                file.existsOnDisk() && renderMode == InlinePreviewMode.GIF -> {
                    AnimatedGifContent(file = file!!)
                }

                file.existsOnDisk() && renderMode == InlinePreviewMode.VIDEO -> {
                    InlineVideoContent(
                        file = file!!,
                        autoplay = msg.type == MessageType.ANIMATION || !videoHasAudio(file)
                    )
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.32f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = label,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomStartContent = {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = duration ?: "Tap to open",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        bottomEndContent = {
            if (showMetaOverlay) {
                MediaMetaBadge(
                    msg = msg,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                )
            }
        }
    )
}

/** Проигрывает GIF-анимацию через Coil. */
@Composable
private fun AnimatedGifContent(file: File) {
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

    if (inspectionMode) {
        AsyncImage(
            model = file,
            contentDescription = "GIF",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        return
    }

    Image(
        painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(context)
                .data(file)
                .allowHardware(false)
                .crossfade(true)
                .build(),
            imageLoader = imageLoader
        ),
        contentDescription = "GIF",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit
    )
}

/** Встраивает видео-плеер ExoPlayer прямо в сообщение. */
@Composable
private fun InlineVideoContent(
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

/** Проверяет, содержит ли видеоролик аудиодорожку. */
private fun videoHasAudio(file: File): Boolean {
    val retriever = MediaMetadataRetriever()
    return runCatching {
        retriever.setDataSource(file.absolutePath)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"
    }.getOrDefault(false).also {
        retriever.release()
    }
}

private enum class InlinePreviewMode {
    GIF,
    VIDEO,
    NONE
}

/** Определяет, как лучше рендерить анимированное вложение: как GIF, видео или заглушку. */
private fun resolveAnimatedPreviewMode(
    type: MessageType,
    file: File?
): InlinePreviewMode {
    val extension = file?.extension?.lowercase()
    return when {
        type == MessageType.VIDEO -> InlinePreviewMode.VIDEO
        type == MessageType.ANIMATION && extension in setOf("gif", "webp") -> InlinePreviewMode.GIF
        type == MessageType.ANIMATION && file != null -> InlinePreviewMode.VIDEO
        else -> InlinePreviewMode.NONE
    }
}

/** Вычисляет безопасное соотношение сторон для медиа-контента. */
private fun mediaAspectRatio(
    width: Int?,
    height: Int?
): Float {
    val mediaWidth = width?.toFloat()
    val mediaHeight = height?.toFloat()
    return if (mediaWidth != null && mediaHeight != null && mediaHeight > 0f) {
        (mediaWidth / mediaHeight).coerceIn(0.56f, 1.9f)
    } else {
        1f
    }
}

/** Открывает локальный файл сообщения через `FileProvider`. */
private fun openMessageFile(
    context: Context,
    file: File?,
    mimeType: String
) {
    if (!file.existsOnDisk()) return

    runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file!!
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }.onFailure {
        Log.e("MsgBubble", "Failed to open ${file?.absolutePath}", it)
    }
}

/** Общий контейнер для визуальных медиа-вложений с градиентом и оверлеями. */
@Composable
private fun MediaFrame(
    file: File?,
    mimeType: String,
    aspectRatio: Float,
    shape: RoundedCornerShape,
    content: @Composable BoxScope.() -> Unit,
    bottomStartContent: @Composable BoxScope.() -> Unit = {},
    bottomEndContent: @Composable BoxScope.() -> Unit = {}
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .heightIn(min = 140.dp, max = 420.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = file.existsOnDisk()) {
                openMessageFile(context, file, mimeType)
            }
    ) {
        content()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.48f))
                    )
                )
        )

        bottomStartContent()
        bottomEndContent()
    }
}

/** Заглушка для медиа, которое ещё не скачано или не удалось загрузить. */
@Composable
private fun MediaPlaceholder(
    icon: ImageVector,
    label: String
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Базовая карточка для файловых вложений. */
@Composable
private fun AttachmentCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .widthIn(max = 280.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.24f))
            .then(
                if (enabled && onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/** Контейнер под иконку вложения. */
@Composable
private fun AttachmentIconBox(
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    shape: RoundedCornerShape = RoundedCornerShape(50.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/** Текстовый блок карточки вложения: заголовок и подпись. */
@Composable
private fun AttachmentInfo(
    title: String,
    subtitle: String,
    titleMaxLines: Int = 1
) {
    Column(modifier = Modifier.widthIn(max = 190.dp)) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            maxLines = titleMaxLines,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Безопасно проверяет наличие файла на диске. */
private fun File?.existsOnDisk(): Boolean = this?.exists() == true

private fun documentExtensionLabel(msg: Message): String? {
    val rawExtension = msg.fileExtension
        ?.trim()
        ?.removePrefix(".")
        ?.takeIf { it.isNotBlank() }
        ?: msg.fileName
            ?.substringAfterLast('.', "")
            ?.trim()
            ?.takeIf { it.isNotBlank() }

    return rawExtension
        ?.uppercase(Locale.US)
        ?.take(6)
}
