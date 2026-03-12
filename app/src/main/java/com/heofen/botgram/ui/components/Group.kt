package com.heofen.botgram.ui.components

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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
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
import com.heofen.botgram.ChatType
import com.heofen.botgram.MessageType
import com.heofen.botgram.database.tables.Chat
import com.heofen.botgram.database.tables.Message
import com.heofen.botgram.database.tables.User
import com.heofen.botgram.ui.theme.BotgramTheme
import com.heofen.botgram.ui.theme.botgramHazeStyle
import com.heofen.botgram.utils.extensions.getInitials
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode


@Composable
fun UserAvatar(
    user: User?,
    modifier: Modifier = Modifier,
    fallbackColor: Color = Color.Gray
) {
    val model = if (user?.avatarLocalPath != null) File(user.avatarLocalPath) else null

    if (model != null && model.exists()) {
        AsyncImage(
            model = model,
            contentDescription = "Avatar",
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(fallbackColor),
            contentAlignment = Alignment.Center
        ) {
            val initials = user?.getInitials() ?: "?"
            Text(
                text = initials,
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun MessageInput(
    text: String,
    hazeState: HazeState,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    val islandStyle = botgramHazeStyle()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RoundedCornerShape(27.dp))
            .hazeEffect(state = hazeState, style = islandStyle)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                shape = RoundedCornerShape(27.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight()
            ) {
                if (text.isEmpty()) {
                    Text(
                        "Сообщение...",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    maxLines = 6,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSendClick,
                enabled = text.isNotBlank(),
                colors = IconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                )
            }
        }
    }
}


@Composable
fun GroupScreenBar(
    chat: Chat,
    hazeState: HazeState,
    onBackClick: () -> Unit = {}
) {
    val islandStyle = botgramHazeStyle()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
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
                    .clickable { onBackClick() }
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(50.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Back",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(50))
                    .hazeEffect(state = hazeState, style = islandStyle)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(50.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                val title = when {
                    chat.title != null -> chat.title
                    else -> "${chat.firstName ?: ""} ${chat.lastName ?: ""}"
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(50.dp)
                    )
                    .hazeEffect(state = hazeState, style = islandStyle),
                contentAlignment = Alignment.Center
            ) {
                ChatAvatar(
                    chat = chat,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

enum class MsgBubbleClusterPosition {
    Single,
    Top,
    Middle,
    Bottom
}

@Composable
fun MessageDateDivider(timestamp: Long) {
    val day = remember(timestamp) {
        Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }
    val formatter = remember(day) {
        val currentYear = LocalDate.now(ZoneId.systemDefault()).year
        if (day.year == currentYear) {
            DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault())
        } else {
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = day.format(formatter),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MsgBubble(
    msg: Message,
    sender: User?,
    replyToMessage: Message? = null,
    replySender: User? = null,
    isPersonalMsg: Boolean = false,
    showAvatar: Boolean = !isPersonalMsg && !msg.isOutgoing,
    showSenderName: Boolean = !isPersonalMsg && !msg.isOutgoing,
    clusterPosition: MsgBubbleClusterPosition = MsgBubbleClusterPosition.Single
) {
    val showChrome = !msg.type.isDetachedBubble()
    val isMediaBubble = msg.type.isRichMediaBubble()
    val isVisualMediaBubble = msg.type.isVisualMediaBubble()
    val hasBodyText = when (msg.type) {
        MessageType.TEXT -> !msg.text.isNullOrBlank()
        else -> !msg.caption.isNullOrBlank()
    }
    val showMediaMetaOverlay = isVisualMediaBubble && !hasBodyText
    val bodyHorizontalPadding = if (showChrome) 12.dp else 4.dp
    val bodyTopPadding = if (showChrome && msg.replyMsgId == null && !isMediaBubble) 10.dp else 0.dp
    val containerColor = if (msg.isOutgoing) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = if (msg.isOutgoing) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    val metaColor = if (showChrome) {
        contentColor.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val bubbleShape = remember(msg.isOutgoing, clusterPosition, showChrome) {
        msgBubbleShape(
            isOutgoing = msg.isOutgoing,
            position = clusterPosition,
            showChrome = showChrome
        )
    }
    val mediaShape = remember(msg.replyMsgId, hasBodyText, showMediaMetaOverlay) {
        mediaContentShape(
            hasContentAbove = msg.replyMsgId != null,
            hasContentBelow = hasBodyText || !showMediaMetaOverlay
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxBubbleWidth = when {
            msg.type == MessageType.VIDEO_NOTE || msg.type.isStickerType() -> 220.dp
            isMediaBubble -> maxWidth * 0.72f
            else -> maxWidth * 0.76f
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (msg.isOutgoing) Arrangement.End else Arrangement.Start
        ) {
            if (!isPersonalMsg && !msg.isOutgoing) {
                Box(
                    modifier = Modifier.width(40.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    if (showAvatar) {
                        UserAvatar(
                            user = sender,
                            modifier = Modifier.size(36.dp),
                            fallbackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                modifier = Modifier.widthIn(max = maxBubbleWidth),
                horizontalAlignment = if (msg.isOutgoing) Alignment.End else Alignment.Start
            ) {
                if (showSenderName) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box (
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
                    ) {
                        Text(
                            text = sender.displayName(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Box(
                    modifier = Modifier
                        .clip(bubbleShape)
                        .then(
                            if (showChrome) {
                                Modifier
                                    .background(containerColor)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f),
                                        shape = bubbleShape
                                    )
                            } else {
                                Modifier
                            }
                        )
                ) {
                    Column(modifier = Modifier.wrapContentWidth()) {
                        if (msg.replyMsgId != null) {
                            ReplyPreview(
                                replyMessage = replyToMessage,
                                replySender = replySender,
                                isOutgoing = msg.isOutgoing,
                                modifier = Modifier.padding(
                                    start = if (showChrome) 12.dp else 4.dp,
                                    end = if (showChrome) 12.dp else 4.dp,
                                    top = if (showChrome) 10.dp else 2.dp
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        when (msg.type) {
                            MessageType.TEXT -> {
                                msg.text?.takeIf { it.isNotBlank() }?.let {
                                    MessageTextWithAdaptiveMeta(
                                        text = it,
                                        msg = msg,
                                        textColor = contentColor,
                                        metaColor = metaColor,
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 16.sp,
                                            lineHeight = 22.sp
                                        ),
                                        fallbackBottomPadding = if (showChrome) 10.dp else 2.dp,
                                        modifier = Modifier.padding(
                                            start = bodyHorizontalPadding,
                                            end = bodyHorizontalPadding,
                                            top = bodyTopPadding
                                        )
                                    )
                                }
                            }
                            MessageType.PHOTO -> MediaMessage(
                                msg = msg,
                                icon = Icons.Default.Image,
                                label = "Photo",
                                shape = mediaShape,
                                showMetaOverlay = showMediaMetaOverlay
                            )
                            MessageType.VIDEO,
                            MessageType.ANIMATION -> VideoMessage(
                                msg = msg,
                                label = if (msg.type == MessageType.ANIMATION) "GIF" else "Video",
                                shape = mediaShape,
                                showMetaOverlay = showMediaMetaOverlay
                            )
                            MessageType.VIDEO_NOTE -> VideoNoteMessage(msg)
                            MessageType.AUDIO -> AudioMessage(
                                msg = msg,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            MessageType.VOICE -> VoiceMessage(
                                msg = msg,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            MessageType.DOCUMENT -> DocumentMessage(
                                msg = msg,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            MessageType.STICKER,
                            MessageType.ANIMATED_STICKER,
                            MessageType.VIDEO_STICKER -> StickerMessage(msg)
                            MessageType.CONTACT -> ContactMessage(
                                msg = msg,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            MessageType.LOCATION -> LocationMessage(msg)
                            else -> {
                                Text(
                                    text = "Unsupported message type: ${msg.type}",
                                    fontSize = 14.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(
                                        start = bodyHorizontalPadding,
                                        end = bodyHorizontalPadding,
                                        top = bodyTopPadding
                                    )
                                )
                            }
                        }

                        if (msg.type != MessageType.TEXT && !msg.caption.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            MessageTextWithAdaptiveMeta(
                                text = msg.caption,
                                msg = msg,
                                textColor = if (showChrome) contentColor else MaterialTheme.colorScheme.onSurface,
                                metaColor = metaColor,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp
                                ),
                                fallbackBottomPadding = if (showChrome) 10.dp else 2.dp,
                                modifier = Modifier.padding(
                                    start = bodyHorizontalPadding,
                                    end = bodyHorizontalPadding
                                )
                            )
                        }

                        if (!showMediaMetaOverlay && msg.type != MessageType.TEXT && msg.caption.isNullOrBlank()) {
                            MessageMetaRow(
                                msg = msg,
                                color = metaColor,
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .padding(
                                        start = if (showChrome) 12.dp else 4.dp,
                                        end = if (showChrome) 12.dp else 4.dp,
                                        top = 6.dp,
                                        bottom = if (showChrome) 10.dp else 2.dp
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageMetaRow(
    msg: Message,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MessageMetaContent(msg = msg, color = color)
    }
}

@Composable
private fun MessageTextWithAdaptiveMeta(
    text: String,
    msg: Message,
    textStyle: TextStyle,
    textColor: Color,
    metaColor: Color,
    fallbackBottomPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val metaLabel = remember(msg.isEdited, msg.timestamp) { buildMessageMetaLabel(msg) }
    val metaTextStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
    )

    BoxWithConstraints(modifier = modifier) {
        val availableWidthPx = with(density) { maxWidth.roundToPx() }
        val textToMetaGapPx = with(density) { 4.dp.roundToPx() }
        val metaTextLayout = remember(metaLabel, metaTextStyle) {
            textMeasurer.measure(
                text = AnnotatedString(metaLabel),
                style = metaTextStyle
            )
        }
        val inlineMetaContentWidthPx = remember(msg.isOutgoing, metaTextLayout.size.width, density) {
            metaTextLayout.size.width + if (msg.isOutgoing) {
                with(density) { 2.dp.roundToPx() + 14.dp.roundToPx() }
            } else {
                0
            }
        }
        val separateMetaContentWidthPx = remember(msg.isOutgoing, metaTextLayout.size.width, density) {
            metaTextLayout.size.width + if (msg.isOutgoing) {
                with(density) { 4.dp.roundToPx() + 14.dp.roundToPx() }
            } else {
                0
            }
        }
        val inlineMetaReserveWidthPx = remember(inlineMetaContentWidthPx, textToMetaGapPx) {
            inlineMetaContentWidthPx + textToMetaGapPx
        }
        val inlineMetaContentHeightPx = remember(metaTextLayout.size.height, density) {
            max(metaTextLayout.size.height, with(density) { 14.dp.roundToPx() })
        }
        val baseLayout = remember(text, textStyle, availableWidthPx) {
            textMeasurer.measure(
                text = AnnotatedString(text),
                style = textStyle,
                constraints = Constraints(maxWidth = availableWidthPx)
            )
        }
        val baseLastLineWidthPx = remember(baseLayout) {
            val lastLineIndex = baseLayout.lineCount - 1
            baseLayout.getLineRight(lastLineIndex) - baseLayout.getLineLeft(lastLineIndex)
        }
        val inlineBubbleWidthPx = remember(baseLayout, baseLastLineWidthPx, inlineMetaReserveWidthPx) {
            max(
                baseLayout.size.width,
                ceil(baseLastLineWidthPx + inlineMetaReserveWidthPx).toInt()
            )
        }
        val inlineTextLayout = remember(text, textStyle, inlineBubbleWidthPx) {
            textMeasurer.measure(
                text = AnnotatedString(text),
                style = textStyle,
                constraints = Constraints(maxWidth = inlineBubbleWidthPx)
            )
        }
        val inlineLastLineWidthPx = remember(inlineTextLayout) {
            val lastLineIndex = inlineTextLayout.lineCount - 1
            inlineTextLayout.getLineRight(lastLineIndex) - inlineTextLayout.getLineLeft(lastLineIndex)
        }
        val inlineLastLineBottomPx = remember(inlineTextLayout) {
            val lastLineIndex = inlineTextLayout.lineCount - 1
            ceil(inlineTextLayout.getLineBottom(lastLineIndex)).toInt()
        }
        val canInlineMeta = remember(text, inlineBubbleWidthPx, availableWidthPx, inlineLastLineWidthPx) {
            if (text.isBlank() || text.endsWith('\n')) {
                false
            } else {
                inlineBubbleWidthPx <= availableWidthPx &&
                    ceil(inlineLastLineWidthPx + inlineMetaReserveWidthPx).toInt() <= inlineBubbleWidthPx
            }
        }
        val inlineBubbleWidthDp = with(density) { inlineBubbleWidthPx.toDp() }
        val inlineMetaXOffsetPx = inlineBubbleWidthPx - inlineMetaContentWidthPx
        val inlineMetaYOffsetPx = max(0, inlineLastLineBottomPx - inlineMetaContentHeightPx)
        val inlineBottomPadding = if (inlineTextLayout.lineCount > 1) fallbackBottomPadding else 0.dp
        val fallbackBubbleWidthPx = remember(baseLayout.size.width, separateMetaContentWidthPx) {
            max(baseLayout.size.width, separateMetaContentWidthPx)
        }
        val fallbackBubbleWidthDp = with(density) { fallbackBubbleWidthPx.toDp() }

        if (canInlineMeta) {
            Box(
                modifier = Modifier
                    .width(inlineBubbleWidthDp)
                    .padding(bottom = inlineBottomPadding)
            ) {
                SelectionContainer {
                    Text(
                        text = text,
                        style = textStyle,
                        color = textColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(
                    modifier = Modifier.offset {
                        IntOffset(
                            x = inlineMetaXOffsetPx,
                            y = inlineMetaYOffsetPx
                        )
                    }
                ) {
                    MessageMetaContent(msg = msg, color = metaColor, inline = true)
                }
            }
        } else {
            Column(
                modifier = Modifier.width(fallbackBubbleWidthDp)
            ) {
                SelectionContainer {
                    Text(
                        text = text,
                        style = textStyle,
                        color = textColor
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    MessageMetaRow(
                        msg = msg,
                        color = metaColor,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
                if (fallbackBottomPadding > 0.dp) {
                    Spacer(modifier = Modifier.height(fallbackBottomPadding))
                }
            }
        }
    }
}

private fun buildMessageMetaLabel(msg: Message): String {
    val formattedDate = formatMessageTime(msg.timestamp)
    return if (msg.isEdited) "edited $formattedDate" else formattedDate
}

@Composable
private fun MessageMetaContent(
    msg: Message,
    color: Color,
    inline: Boolean = false
) {
    val metaLabel = remember(msg.isEdited, msg.timestamp) { buildMessageMetaLabel(msg) }

    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = metaLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            color = color
        )

        if (msg.isOutgoing) {
            Spacer(modifier = Modifier.width(if (inline) 2.dp else 4.dp))
            Icon(
                imageVector = if (msg.readStatus) Icons.Default.DoneAll else Icons.Default.Done,
                contentDescription = if (msg.readStatus) "Read" else "Sent",
                tint = color,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun MediaMetaBadge(
    msg: Message,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x99353535))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatMessageTime(msg.timestamp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )

        if (msg.isOutgoing) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (msg.readStatus) Icons.Default.DoneAll else Icons.Default.Done,
                contentDescription = if (msg.readStatus) "Read" else "Sent",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun ReplyPreview(
    replyMessage: Message?,
    replySender: User?,
    isOutgoing: Boolean,
    modifier: Modifier = Modifier
) {
    val accentColor = if (isOutgoing) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = modifier
            .widthIn(max = 280.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.34f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = replySender.displayName(defaultName = "Reply"),
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = replyMessage.replyPreviewText(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MediaMessage(
    msg: Message,
    icon: ImageVector,
    label: String,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    showMetaOverlay: Boolean = false
) {
    val context = LocalContext.current
    val file = msg.fileLocalPath?.let { File(it) }
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val placeholderContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    val imageAspectRatio = remember(msg.width, msg.height) {
        mediaAspectRatio(msg.width, msg.height)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(imageAspectRatio)
            .heightIn(min = 140.dp, max = 420.dp)
            .clip(shape)
            .background(placeholderColor)
            .clickable(enabled = file != null && file.exists()) {
                openMessageFile(context, file, "image/*")
            }
    ) {
        if (file != null && file.exists()) {
            AsyncImage(
                model = file,
                contentDescription = label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(48.dp),
                    tint = placeholderContentColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = label, color = placeholderContentColor)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.48f))
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }

        if (showMetaOverlay) {
            MediaMetaBadge(
                msg = msg,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
            )
        }
    }
}

@Composable
fun VideoNoteMessage(msg: Message) {
    val context = LocalContext.current
    val file = msg.fileLocalPath?.let { File(it) }

    Box(
        modifier = Modifier
            .size(216.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = file != null && file.exists()) {
                openMessageFile(context, file, "video/*")
            },
        contentAlignment = Alignment.Center
    ) {
        if (file != null && file.exists()) {
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

@Composable
fun StickerMessage(msg: Message) {
    val file = msg.fileLocalPath?.let { File(it) }

    Box(
        modifier = Modifier.size(188.dp),
        contentAlignment = Alignment.Center
    ) {
        if (file != null && file.exists()) {
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

@Composable
fun AudioMessage(
    msg: Message,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val file = msg.fileLocalPath?.let { File(it) }

    Row(
        modifier = modifier
            .widthIn(max = 280.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.24f))
            .clickable(enabled = file != null && file.exists()) {
                openMessageFile(context, file, "audio/*")
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Audiotrack,
                contentDescription = "Audio",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.widthIn(max = 190.dp)) {
            Text(
                text = msg.fileName ?: "Audio",
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOfNotNull(
                    formatDuration(msg.duration).takeIf { it.isNotBlank() },
                    formatFileSize(msg.fileSize).takeIf { it.isNotBlank() }
                ).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun VoiceMessage(
    msg: Message,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val file = msg.fileLocalPath?.let { File(it) }

    Row(
        modifier = modifier
            .widthIn(max = 280.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.24f))
            .clickable(enabled = file != null && file.exists()) {
                openMessageFile(context, file, "audio/*")
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
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

@Composable
fun DocumentMessage(
    msg: Message,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val file = msg.fileLocalPath?.let { File(it) }

    Row(
        modifier = modifier
            .widthIn(max = 280.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.24f))
            .clickable(enabled = file != null && file.exists()) {
                openMessageFile(context, file, "*/*")
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = "File",
                tint = MaterialTheme.colorScheme.tertiary
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.widthIn(max = 190.dp)) {
            Text(
                text = msg.fileName ?: "Document",
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatFileSize(msg.fileSize),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ContactMessage(
    msg: Message,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .widthIn(max = 280.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.24f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ContactPage,
                contentDescription = "Contact",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.widthIn(max = 190.dp)) {
            Text(text = "Contact", fontWeight = FontWeight.SemiBold)
            Text(
                text = msg.text ?: "Shared contact",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LocationMessage(msg: Message) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.24f))
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
            text = "Location preview",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 12.dp)
        )
        Text(
            text = msg.text ?: "Map coordinates are not available in the current model yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
        )
    }
}

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

fun formatDuration(seconds: Long?): String {
    if (seconds == null) return ""
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

@Composable
fun VideoMessage(
    msg: Message,
    label: String = "Video",
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    showMetaOverlay: Boolean = false
) {
    val context = LocalContext.current
    val file = msg.fileLocalPath?.let { File(it) }
    val duration = formatDuration(msg.duration).takeIf { it.isNotBlank() }
    val renderMode = remember(msg.type, file?.path) {
        resolveAnimatedPreviewMode(msg.type, file)
    }
    val videoAspectRatio = remember(msg.width, msg.height) {
        mediaAspectRatio(msg.width, msg.height)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(videoAspectRatio)
            .heightIn(min = 140.dp, max = 420.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = file != null && file.exists()) {
                openMessageFile(context, file, "video/*")
            }
    ) {
        when {
            file != null && file.exists() && renderMode == InlinePreviewMode.GIF -> {
                AnimatedGifContent(file = file)
            }

            file != null && file.exists() && renderMode == InlinePreviewMode.VIDEO -> {
                InlineVideoContent(
                    file = file,
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.48f))
                    )
                )
        )

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

        if (showMetaOverlay) {
            MediaMetaBadge(
                msg = msg,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
            )
        }
    }
}

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

private fun msgBubbleShape(
    isOutgoing: Boolean,
    position: MsgBubbleClusterPosition,
    showChrome: Boolean
): RoundedCornerShape {
    if (!showChrome) return RoundedCornerShape(0.dp)

    return if (isOutgoing) {
        when (position) {
            MsgBubbleClusterPosition.Single -> RoundedCornerShape(12.dp, 12.dp, 4.dp, 12.dp)
            MsgBubbleClusterPosition.Top -> RoundedCornerShape(12.dp, 12.dp, 4.dp, 12.dp)
            MsgBubbleClusterPosition.Middle -> RoundedCornerShape(12.dp, 4.dp, 4.dp, 12.dp)
            MsgBubbleClusterPosition.Bottom -> RoundedCornerShape(12.dp, 4.dp, 12.dp, 12.dp)
        }
    } else {
        when (position) {
            MsgBubbleClusterPosition.Single -> RoundedCornerShape(12.dp, 12.dp, 12.dp, 4.dp)
            MsgBubbleClusterPosition.Top -> RoundedCornerShape(12.dp, 12.dp, 12.dp, 4.dp)
            MsgBubbleClusterPosition.Middle -> RoundedCornerShape(4.dp, 12.dp, 12.dp, 4.dp)
            MsgBubbleClusterPosition.Bottom -> RoundedCornerShape(4.dp, 12.dp, 12.dp, 12.dp)
        }
    }
}

private fun mediaContentShape(
    hasContentAbove: Boolean,
    hasContentBelow: Boolean
): RoundedCornerShape {
    val large = 10.dp
    val inner = 2.dp
    return RoundedCornerShape(
        topStart = if (hasContentAbove) inner else large,
        topEnd = if (hasContentAbove) inner else large,
        bottomEnd = if (hasContentBelow) inner else large,
        bottomStart = if (hasContentBelow) inner else large
    )
}

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

private fun MessageType.isDetachedBubble(): Boolean =
    this == MessageType.STICKER ||
        this == MessageType.ANIMATED_STICKER ||
        this == MessageType.VIDEO_STICKER ||
        this == MessageType.VIDEO_NOTE

private fun MessageType.isStickerType(): Boolean =
    this == MessageType.STICKER ||
        this == MessageType.ANIMATED_STICKER ||
        this == MessageType.VIDEO_STICKER

private fun MessageType.isRichMediaBubble(): Boolean =
    this == MessageType.PHOTO ||
        this == MessageType.VIDEO ||
        this == MessageType.ANIMATION ||
        this == MessageType.LOCATION

private fun MessageType.isVisualMediaBubble(): Boolean =
    this == MessageType.PHOTO ||
        this == MessageType.VIDEO ||
        this == MessageType.ANIMATION

private fun formatMessageTime(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
}

private fun User?.displayName(defaultName: String = "Unknown"): String =
    listOfNotNull(this?.firstName, this?.lastName)
        .joinToString(" ")
        .ifBlank { defaultName }

private fun Message?.replyPreviewText(): String =
    when (this?.type) {
        null -> "Original message"
        MessageType.TEXT -> this.text?.ifBlank { "Message" } ?: "Message"
        MessageType.PHOTO -> this.caption?.ifBlank { "Photo" } ?: "Photo"
        MessageType.VIDEO -> this.caption?.ifBlank { "Video" } ?: "Video"
        MessageType.ANIMATION -> this.caption?.ifBlank { "GIF" } ?: "GIF"
        MessageType.AUDIO -> this.fileName ?: "Audio"
        MessageType.VOICE -> "Voice message"
        MessageType.VIDEO_NOTE -> "Video note"
        MessageType.DOCUMENT -> this.fileName ?: "Document"
        MessageType.STICKER,
        MessageType.ANIMATED_STICKER,
        MessageType.VIDEO_STICKER -> "Sticker"
        MessageType.CONTACT -> this.text ?: "Contact"
        MessageType.LOCATION -> "Location"
    }

private fun openMessageFile(
    context: android.content.Context,
    file: File?,
    mimeType: String
) {
    if (file == null || !file.exists()) return

    runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }.onFailure {
        Log.e("MsgBubble", "Failed to open ${file.absolutePath}", it)
    }
}


@Composable
@Preview
fun MsgBubblePreview() {
    val msg = Message(
        messageId = 1,
        chatId = 123456L,
        topicId = null,
        senderId = 789012L,
        type = MessageType.TEXT,
        timestamp = 1734357600000L,
        text = "Привет! Как дела?",
        caption = null,
        replyMsgId = null,
        replyMsgTopicId = null,
        fileName = null,
        fileExtension = null,
        fileId = null,
        fileUniqueId = null,
        fileLocalPath = null,
        fileSize = null,
        width = null,
        height = null,
        duration = null,
        thumbnailFileId = null,
        isEdited = false,
        editedAt = null,
        mediaGroupId = null,
        readStatus = true,
        isOutgoing = false
    )
    val user = User(
        id = 1,
        firstName = "Геннадий",
        lastName = null,
        bio = null,
        avatarFileId = null,
        avatarFileUniqueId = null,
        avatarLocalPath = null,
    )
    BotgramTheme() {
        MsgBubble(msg, user)
    }
}

@Preview(showBackground = true)
@Composable
fun GroupScreenBarPreview() {
    val chat = Chat(
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
    )
    val hazeState = remember { HazeState() }
    BotgramTheme() {
        GroupScreenBar(
            chat = chat,
            hazeState = hazeState
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageInputPreview() {
    val hazeState = remember { HazeState() }

    BotgramTheme {
        MessageInput(
            text = "",
            hazeState = hazeState,
            onTextChange = {},
            onSendClick = {}
        )
    }
}
