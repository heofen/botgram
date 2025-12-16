package com.heofen.botgram.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Text
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heofen.botgram.ChatType
import com.heofen.botgram.MessageType
import com.heofen.botgram.database.tables.Chat
import com.heofen.botgram.ui.theme.BotgramTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.heofen.botgram.utils.extensions.getInitials
import dev.chrisbanes.haze.hazeSource
import java.io.File

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
    onMenuClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
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
                    .clickable { onMenuClick },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
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
                Text(
                    text = "Botgram",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
                    )
                    .clickable { onSearchClick },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier
                        .size(30.dp)
                )
            }
        }
    }
}

@Composable
fun ChatCell(
    chat: Chat,
    unreadedCount: Int = 0,
    onChatSellClick: (Long) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .drawBehind {
                drawLine(
                    color = Color.Black,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color.Black,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .background(color = MaterialTheme.colorScheme.background)
            .fillMaxWidth(1f)
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
                    modifier = Modifier.weight(1f)
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
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (chat.lastMessageType == MessageType.TEXT) {
                    val preview = chat.lastMessageText
                        ?: "⚠\uFE0F Тут явно что-то пошло не по плану"

                    Text(
                        text = preview,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.weight(1f)
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
//                Box(
//                    modifier = Modifier
//                        .size(24.dp)
//                        .clip(CircleShape)
//                        .background(Color.Green),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text(
//                        text = chat.unreadCount.toString(),
//                        color = Color.White,
//                        fontSize = 14.sp
//                    )
//                }
            }
        }
    }
}

@Composable
@Preview
fun ChatCellPreview() {
    BotgramTheme() {
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
        ChatCell(chat)
    }
}


@Preview(showBackground = true)
@Composable
fun ChatListScreenBarPreview() {
    MaterialTheme {
        val hazeState = remember { HazeState() }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState),
                contentPadding = PaddingValues(top = 90.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(10) { index ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Chat Item #$index")
                        }
                    }
                }
            }

            ChatListScreenBar(
                title = "Котобот",
                hazeState = hazeState,
                onMenuClick = { },
                onSearchClick = { },
            )
        }
    }
}
