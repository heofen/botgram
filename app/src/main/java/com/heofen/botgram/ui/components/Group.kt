package com.heofen.botgram.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isPersonalMsg) {
            UserAvatar(
                user = sender,
                modifier = Modifier.size(40.dp)
            )

        }
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .background(
                    color = if (!msg.isOutgoing) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.secondaryContainer ,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column() {
                if (!isPersonalMsg) {
                    val name = (sender.firstName) + " " + (sender.lastName ?: "")
                    Text(
                        text = name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (!msg.isOutgoing) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (msg.type == MessageType.TEXT && msg.text != null) {
                        Text(
                            text = msg.text,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f, fill = false),
                            color = if (!msg.isOutgoing) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    } else {
                        Text(
                            text = "Unsupported message type",
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.weight(1f, fill = false),
                            color = if (!msg.isOutgoing) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    val instant = Instant.ofEpochMilli(msg.timestamp)
                    val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                    val formatter = DateTimeFormatter.ofPattern("HH:mm")
                    val formattedDate = dateTime.format(formatter)

                    Text(
                        text = formattedDate,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (!msg.isOutgoing) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}


@Preview()
@Composable
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