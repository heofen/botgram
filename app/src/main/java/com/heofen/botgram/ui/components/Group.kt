package com.heofen.botgram.ui.components

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBackIosNew
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.heofen.botgram.ChatType
import com.heofen.botgram.MessageType
import com.heofen.botgram.database.tables.Chat
import com.heofen.botgram.database.tables.Message
import com.heofen.botgram.database.tables.User
import com.heofen.botgram.ui.theme.BotgramTheme
import com.heofen.botgram.utils.extensions.getInitials
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.platform.LocalContext


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
    val islandStyle = HazeStyle(
        blurRadius = 20.dp,
        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        tint = HazeTint(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
        noiseFactor = 0.05f
    )

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
                .padding(horizontal = 12.dp, vertical = 8.dp),
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
    val islandStyle = HazeStyle(
        blurRadius = 20.dp,
        backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
        tint = HazeTint(MaterialTheme.colorScheme.surface.copy(alpha = 0.0f)),
        noiseFactor = 0.05f
    )

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

@Composable
fun MsgBubble(
    msg: Message,
    sender: User,
    isPersonalMsg: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        if (!isPersonalMsg && !msg.isOutgoing) {
            UserAvatar(
                user = sender,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .width(if (msg.type == MessageType.STICKER || msg.type == MessageType.ANIMATED_STICKER || msg.type == MessageType.VIDEO_STICKER || msg.type == MessageType.VIDEO_NOTE) 200.dp else 300.dp) // Ограничиваем ширину
                .wrapContentWidth(if (msg.isOutgoing) Alignment.End else Alignment.Start)
                .background(
                    color = if (msg.type == MessageType.STICKER || msg.type == MessageType.ANIMATED_STICKER || msg.type == MessageType.VIDEO_STICKER || msg.type == MessageType.VIDEO_NOTE)
                        Color.Transparent
                    else if (!msg.isOutgoing) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(
                    if (msg.type == MessageType.STICKER || msg.type == MessageType.ANIMATED_STICKER || msg.type == MessageType.VIDEO_STICKER || msg.type == MessageType.VIDEO_NOTE)
                        0.dp
                    else 8.dp
                )
        ) {
            Column(
                modifier = Modifier.wrapContentWidth()
            ) {
                if (!isPersonalMsg && !msg.isOutgoing) {
                    val name = (sender.firstName) + " " + (sender.lastName ?: "")
                    Text(
                        text = name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                when (msg.type) {
                    MessageType.TEXT -> {
                        msg.text?.let {
                            Text(
                                text = it,
                                fontSize = 16.sp,
                                color = if (!msg.isOutgoing) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    MessageType.PHOTO -> {
                        MediaMessage(
                            msg = msg,
                            icon = Icons.Default.Image,
                            label = "Photo",
                            isVideo = false
                        )
                    }
                    MessageType.VIDEO,
                    MessageType.ANIMATION -> {
                        VideoMessage(
                            msg = msg,
                            label = if (msg.type == MessageType.ANIMATION) "GIF" else "Video"
                        )
                    }
                    MessageType.VIDEO_NOTE -> VideoNoteMessage(msg)
                    MessageType.AUDIO -> AudioMessage(msg)
                    MessageType.VOICE -> VoiceMessage(msg)
                    MessageType.DOCUMENT -> DocumentMessage(msg)
                    MessageType.STICKER, MessageType.ANIMATED_STICKER, MessageType.VIDEO_STICKER -> StickerMessage(msg)
                    MessageType.CONTACT -> ContactMessage(msg)
                    MessageType.LOCATION -> LocationMessage(msg)
                    else -> {
                        Text(
                            text = "Unsupported message type: ${msg.type}",
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (msg.type != MessageType.TEXT && !msg.caption.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = msg.caption,
                        fontSize = 15.sp,
                        color = if (!msg.isOutgoing) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                val instant = Instant.ofEpochMilli(msg.timestamp)
                val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                val formattedDate = dateTime.format(formatter)

                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.align(Alignment.End),
                    color = if (msg.type == MessageType.STICKER || msg.type == MessageType.ANIMATED_STICKER || msg.type == MessageType.VIDEO_STICKER)
                        Color.Gray
                    else if (!msg.isOutgoing) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun MediaMessage(msg: Message, icon: ImageVector, label: String, isVideo: Boolean = false) {
    val file = msg.fileLocalPath?.let { File(it) }

    Log.d("BUBBLE", "msgId=${msg.messageId} type=${msg.type} path=${msg.fileLocalPath}")

    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))) {
        if (file != null && file.exists()) {

            Log.d("BUBBLE", "show media file=${file.absolutePath}")

            AsyncImage(
                model = file,
                contentDescription = label,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                contentScale = ContentScale.Crop
            )
            if (isVideo) {
                Box(
                    modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color.Gray.copy(alpha = 0.3f)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(48.dp), tint = Color.Gray)
                Text(text = label, color = Color.Gray)
                if (msg.fileSize != null) {
                    Text(text = formatFileSize(msg.fileSize), fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun VideoNoteMessage(msg: Message) {
    val file = msg.fileLocalPath?.let { File(it) }
    Box(
        modifier = Modifier
            .size(200.dp)
            .clip(CircleShape)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (file != null && file.exists()) {
            AsyncImage(
                model = file,
                contentDescription = "Video Note",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(48.dp))
        } else {
            Icon(Icons.Default.Videocam, contentDescription = "Video Note", tint = Color.White)
        }
    }
}

@Composable
fun StickerMessage(msg: Message) {
    val file = msg.fileLocalPath?.let { File(it) }
    if (file != null && file.exists()) {
        AsyncImage(
            model = file,
            contentDescription = "Sticker",
            modifier = Modifier
                .size(180.dp)
                .padding(4.dp)
        )
    } else {
        Icon(
            imageVector = Icons.Default.BrokenImage,
            contentDescription = "Sticker placeholder",
            modifier = Modifier.size(100.dp),
            tint = Color.Gray
        )
    }
}

@Composable
fun AudioMessage(msg: Message) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Audiotrack, contentDescription = "Audio", tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = msg.fileName ?: "Audio",
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatDuration(msg.duration) + " • " + formatFileSize(msg.fileSize),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun VoiceMessage(msg: Message) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Icon(Icons.Default.Mic, contentDescription = "Voice", modifier = Modifier.size(16.dp), tint = Color.Gray)
            Text(
                text = formatDuration(msg.duration),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun DocumentMessage(msg: Message) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = "File", tint = MaterialTheme.colorScheme.tertiary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = msg.fileName ?: "Document",
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatFileSize(msg.fileSize),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ContactMessage(msg: Message) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.ContactPage, contentDescription = "Contact", modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = "Contact", fontWeight = FontWeight.Bold)
            Text(text = msg.text ?: "User", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun LocationMessage(msg: Message) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = Color.Red, modifier = Modifier.size(48.dp))
        }
        Text(text = "Location", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
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
    label: String = "Video"
) {
    val context = LocalContext.current
    val file = msg.fileLocalPath?.let { File(it) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(enabled = file != null && file.exists()) {
                if (file != null && file.exists()) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        context.packageName + ".fileprovider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "video/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Text(text = label, color = Color.White)

            msg.fileSize?.let {
                Text(
                    text = formatFileSize(it),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
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
