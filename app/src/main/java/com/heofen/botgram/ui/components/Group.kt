package com.heofen.botgram.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
fun GroupScreenBar(
    chat: Chat,
    hazeState: HazeState,
    onBackClick: () -> Unit = {}
) {
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
                    .hazeEffect(
                        state = hazeState,
                        style = HazeStyle(
                            blurRadius = 40.dp,
                            tint = HazeTint(Color.White.copy(alpha = 0.4f)),
                            noiseFactor = 0.15f,
                        )
                    )
                    .clickable{onBackClick},
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Back",
                    modifier = Modifier
                        .size(30.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(50))
                    .hazeEffect(
                        state = hazeState,
                        style = HazeStyle(
                            blurRadius = 40.dp,
                            tint = HazeTint(Color.White.copy(alpha = 0.4f)),
                            noiseFactor = 0.15f,
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                val title: String = when {
                    chat.title != null -> chat.title
                    else -> (chat.firstName ?: "") + " " + (chat.lastName ?: "")
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    fontSize = 20.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .hazeEffect(
                        state = hazeState,
                        style = HazeStyle(
                            blurRadius = 40.dp,
                            tint = HazeTint(Color.White.copy(alpha = 0.4f)),
                            noiseFactor = 0.15f,
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                ChatAvatar(
                    chat = chat,
                    modifier = Modifier.size(50.dp)
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
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .background(
                    color = if (!msg.isOutgoing) Color(0xFF90CAF9) else Color.Gray,
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
                            color = Color.Black
                        )
                    } else {
                        Text(
                            text = "Unsupported message type",
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.weight(1f, fill = false),
                            color = Color.Black
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
                        color = Color.Gray
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