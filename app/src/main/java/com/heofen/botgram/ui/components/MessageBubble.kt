package com.heofen.botgram.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heofen.botgram.MessageType
import com.heofen.botgram.database.tables.Message
import com.heofen.botgram.database.tables.User
import com.heofen.botgram.ui.theme.BotgramTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

/** Положение пузыря сообщения внутри визуального кластера соседних сообщений. */
enum class MsgBubbleClusterPosition {
    Single,
    Top,
    Middle,
    Bottom
}

private object MsgBubbleTokens {
    val BubbleCornerRadius = 12.dp
    val BubbleHorizontalPadding = 12.dp
    val BubbleVerticalPadding = 8.dp
    val AttachmentBubblePadding = 8.dp
    val TimestampEnd = 8.dp
    val TimestampBottom = 8.dp
    val TimestampReservedWidth = 40.dp
    val TimestampReservedHeight = 16.dp
    val TextBottomAir = 4.dp
    val CaptionTopSpacing = 6.dp
    val AttachmentBottomReserve = 28.dp
    val InlineMetaGap = 4.dp
    val SingleLineBottomInset = 2.dp
    val VisualMediaMetaGap = 2.dp
    val VisualMediaMetaHorizontalPadding = 7.dp
    val VisualMediaMetaVerticalPadding = 2.dp
    val VisualMediaMetaCornerRadius = 7.dp
    val VisualMediaFooterHorizontalPadding = 8.dp
    val VisualMediaFooterTopPadding = 7.dp
    val VisualMediaFooterMetaEnd = 8.dp
    val VisualMediaFooterMetaBottom = 7.dp
    val VisualMediaFooterTextToMetaGap = 4.dp
    val VisualMediaFooterMinHeight = 26.dp
}

/** Разделитель по дате между сообщениями разных календарных дней. */
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

/** Универсальный пузырь сообщения с поддержкой reply, медиа и метаданных. */
@Composable
fun MsgBubble(
    msg: Message,
    sender: User?,
    replyToMessage: Message? = null,
    replySender: User? = null,
    modifier: Modifier = Modifier,
    isPersonalMsg: Boolean = false,
    showAvatar: Boolean = !isPersonalMsg && !msg.isOutgoing,
    showSenderName: Boolean = !isPersonalMsg && !msg.isOutgoing,
    clusterPosition: MsgBubbleClusterPosition = MsgBubbleClusterPosition.Single,
    voicePlaybackState: VoiceMessagePlaybackState? = null,
    onClick: (() -> Unit)? = null
) {
    val isMediaBubble = msg.type.isRichMediaBubble()
    val isVisualMediaBubble = msg.type.isVisualMediaBubble()
    val hasBodyText = when (msg.type) {
        MessageType.TEXT -> !msg.text.isNullOrBlank()
        else -> !msg.caption.isNullOrBlank()
    }
    val showStandaloneVisualMedia = isVisualMediaBubble && !hasBodyText && msg.replyMsgId == null
    val showChrome = !msg.type.isDetachedBubble() && !showStandaloneVisualMedia
    val showMediaMetaOverlay = isVisualMediaBubble && !hasBodyText
    val bodyHorizontalPadding = if (showChrome) MsgBubbleTokens.BubbleHorizontalPadding else 4.dp
    val bodyTopPadding = if (showChrome && msg.replyMsgId == null && !isMediaBubble) {
        MsgBubbleTokens.BubbleVerticalPadding
    } else {
        0.dp
    }
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
    val showBubbleBorder = showChrome && msg.type != MessageType.VOICE
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

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val maxBubbleWidth = when {
            msg.type == MessageType.VIDEO_NOTE || msg.type.isStickerType() -> 220.dp
            isMediaBubble -> maxWidth * 0.7f
            else -> maxWidth * 0.7f
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
                    SenderNameBadge(sender = sender)
                }

                Box(
                    modifier = Modifier
                        .clip(bubbleShape)
                        .then(
                            if (showChrome) {
                                Modifier
                                    .background(containerColor)
                                    .then(
                                        if (showBubbleBorder) {
                                            Modifier.border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f),
                                                shape = bubbleShape
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                            } else {
                                Modifier
                            }
                        )
                        .then(
                            if (onClick != null) {
                                Modifier.clickable(onClick = onClick)
                            } else {
                                Modifier
                            }
                        )
                ) {
                    MessageBubbleContent(
                        msg = msg,
                        replyToMessage = replyToMessage,
                        replySender = replySender,
                        showChrome = showChrome,
                        showMediaMetaOverlay = showMediaMetaOverlay,
                        bodyHorizontalPadding = bodyHorizontalPadding,
                        bodyTopPadding = bodyTopPadding,
                        containerColor = containerColor,
                        contentColor = contentColor,
                        metaColor = metaColor,
                        mediaShape = mediaShape,
                        voicePlaybackState = voicePlaybackState
                    )
                }
            }
        }
    }
}

/** Небольшой бейдж с именем отправителя для групповых чатов. */
@Composable
private fun SenderNameBadge(sender: User?) {
    Spacer(modifier = Modifier.height(6.dp))
    Box(
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

/** Собирает содержимое пузыря: reply, основной контент и метаданные. */
@Composable
private fun MessageBubbleContent(
    msg: Message,
    replyToMessage: Message?,
    replySender: User?,
    showChrome: Boolean,
    showMediaMetaOverlay: Boolean,
    bodyHorizontalPadding: Dp,
    bodyTopPadding: Dp,
    containerColor: Color,
    contentColor: Color,
    metaColor: Color,
    mediaShape: RoundedCornerShape,
    voicePlaybackState: VoiceMessagePlaybackState?
) {
    val replyPadding = Modifier.padding(
        start = if (showChrome) 12.dp else 4.dp,
        end = if (showChrome) 12.dp else 4.dp,
        top = if (showChrome) 9.dp else 2.dp
    )

    if (msg.replyMsgId != null) {
        ReplyAwareBubbleLayout(
            reply = {
                ReplyPreview(
                    replyMessage = replyToMessage,
                    replySender = replySender,
                    isOutgoing = msg.isOutgoing,
                    modifier = replyPadding
                )
            },
            body = {
                BubbleBodyContent(
                    msg = msg,
                    showChrome = showChrome,
                    showMediaMetaOverlay = showMediaMetaOverlay,
                    bodyHorizontalPadding = bodyHorizontalPadding,
                    bodyTopPadding = bodyTopPadding,
                    containerColor = containerColor,
                    contentColor = contentColor,
                    metaColor = metaColor,
                    mediaShape = mediaShape,
                    voicePlaybackState = voicePlaybackState
                )
            }
        )
    } else {
        BubbleBodyContent(
            msg = msg,
            showChrome = showChrome,
            showMediaMetaOverlay = showMediaMetaOverlay,
            bodyHorizontalPadding = bodyHorizontalPadding,
            bodyTopPadding = bodyTopPadding,
            containerColor = containerColor,
            contentColor = contentColor,
            metaColor = metaColor,
            mediaShape = mediaShape,
            voicePlaybackState = voicePlaybackState
        )
    }
}

/** Раскладывает reply-превью и тело сообщения в один пузырь фиксированной ширины. */
@Composable
private fun ReplyAwareBubbleLayout(
    reply: @Composable () -> Unit,
    body: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Layout(
        modifier = modifier,
        content = {
            reply()
            body()
        }
    ) { measurables, constraints ->
        val bodyPlaceable = measurables[1].measure(constraints)
        val targetWidth = bodyPlaceable.width.coerceIn(constraints.minWidth, constraints.maxWidth)
        val replyPlaceable = measurables[0].measure(
            constraints.copy(
                minWidth = targetWidth,
                maxWidth = targetWidth
            )
        )
        val spacing = 4.dp.roundToPx()
        val width = max(replyPlaceable.width, bodyPlaceable.width)
        val height = replyPlaceable.height + spacing + bodyPlaceable.height

        layout(width, height) {
            replyPlaceable.placeRelative(0, 0)
            bodyPlaceable.placeRelative(0, replyPlaceable.height + spacing)
        }
    }
}

/** Выбирает визуальное представление сообщения по его типу. */
@Composable
private fun BubbleBodyContent(
    msg: Message,
    showChrome: Boolean,
    showMediaMetaOverlay: Boolean,
    bodyHorizontalPadding: Dp,
    bodyTopPadding: Dp,
    containerColor: Color,
    contentColor: Color,
    metaColor: Color,
    mediaShape: RoundedCornerShape,
    voicePlaybackState: VoiceMessagePlaybackState?
) {
    val isVisualMediaBubble = msg.type.isVisualMediaBubble()
    val hasCaption = msg.type != MessageType.TEXT && !msg.caption.isNullOrBlank()
    val showPinnedMeta = !isVisualMediaBubble &&
        !showMediaMetaOverlay &&
        msg.type != MessageType.TEXT &&
        msg.type != MessageType.VOICE

    val bodyContent: @Composable () -> Unit = {
        when (msg.type) {
            MessageType.TEXT -> {
                msg.text?.takeIf { it.isNotBlank() }?.let {
                    MessageTextWithPinnedMeta(
                        text = it,
                        msg = msg,
                        textColor = contentColor,
                        metaColor = metaColor,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                        ),
                        minHeight = 0.dp,
                        modifier = Modifier.padding(
                            start = bodyHorizontalPadding,
                            end = bodyHorizontalPadding,
                            top = bodyTopPadding
                        )
                    )
                }
            }

            MessageType.PHOTO -> VisualMediaBubbleLayout(
                msg = msg,
                showChrome = showChrome,
                bubbleColor = containerColor,
                contentColor = contentColor,
                media = {
                    MediaMessage(
                        msg = msg,
                        icon = Icons.Default.Image,
                        label = "Photo",
                        shape = mediaShape,
                        showMetaOverlay = false
                    )
                }
            )

            MessageType.VIDEO,
            MessageType.ANIMATION -> VisualMediaBubbleLayout(
                msg = msg,
                showChrome = showChrome,
                bubbleColor = containerColor,
                contentColor = contentColor,
                media = {
                    VideoMessage(
                        msg = msg,
                        label = if (msg.type == MessageType.ANIMATION) "GIF" else "Video",
                        shape = mediaShape,
                        showMetaOverlay = false
                    )
                }
            )

            MessageType.VIDEO_NOTE -> VideoNoteMessage(msg)

            MessageType.AUDIO -> AudioMessage(
                msg = msg,
                modifier = Modifier.padding(horizontal = MsgBubbleTokens.AttachmentBubblePadding)
            )

            MessageType.VOICE -> VoiceMessage(
                msg = msg,
                playbackState = voicePlaybackState,
                modifier = Modifier.padding(
                    horizontal = MsgBubbleTokens.AttachmentBubblePadding,
                    vertical = MsgBubbleTokens.BubbleVerticalPadding
                )
            )

            MessageType.DOCUMENT -> DocumentMessage(
                msg = msg,
                modifier = Modifier.padding(horizontal = MsgBubbleTokens.AttachmentBubblePadding)
            )

            MessageType.STICKER,
            MessageType.ANIMATED_STICKER,
            MessageType.VIDEO_STICKER -> StickerMessage(msg)

            MessageType.CONTACT -> ContactMessage(
                msg = msg,
                modifier = Modifier.padding(horizontal = MsgBubbleTokens.AttachmentBubblePadding)
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

        if (!isVisualMediaBubble && msg.type != MessageType.TEXT && hasCaption) {
            Spacer(modifier = Modifier.height(MsgBubbleTokens.CaptionTopSpacing))
            CaptionTextBlock(
                text = msg.caption,
                textColor = if (showChrome) contentColor else MaterialTheme.colorScheme.onSurface,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                ),
                modifier = Modifier.padding(
                    start = bodyHorizontalPadding,
                    end = bodyHorizontalPadding
                )
            )
        }
        if (showPinnedMeta && msg.caption.isNullOrBlank()) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MsgBubbleTokens.AttachmentBottomReserve)
            )
        }
    }

    if (showPinnedMeta) {
        BubbleMetaAnchoredBox(
            msg = msg,
            metaColor = metaColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.wrapContentWidth()) {
                bodyContent()
            }
        }
    } else {
        Column(modifier = Modifier.wrapContentWidth()) {
            bodyContent()
        }
    }
}

@Composable
private fun VisualMediaBubbleLayout(
    msg: Message,
    showChrome: Boolean,
    bubbleColor: Color,
    contentColor: Color,
    media: @Composable () -> Unit
) {
    val hasCaption = !msg.caption.isNullOrBlank()

    Column(modifier = Modifier.wrapContentWidth()) {
        media()
        when {
            hasCaption -> {
                MediaCaptionFooter(
                    msg = msg,
                    text = msg.caption.orEmpty(),
                    textColor = contentColor,
                    metaColor = visualMediaMetaColor(bubbleColor, contentColor)
                )
            }

            showChrome -> {
                MediaMetaFooter(
                    msg = msg,
                    metaColor = visualMediaMetaColor(bubbleColor, contentColor)
                )
            }

            else -> {
                Spacer(modifier = Modifier.height(MsgBubbleTokens.VisualMediaMetaGap))
                Box(modifier = Modifier.fillMaxWidth()) {
                    DetachedMediaMetaPill(
                        msg = msg,
                        backgroundColor = bubbleColor,
                        contentColor = visualMediaMetaColor(bubbleColor, contentColor),
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaMetaFooter(
    msg: Message,
    metaColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MsgBubbleTokens.VisualMediaFooterMinHeight)
    ) {
        Text(
            text = formatMessageTime(msg.timestamp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            ),
            color = metaColor,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    top = MsgBubbleTokens.VisualMediaFooterTopPadding,
                    end = MsgBubbleTokens.VisualMediaFooterMetaEnd,
                    bottom = MsgBubbleTokens.VisualMediaFooterMetaBottom
                )
        )
    }
}

@Composable
private fun CaptionTextBlock(
    text: String,
    textStyle: TextStyle,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    SelectionContainer {
        Text(
            text = text,
            style = textStyle,
            color = textColor,
            modifier = modifier.padding(
                end = MsgBubbleTokens.TimestampReservedWidth,
                bottom = MsgBubbleTokens.TimestampReservedHeight
            )
        )
    }
}

@Composable
private fun MediaCaptionFooter(
    msg: Message,
    text: String,
    textColor: Color,
    metaColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MsgBubbleTokens.VisualMediaFooterMinHeight)
            .padding(
                start = MsgBubbleTokens.VisualMediaFooterHorizontalPadding,
                end = MsgBubbleTokens.VisualMediaFooterHorizontalPadding,
                top = MsgBubbleTokens.VisualMediaFooterTopPadding
            )
    ) {
        SelectionContainer {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                ),
                color = textColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        end = MsgBubbleTokens.TimestampReservedWidth,
                        bottom = MsgBubbleTokens.VisualMediaFooterMetaBottom
                    )
            )
        }
        Text(
            text = formatMessageTime(msg.timestamp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            ),
            color = metaColor,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = MsgBubbleTokens.VisualMediaFooterMetaEnd,
                    bottom = MsgBubbleTokens.VisualMediaFooterMetaBottom
                )
        )
    }
}

/** Рисует текст сообщения с закреплённым временем в правом нижнем углу. */
@Composable
private fun MessageTextWithPinnedMeta(
    text: String,
    msg: Message,
    textStyle: TextStyle,
    textColor: Color,
    metaColor: Color,
    minHeight: Dp = 0.dp,
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
        val textToMetaGapPx = with(density) { MsgBubbleTokens.InlineMetaGap.roundToPx() }
        val timestampEndPx = with(density) { MsgBubbleTokens.TimestampEnd.roundToPx() }
        val metaTextLayout = remember(metaLabel, metaTextStyle) {
            textMeasurer.measure(
                text = AnnotatedString(metaLabel),
                style = metaTextStyle
            )
        }
        val inlineMetaContentWidthPx = remember(msg.isOutgoing, metaTextLayout.size.width, density) {
            metaTextLayout.size.width + if (msg.isOutgoing) {
                with(density) { 4.dp.roundToPx() + 14.dp.roundToPx() }
            } else {
                0
            }
        }
        val inlineMetaReserveWidthPx = inlineMetaContentWidthPx + textToMetaGapPx + timestampEndPx
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
        val inlineMetaXOffsetPx = inlineBubbleWidthPx - inlineMetaContentWidthPx - timestampEndPx
        val inlineMetaYOffsetPx = max(0, inlineLastLineBottomPx - inlineMetaContentHeightPx)
        val inlineBottomInset = if (inlineTextLayout.lineCount > 1) {
            MsgBubbleTokens.TimestampBottom
        } else {
            MsgBubbleTokens.SingleLineBottomInset
        }

        if (canInlineMeta) {
            Box(
                modifier = Modifier
                    .width(inlineBubbleWidthDp)
                    .defaultMinSize(minHeight = minHeight)
                    .padding(bottom = inlineBottomInset)
            ) {
                SelectionContainer {
                    Text(
                        text = text,
                        style = textStyle,
                        color = textColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                MessageMetaContent(
                    msg = msg,
                    color = metaColor,
                    modifier = Modifier.offset {
                        IntOffset(
                            x = inlineMetaXOffsetPx,
                            y = inlineMetaYOffsetPx
                        )
                    }
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .defaultMinSize(minHeight = minHeight)
                    .widthIn(max = maxWidth)
            ) {
                SelectionContainer {
                    Text(
                        text = text,
                        style = textStyle,
                        color = textColor
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            end = MsgBubbleTokens.TimestampEnd,
                            bottom = MsgBubbleTokens.TimestampBottom
                        )
                ) {
                    MessageMetaContent(
                        msg = msg,
                        color = metaColor,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }
}

@Composable
private fun BubbleMetaAnchoredBox(
    msg: Message,
    metaColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        content()
        MessageMetaContent(
            msg = msg,
            color = metaColor,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = MsgBubbleTokens.TimestampEnd,
                    bottom = MsgBubbleTokens.TimestampBottom
                )
        )
    }
}

/** Собирает подпись времени и признака редактирования. */
private fun buildMessageMetaLabel(msg: Message): String {
    val formattedDate = formatMessageTime(msg.timestamp)
    return if (msg.isEdited) "edited $formattedDate" else formattedDate
}

/** Отрисовывает компактный блок времени и статуса отправки. */
@Composable
internal fun MessageMetaContent(
    msg: Message,
    color: Color,
    modifier: Modifier = Modifier
) {
    val metaLabel = remember(msg.isEdited, msg.timestamp) { buildMessageMetaLabel(msg) }

    Row(
        modifier = modifier,
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
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (msg.readStatus) Icons.Default.DoneAll else Icons.Default.Done,
                contentDescription = if (msg.readStatus) "Read" else "Sent",
                tint = color,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/** Бейдж метаданных поверх визуального медиа. */
@Composable
internal fun MediaMetaBadge(
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
private fun DetachedMediaMetaPill(
    msg: Message,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = formatMessageTime(msg.timestamp),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        ),
        color = contentColor,
        modifier = modifier
            .clip(RoundedCornerShape(MsgBubbleTokens.VisualMediaMetaCornerRadius))
            .background(backgroundColor)
            .padding(
                horizontal = MsgBubbleTokens.VisualMediaMetaHorizontalPadding,
                vertical = MsgBubbleTokens.VisualMediaMetaVerticalPadding
            )
    )
}

/** Превью сообщения, на которое сделан ответ. */
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
    val highlightColor = if (isOutgoing) {
        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(highlightColor)
            .padding(start = 8.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = replySender.displayName(defaultName = "Reply"),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = replyMessage.replyPreviewText(),
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 16.sp
                ),
                color = if (isOutgoing) {
                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f)
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Вычисляет форму пузыря в зависимости от направления и позиции в кластере. */
private fun msgBubbleShape(
    isOutgoing: Boolean,
    position: MsgBubbleClusterPosition,
    showChrome: Boolean
): RoundedCornerShape {
    if (!showChrome) return RoundedCornerShape(0.dp)
    return RoundedCornerShape(MsgBubbleTokens.BubbleCornerRadius)
}

/** Подбирает форму вложенного медиа с учётом текста сверху и снизу. */
private fun mediaContentShape(
    hasContentAbove: Boolean,
    hasContentBelow: Boolean
): RoundedCornerShape {
    val large = 12.dp
    val inner = 3.dp
    return RoundedCornerShape(
        topStart = if (hasContentAbove) inner else large,
        topEnd = if (hasContentAbove) inner else large,
        bottomEnd = if (hasContentBelow) inner else large,
        bottomStart = if (hasContentBelow) inner else large
    )
}

/** Определяет, нужно ли рисовать отдельный хромированный пузырь для типа сообщения. */
private fun MessageType.isDetachedBubble(): Boolean =
    this == MessageType.STICKER ||
        this == MessageType.ANIMATED_STICKER ||
        this == MessageType.VIDEO_STICKER ||
        this == MessageType.VIDEO_NOTE

/** Определяет, относится ли сообщение к стикерам. */
private fun MessageType.isStickerType(): Boolean =
    this == MessageType.STICKER ||
        this == MessageType.ANIMATED_STICKER ||
        this == MessageType.VIDEO_STICKER

/** Определяет, использует ли сообщение медиа-пузырь расширенного формата. */
private fun MessageType.isRichMediaBubble(): Boolean =
    this == MessageType.PHOTO ||
        this == MessageType.VIDEO ||
        this == MessageType.ANIMATION ||
        this == MessageType.LOCATION

/** Определяет, относится ли сообщение к визуальным медиа для overlay-метаданных. */
private fun MessageType.isVisualMediaBubble(): Boolean =
    this == MessageType.PHOTO ||
        this == MessageType.VIDEO ||
        this == MessageType.ANIMATION

/** Форматирует время сообщения для UI. */
private fun formatMessageTime(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
}

private fun visualMediaMetaColor(
    bubbleColor: Color,
    contentColor: Color
): Color = if (bubbleColor.luminance() > 0.60f) {
    contentColor.copy(alpha = 0.45f)
} else {
    Color.White.copy(alpha = 0.82f)
}

/** Возвращает отображаемое имя пользователя с fallback-значением. */
fun User?.displayName(defaultName: String = "Unknown"): String =
    listOfNotNull(this?.firstName, this?.lastName)
        .joinToString(" ")
        .ifBlank { defaultName }

/** Возвращает короткий текст для превью сообщения в блоке ответа. */
fun Message?.replyPreviewText(): String =
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
        latitude = null,
        longitude = null,
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
        avatarLocalPath = null
    )

    BotgramTheme {
        MsgBubble(msg = msg, sender = user)
    }
}
