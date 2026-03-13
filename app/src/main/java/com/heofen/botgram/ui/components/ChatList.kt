package com.heofen.botgram.ui.components

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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
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
import com.heofen.botgram.database.tables.ChatListItem
import com.heofen.botgram.ui.theme.BotgramTheme
import com.heofen.botgram.ui.theme.botgramHazeStyle
import com.heofen.botgram.utils.extensions.getInitials
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import java.io.File
import java.time.Instant
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
    val title = when {
        !chat.title.isNullOrBlank() -> chat.title
        else -> listOfNotNull(chat.firstName, chat.lastName)
            .joinToString(" ")
            .ifBlank { "Unknown chat" }
    }
    val formattedTime = formatChatTime(chat.lastMessageTime)
    val preview = chat.previewText()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChatSellClick(chat.id) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChatAvatar(
            chat = chat,
            modifier = Modifier.size(54.dp)
        )

        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )

                if (formattedTime.isNotEmpty()) {
                    Text(
                        text = formattedTime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = preview,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (unreadedCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadedCount > 99) "99+" else unreadedCount.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private fun formatChatTime(timestampMillis: Long?): String {
    if (timestampMillis == null || timestampMillis <= 0L) return ""

    return runCatching {
        Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrDefault("")
}

private fun Chat.previewText(): String {
    val textPreview = lastMessageText?.trim().orEmpty()
    if (lastMessageType == MessageType.TEXT && textPreview.isNotEmpty()) {
        return textPreview
    }

    return when (lastMessageType) {
        MessageType.TEXT -> "Сообщение"
        MessageType.PHOTO -> "Фото"
        MessageType.VIDEO -> "Видео"
        MessageType.ANIMATION -> "GIF"
        MessageType.AUDIO -> "Аудио"
        MessageType.VOICE -> "Голосовое сообщение"
        MessageType.VIDEO_NOTE -> "Видеосообщение"
        MessageType.DOCUMENT -> "Документ"
        MessageType.STICKER,
        MessageType.ANIMATED_STICKER,
        MessageType.VIDEO_STICKER -> "Стикер"
        MessageType.CONTACT -> "Контакт"
        MessageType.LOCATION -> "Локация"
        null -> ""
        else -> "Сообщение"
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
