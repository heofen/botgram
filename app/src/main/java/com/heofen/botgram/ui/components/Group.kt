package com.heofen.botgram.ui.components

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.util.Size
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.heofen.botgram.ChatType
import com.heofen.botgram.MessageType
import com.heofen.botgram.R
import com.heofen.botgram.database.tables.Chat
import com.heofen.botgram.database.tables.Message
import com.heofen.botgram.database.tables.User
import com.heofen.botgram.ui.screens.group.ComposerMediaItem
import com.heofen.botgram.ui.theme.BotgramTheme
import com.heofen.botgram.ui.theme.BotgramBackdrop
import com.heofen.botgram.ui.theme.botgramLiquidGlass
import com.heofen.botgram.ui.theme.rememberBotgramBackdrop
import com.heofen.botgram.utils.extensions.getInitials
import java.io.File

/** Отрисовывает аватар пользователя или fallback по инициалам. */
@Composable
fun UserAvatar(
    user: User?,
    modifier: Modifier = Modifier,
    fallbackColor: Color = Color.Gray
) {
    val model = user?.avatarLocalPath?.let(::File)

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
            Text(
                text = user?.getInitials() ?: "?",
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

/** Поле ввода сообщения с ответом, вложениями и кнопками отправки. */
@Composable
fun MessageInput(
    modifier: Modifier = Modifier,
    text: String,
    backdrop: BotgramBackdrop,
    replyMessage: Message? = null,
    replySender: User? = null,
    pendingMedia: List<ComposerMediaItem> = emptyList(),
    onTextChange: (String) -> Unit,
    onAttachmentClick: () -> Unit = {},
    onSendClick: () -> Unit,
    onRemovePendingMedia: (String) -> Unit = {},
    onCancelReply: () -> Unit = {}
) {
    val inputShape = RoundedCornerShape(30.dp)
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 18.sp,
        lineHeight = 26.sp
    )
    val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
    val attachmentTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
    val canSend = text.isNotBlank() || pendingMedia.isNotEmpty()
    val sendContainerColor = MaterialTheme.colorScheme.primary.copy(
        alpha = if (canSend) 1f else 0.42f
    )
    val sendIconColor = MaterialTheme.colorScheme.onPrimary.copy(
        alpha = if (canSend) 1f else 0.72f
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessHigh
                ),
                alignment = Alignment.BottomCenter
            )
            .botgramLiquidGlass(backdrop = backdrop, shape = inputShape)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (replyMessage != null || pendingMedia.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, top = 10.dp, end = 10.dp)
                ) {
                    if (replyMessage != null) {
                        ReplyComposerPreview(
                            replyMessage = replyMessage,
                            replySender = replySender,
                            onCancelReply = onCancelReply
                        )
                    }

                    if (replyMessage != null && pendingMedia.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (pendingMedia.isNotEmpty()) {
                        PendingMediaStrip(
                            items = pendingMedia,
                            onRemove = onRemovePendingMedia
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth()
                        .padding(start = 60.dp, top = 10.dp, end = 60.dp, bottom = 10.dp)
                        .wrapContentHeight()
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = stringResource(R.string.message_input_placeholder),
                            color = placeholderColor,
                            style = textStyle
                        )
                    }

                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        minLines = 1,
                        maxLines = 7,
                        textStyle = textStyle,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 10.dp, bottom = 10.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onAttachmentClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_attachment_compose),
                        contentDescription = stringResource(R.string.action_open_attachments),
                        modifier = Modifier.size(28.dp),
                        tint = attachmentTint
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 10.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(sendContainerColor)
                        .clickable(enabled = canSend, onClick = onSendClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_message_input_send_arrow),
                        contentDescription = stringResource(R.string.action_send),
                        modifier = Modifier.size(20.dp),
                        tint = sendIconColor
                    )
                }
            }
        }
    }
}

/** Горизонтальная лента ещё не отправленных медиафайлов. */
@Composable
private fun PendingMediaStrip(
    items: List<ComposerMediaItem>,
    onRemove: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            PendingMediaPreview(
                item = item,
                onRemove = { onRemove(item.localPath) }
            )
        }
    }
}

/** Миниатюра выбранного медиафайла с кнопкой удаления. */
@Composable
private fun PendingMediaPreview(
    item: ComposerMediaItem,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val file = remember(item.localPath) { File(item.localPath) }
    val videoThumbnail = rememberVideoThumbnail(
        file = file,
        enabled = item.mimeType.startsWith("video/")
    )

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.34f))
    ) {
        if (videoThumbnail != null) {
            Image(
                bitmap = videoThumbnail.asImageBitmap(),
                contentDescription = item.fileName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            AsyncImage(
                model = file,
                contentDescription = item.fileName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        if (item.mimeType.startsWith("video/")) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Video",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp),
            colors = IconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.45f),
                contentColor = Color.White,
                disabledContainerColor = Color.Black.copy(alpha = 0.25f),
                disabledContentColor = Color.White.copy(alpha = 0.38f)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.action_remove_media),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun rememberVideoThumbnail(
    file: File,
    enabled: Boolean,
    sizePx: Int = 512
): Bitmap? {
    val context = LocalContext.current
    val thumbnailState = produceState<Bitmap?>(
        initialValue = null,
        context,
        file.absolutePath,
        enabled,
        sizePx
    ) {
        value = if (!enabled) {
            null
        } else {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    ThumbnailUtils.createVideoThumbnail(file, Size(sizePx, sizePx), null)
                }.getOrNull()
            }
        }
    }
    return thumbnailState.value
}

/** Превью сообщения, на которое пользователь сейчас отвечает. */
@Composable
private fun ReplyComposerPreview(
    replyMessage: Message,
    replySender: User?,
    onCancelReply: () -> Unit
) {
    val accentColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.24f))
            .padding(start = 10.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(accentColor)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = replySender.displayName(defaultName = stringResource(R.string.message_reply_title)),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = replyMessage.replyPreviewText(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onCancelReply,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.action_cancel_reply),
            )
        }
    }
}

/** Верхняя панель переписки с названием чата и кнопкой назад. */
@Composable
fun GroupScreenBar(
    chat: Chat,
    backdrop: BotgramBackdrop,
    onBackClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {}
) {
    val actionShape = CircleShape
    val titleShape = RoundedCornerShape(50.dp)
    val avatarClickable = chat.type == ChatType.PRIVATE

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
                    .botgramLiquidGlass(backdrop = backdrop, shape = actionShape)
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = stringResource(R.string.action_back),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .botgramLiquidGlass(backdrop = backdrop, shape = titleShape)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chat.displayTitle(),
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
                    .clip(actionShape)
                    .botgramLiquidGlass(backdrop = backdrop, shape = actionShape)
                    .clickable(enabled = avatarClickable, onClick = onAvatarClick),
                contentAlignment = Alignment.Center
            ) {
                ChatAvatar(
                    chat = chat,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                )
            }
        }
    }
}

/** Возвращает лучшее доступное отображаемое название чата. */
private fun Chat.displayTitle(): String =
    title ?: listOfNotNull(firstName, lastName).joinToString(" ").ifBlank { "" }

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
    val backdrop = rememberBotgramBackdrop()

    BotgramTheme {
        GroupScreenBar(
            chat = chat,
            backdrop = backdrop
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageInputPreview() {
    val backdrop = rememberBotgramBackdrop()

    BotgramTheme {
        MessageInput(
            text = "",
            backdrop = backdrop,
            pendingMedia = emptyList(),
            onTextChange = {},
            onAttachmentClick = {},
            onSendClick = {},
            onRemovePendingMedia = {}
        )
    }
}
