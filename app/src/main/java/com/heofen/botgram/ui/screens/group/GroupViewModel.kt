package com.heofen.botgram.ui.screens.group

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.insertHeaderItem
import androidx.paging.insertSeparators
import androidx.paging.map
import com.heofen.botgram.data.local.ActiveChatTracker
import com.heofen.botgram.data.remote.OutgoingVisualMedia
import com.heofen.botgram.data.repository.ChatRepository
import com.heofen.botgram.data.repository.MessageRepository
import com.heofen.botgram.data.repository.UserRepository
import com.heofen.botgram.database.tables.Chat
import com.heofen.botgram.database.tables.Message
import com.heofen.botgram.database.tables.User
import com.heofen.botgram.MessageType
import com.heofen.botgram.ui.components.MsgBubbleClusterPosition
import com.heofen.botgram.ui.components.SendStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/** Элемент списка сообщений для UI (сообщение или заголовок с датой). */
sealed class MessageUiModel {
    data class MessageItem(
        val message: Message,
        val sender: User?,
        val replyToMessage: Message?,
        val replySender: User?,
        val sendStatus: SendStatus? = null,
        val mediaGroupMessages: List<Message>? = null
    ) : MessageUiModel() {
        val messageId: Long get() = message.messageId
        val chatId: Long get() = message.chatId
    }

    data class DateHeader(
        val date: LocalDate,
        val timestamp: Long
    ) : MessageUiModel() {
        val epochDay: Long get() = date.toEpochDay()
    }
}

/** Оптимистичное сообщение в процессе отправки или с ошибкой. */
data class PendingMessageEntry(
    val localId: String,
    val message: Message,
    val status: SendStatus
)

/** Состояние экрана переписки. */
data class GroupUiState(
    val chat: Chat? = null,
    val isLoading: Boolean = true,
    val messageText: String = "",
    val replyToMessageId: Long? = null,
    val replyMessage: Message? = null,
    val replySender: User? = null,
    val pendingMedia: List<ComposerMediaItem> = emptyList()
)

/** Медиафайл, выбранный пользователем перед отправкой. */
data class ComposerMediaItem(
    val localPath: String,
    val mimeType: String,
    val fileName: String
)

/** ViewModel экрана переписки: загрузка истории, отправка сообщений и удаление. */
class GroupViewModel(
    val chatId: Long,
    private val chatRepository: ChatRepository,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    val activeChatTracker: ActiveChatTracker
) : ViewModel() {
    private val mediaLoadRequested = java.util.concurrent.ConcurrentHashMap.newKeySet<Pair<Long, Long>>()
    private val userAvatarLoadRequested = java.util.concurrent.ConcurrentHashMap.newKeySet<Long>()
    private var chatAvatarLoadRequested = false
    private var tempMessageIdCounter = -1L
    private val _sendingMessages = MutableStateFlow<List<PendingMessageEntry>>(emptyList())
    val sendingMessages: StateFlow<List<PendingMessageEntry>> = _sendingMessages.asStateFlow()

    private val _users = MutableStateFlow<Map<Long, User>>(emptyMap())
    val users: StateFlow<Map<Long, User>> = _users.asStateFlow()

    val messagesFlow: Flow<PagingData<MessageUiModel>> = Pager<Int, Message>(
        config = PagingConfig(
            pageSize = 40,
            initialLoadSize = 40,
            prefetchDistance = 20,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { messageRepository.getChatMessagesPaging(chatId) }
    ).flow
    .map { pagingData: PagingData<Message> ->
        val mappedData = pagingData.map { message: Message ->
            val sender = message.senderId?.let { userId ->
                val cached = _users.value[userId]
                val user = cached ?: userRepository.getById(userId)
                if (user != null && cached == null) {
                    _users.update { it + (userId to user) }
                }
                if (userAvatarLoadRequested.add(userId)) {
                    viewModelScope.launch(Dispatchers.IO) {
                        userRepository.loadAvatarIfMissing(userId)
                        val updated = userRepository.getById(userId)
                        if (updated != null) {
                            _users.update { it + (userId to updated) }
                        }
                    }
                }
                user
            }

            val replyTo = message.replyMsgId?.let { messageRepository.getMessage(chatId, it) }
            val replySender = replyTo?.senderId?.let { userId ->
                val cached = _users.value[userId]
                val user = cached ?: userRepository.getById(userId)
                if (user != null && cached == null) {
                    _users.update { it + (userId to user) }
                }
                user
            }

            val mediaGroupMessages = message.mediaGroupId?.let { groupId ->
                val list = messageRepository.getMediaGroupList(groupId)
                if (list.size > 1) list else null
            }

            val key = message.chatId to message.messageId
            if (mediaLoadRequested.add(key)) {
                viewModelScope.launch(Dispatchers.IO) {
                    messageRepository.ensureMediaDownloaded(message)
                }
            }

            MessageUiModel.MessageItem(
                message = message,
                sender = sender,
                replyToMessage = replyTo,
                replySender = replySender,
                mediaGroupMessages = mediaGroupMessages
            )
        }

        val filteredData = mappedData.filter { item ->
            val mediaGroupId = item.message.mediaGroupId
            if (mediaGroupId != null && item.mediaGroupMessages != null) {
                val representativeId = item.mediaGroupMessages.lastOrNull()?.messageId
                item.message.messageId == representativeId
            } else {
                true
            }
        }

        filteredData.insertSeparators { before, after ->
            if (after == null) {
                if (before != null) {
                    val date = Instant.ofEpochMilli(before.message.timestamp)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    MessageUiModel.DateHeader(date = date, timestamp = before.message.timestamp)
                } else {
                    null
                }
            } else if (before != null) {
                val beforeDay = messageDayEpochDay(before.message.timestamp)
                val afterDay = messageDayEpochDay(after.message.timestamp)
                if (beforeDay != afterDay) {
                    val date = Instant.ofEpochMilli(before.message.timestamp)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    MessageUiModel.DateHeader(date = date, timestamp = before.message.timestamp)
                } else {
                    null
                }
            } else {
                null
            }
        }
    }.cachedIn(viewModelScope)

    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()


    init {
        observeGroupData()
    }

    /** Обновляет текст в поле ввода сообщения. */
    fun onMessageChange(text: String) {
        _uiState.update { it.copy(messageText = text) }
    }

    fun selectReplyMessage(message: Message) {
        viewModelScope.launch {
            val sender = message.senderId?.let { userId ->
                _users.value[userId] ?: userRepository.getById(userId)?.also { user ->
                    _users.update { it + (userId to user) }
                }
            }
            _uiState.update {
                it.copy(
                    replyToMessageId = message.messageId,
                    replyMessage = message,
                    replySender = sender
                )
            }
        }
    }

    fun clearReplyMessage() {
        _uiState.update {
            it.copy(
                replyToMessageId = null,
                replyMessage = null,
                replySender = null
            )
        }
    }

    fun sendMessage() {
        val state = _uiState.value
        val text = state.messageText.trim()
        val replyToMessageId = state.replyToMessageId
        val pendingMedia = state.pendingMedia
        if (text.isBlank() && pendingMedia.isEmpty()) return

        if (pendingMedia.isNotEmpty()) {
            sendMediaMessages(text.ifBlank { null }, pendingMedia.toList(), replyToMessageId)
        } else {
            sendTextOptimistic(text, replyToMessageId)
        }
    }

    private fun sendTextOptimistic(text: String, replyToMessageId: Long?) {
        val localId = System.nanoTime().toString()
        val tempId = tempMessageIdCounter--
        val now = System.currentTimeMillis()
        val tempMessage = Message(
            messageId = tempId,
            chatId = chatId,
            topicId = null,
            senderId = null,
            type = MessageType.TEXT,
            timestamp = now,
            text = text,
            caption = null,
            replyMsgId = replyToMessageId,
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
            readStatus = false,
            isOutgoing = true
        )

        _uiState.update { it.copy(messageText = "", replyToMessageId = null) }
        _sendingMessages.update { it + PendingMessageEntry(localId, tempMessage, SendStatus.SENDING) }

        viewModelScope.launch {
            val sentMessage = messageRepository.sendTextMessage(
                chatId = chatId,
                text = text,
                replyToMessageId = replyToMessageId
            )
            if (sentMessage != null) {
                _sendingMessages.update { list -> list.filterNot { it.localId == localId } }
                chatRepository.updateLastMessage(
                    chatId = sentMessage.chatId,
                    type = sentMessage.type,
                    text = sentMessage.text ?: sentMessage.caption,
                    time = sentMessage.timestamp,
                    senderId = sentMessage.senderId
                )
            } else {
                _sendingMessages.update { list ->
                    list.map { if (it.localId == localId) it.copy(status = SendStatus.FAILED) else it }
                }
            }
        }
    }

    private fun sendMediaMessages(caption: String?, mediaItems: List<ComposerMediaItem>, replyToMessageId: Long?) {
        _uiState.update { it.copy(messageText = "", replyToMessageId = null, pendingMedia = emptyList()) }
        viewModelScope.launch {
            try {
                val sentMessages = messageRepository.sendVisualMediaMessages(
                    chatId = chatId,
                    media = mediaItems.map {
                        OutgoingVisualMedia(file = File(it.localPath), mimeType = it.mimeType)
                    },
                    caption = caption,
                    replyToMessageId = replyToMessageId
                )
                sentMessages.lastOrNull()?.let { lastSentMessage ->
                    chatRepository.updateLastMessage(
                        chatId = lastSentMessage.chatId,
                        type = lastSentMessage.type,
                        text = lastSentMessage.text ?: lastSentMessage.caption,
                        time = lastSentMessage.timestamp,
                        senderId = lastSentMessage.senderId
                    )
                }
            } catch (e: Exception) {
                Log.e("GroupViewModel", "Failed to send media", e)
            }
        }
    }

    suspend fun sendLocation(latitude: Double, longitude: Double): Boolean {
        val replyToMessageId = _uiState.value.replyToMessageId

        return try {
            val sentMessage = messageRepository.sendLocationMessage(
                chatId = chatId,
                latitude = latitude,
                longitude = longitude,
                replyToMessageId = replyToMessageId
            )
            if (sentMessage != null) {
                _uiState.update {
                    it.copy(
                        messageText = "",
                        replyToMessageId = null
                    )
                }
                chatRepository.updateLastMessage(
                    chatId = sentMessage.chatId,
                    type = sentMessage.type,
                    text = sentMessage.text ?: sentMessage.caption,
                    time = sentMessage.timestamp,
                    senderId = sentMessage.senderId
                )
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("GroupViewModel", "Failed to send location", e)
            false
        }
    }

    suspend fun sendDocument(localPath: String, mimeType: String): Boolean {
        val state = _uiState.value
        val caption = state.messageText.trim().ifBlank { null }
        val replyToMessageId = state.replyToMessageId

        return try {
            val sentMessage = messageRepository.sendDocumentMessage(
                chatId = chatId,
                localFile = File(localPath),
                mimeType = mimeType,
                caption = caption,
                replyToMessageId = replyToMessageId
            )
            if (sentMessage != null) {
                _uiState.update {
                    it.copy(
                        messageText = "",
                        replyToMessageId = null
                    )
                }
                chatRepository.updateLastMessage(
                    chatId = sentMessage.chatId,
                    type = sentMessage.type,
                    text = sentMessage.text ?: sentMessage.caption ?: sentMessage.fileName,
                    time = sentMessage.timestamp,
                    senderId = sentMessage.senderId
                )
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("GroupViewModel", "Failed to send document", e)
            false
        }
    }

    fun addPendingMedia(items: List<ComposerMediaItem>) {
        if (items.isEmpty()) return
        _uiState.update { state ->
            // Исключаем дубликаты по локальному пути файла.
            val existing = state.pendingMedia.mapTo(mutableSetOf()) { it.localPath }
            state.copy(
                pendingMedia = state.pendingMedia + items.filter { existing.add(it.localPath) }
            )
        }
    }

    fun removePendingMedia(localPath: String) {
        _uiState.update { state ->
            state.copy(
                pendingMedia = state.pendingMedia.filterNot { it.localPath == localPath }
            )
        }
    }

    fun deleteMessageForMe(message: Message) {
        viewModelScope.launch {
            try {
                messageRepository.deleteMessageForMe(message.chatId, message.messageId)
                refreshLastMessage()
            } catch (e: Exception) {
                Log.e("GroupViewModel", "Failed to delete message locally", e)
            }
        }
    }

    fun deleteMessageForEveryone(message: Message) {
        viewModelScope.launch {
            try {
                val deleted = messageRepository.deleteMessageForEveryone(
                    chatId = message.chatId,
                    messageId = message.messageId
                )
                if (deleted) {
                    refreshLastMessage()
                } else {
                    Log.w("GroupViewModel", "Message was not deleted for everyone: ${message.messageId}")
                }
            } catch (e: Exception) {
                Log.e("GroupViewModel", "Failed to delete message for everyone", e)
            }
        }
    }

    /** Подписывается на чат. */
    private fun observeGroupData() {
        viewModelScope.launch {
            launch {
                chatRepository.observeById(chatId).collect { chat ->
                    _uiState.update { it.copy(chat = chat, isLoading = false) }

                    if (chat != null && !chatAvatarLoadRequested) {
                        chatAvatarLoadRequested = true
                        launch(Dispatchers.IO) {
                            chatRepository.loadAvatarIfMissing(chatId)
                        }
                    }
                }
            }
        }
    }

    /** Пересчитывает summary последнего сообщения после удаления из истории. */
    private suspend fun refreshLastMessage() {
        val lastMessage = messageRepository.getLastMessage(chatId)
        chatRepository.updateLastMessage(
            chatId = chatId,
            type = lastMessage?.type,
            text = lastMessage?.text ?: lastMessage?.caption,
            time = lastMessage?.timestamp,
            senderId = lastMessage?.senderId
        )
    }
}

internal const val MESSAGE_CLUSTER_WINDOW_MS = 5 * 60 * 1000L

/** Решает, нужно ли визуально склеить соседние сообщения в один кластер. */
internal fun shouldClusterMessages(
    current: Message,
    neighbour: Message,
    currentDay: Long,
    neighbourDay: Long
): Boolean {
    if (current.isOutgoing != neighbour.isOutgoing) return false
    if (current.senderId != neighbour.senderId) return false
    if (currentDay != neighbourDay) return false

    return abs(current.timestamp - neighbour.timestamp) <= MESSAGE_CLUSTER_WINDOW_MS
}

/** Возвращает день сообщения как количество дней с эпохи — дешевле, чем сравнивать LocalDate. */
internal fun messageDayEpochDay(timestamp: Long): Long =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
