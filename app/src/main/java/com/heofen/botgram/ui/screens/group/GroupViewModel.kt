package com.heofen.botgram.ui.screens.group

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/** Готовый к отрисовке элемент списка сообщений: все соседние решения уже посчитаны. */
data class MessageRenderItem(
    val message: Message,
    val sender: User?,
    val replyToMessage: Message?,
    val replySender: User?,
    val clusterPosition: MsgBubbleClusterPosition,
    val showDateHeader: Boolean,
    val sendStatus: SendStatus? = null,
    /** Все сообщения медиагруппы (≥2), отсортированные от старых к новым. Null для одиночных сообщений. */
    val mediaGroupMessages: List<Message>? = null
)

/** Оптимистичное сообщение в процессе отправки или с ошибкой. */
data class PendingMessageEntry(
    val localId: String,
    val message: Message,
    val status: SendStatus
)

/** Состояние экрана переписки. */
data class GroupUiState(
    val chat: Chat? = null,
    val renderItems: List<MessageRenderItem> = emptyList(),
    val isLoading: Boolean = true,
    val messageText: String = "",
    val replyToMessageId: Long? = null,
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
    private val chatId: Long,
    private val chatRepository: ChatRepository,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val mediaLoadRequested = mutableSetOf<Pair<Long, Long>>()
    private val userAvatarLoadRequested = mutableSetOf<Long>()
    private var chatAvatarLoadRequested = false
    private var cachedUsers: Map<Long, User> = emptyMap()
    private var tempMessageIdCounter = -1L
    private val _sendingMessages = MutableStateFlow<List<PendingMessageEntry>>(emptyList())

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
        _uiState.update { it.copy(replyToMessageId = message.messageId) }
    }

    fun clearReplyMessage() {
        _uiState.update { it.copy(replyToMessageId = null) }
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

    /** Подписывается на чат, сообщения, пользователей и ленивую загрузку медиа. */
    private fun observeGroupData() {
        viewModelScope.launch {
            launch {
                chatRepository.observeById(chatId).collect { chat ->
                    _uiState.update { it.copy(chat = chat) }

                    if (chat != null && !chatAvatarLoadRequested) {
                        chatAvatarLoadRequested = true
                        launch(Dispatchers.IO) {
                            chatRepository.loadAvatarIfMissing(chatId)
                        }
                    }
                }
            }

            launch {
                combine(
                    messageRepository.getChatMessages(chatId),
                    _sendingMessages
                ) { messages, pending -> messages to pending }
                .collectLatest { (messages, pending) ->
                    // DAO уже отдаёт сообщения от новых к старым — под reverseLayout = true.
                    val users = loadUsersFor(messages)
                    val renderItems = withContext(Dispatchers.Default) {
                        buildRenderItems(messages, users, pending)
                    }

                    _uiState.update {
                        it.copy(
                            renderItems = renderItems,
                            isLoading = false
                        )
                    }

                    withContext(Dispatchers.IO) {
                        // Медиа и аватары догружаются в фоне, чтобы не задерживать первичную отрисовку.
                        messages.forEach { message ->
                            val key = message.chatId to message.messageId
                            if (mediaLoadRequested.add(key)) {
                                messageRepository.ensureMediaDownloaded(message)
                            }
                        }
                        users.keys.forEach { userId ->
                            if (userAvatarLoadRequested.add(userId)) {
                                userRepository.loadAvatarIfMissing(userId)
                            }
                        }
                    }
                }
            }
        }
    }

    /** Догружает только тех пользователей, которых ещё нет в кеше, одной выборкой. */
    private suspend fun loadUsersFor(messages: List<Message>): Map<Long, User> {
        val requiredIds = messages.mapNotNullTo(mutableSetOf()) { it.senderId }
        if (requiredIds.isEmpty()) {
            cachedUsers = emptyMap()
            return emptyMap()
        }

        val missingIds = requiredIds.filterNot { cachedUsers.containsKey(it) }
        val updatedCache = if (missingIds.isEmpty()) {
            cachedUsers
        } else {
            val fetched = withContext(Dispatchers.IO) { userRepository.getByIds(missingIds) }
            cachedUsers + fetched.associateBy { it.id }
        }
        // Удаляем из кеша пользователей, которых больше нет в активной переписке.
        val trimmed = if (updatedCache.keys == requiredIds) {
            updatedCache
        } else {
            updatedCache.filterKeys { it in requiredIds }
        }
        cachedUsers = trimmed
        return trimmed
    }

    /** Считает соседние отношения между сообщениями для UI — позиции кластеров и заголовки дат. */
    private fun buildRenderItems(
        messagesNewestFirst: List<Message>,
        users: Map<Long, User>,
        pendingEntries: List<PendingMessageEntry> = emptyList()
    ): List<MessageRenderItem> {
        // Pending-сообщения самые новые — идут первыми (reverseLayout=true отобразит их внизу).
        val pendingItems = pendingEntries.reversed().map { entry ->
            MessageRenderItem(
                message = entry.message,
                sender = null,
                replyToMessage = null,
                replySender = null,
                clusterPosition = MsgBubbleClusterPosition.Single,
                showDateHeader = false,
                sendStatus = entry.status
            )
        }

        if (messagesNewestFirst.isEmpty()) return pendingItems

        val messagesById = messagesNewestFirst.associateBy { it.messageId }
        val messageDays = LongArray(messagesNewestFirst.size) { i ->
            messageDayEpochDay(messagesNewestFirst[i].timestamp)
        }

        val dbItems = mutableListOf<MessageRenderItem>()
        var index = 0

        while (index < messagesNewestFirst.size) {
            val message = messagesNewestFirst[index]
            val mediaGroupId = message.mediaGroupId

            // Собираем все подряд идущие сообщения с тем же mediaGroupId.
            val groupSize: Int
            val mediaGroupMessages: List<Message>?
            if (mediaGroupId != null) {
                var j = index + 1
                while (j < messagesNewestFirst.size &&
                    messagesNewestFirst[j].mediaGroupId == mediaGroupId) {
                    j++
                }
                groupSize = j - index
                mediaGroupMessages = if (groupSize > 1) {
                    messagesNewestFirst.subList(index, j).sortedBy { it.timestamp }
                } else {
                    null
                }
            } else {
                groupSize = 1
                mediaGroupMessages = null
            }

            // older/newer соседи считаются уже за пределами всей группы.
            val effectiveOlderIndex = index + groupSize
            val effectiveNewerIndex = index - 1
            val olderMessage = messagesNewestFirst.getOrNull(effectiveOlderIndex)
            val newerMessage = messagesNewestFirst.getOrNull(effectiveNewerIndex)
            val currentDay = messageDays[index]
            val olderDay = if (effectiveOlderIndex < messageDays.size) messageDays[effectiveOlderIndex] else null

            val isGroupedWithOlder = olderMessage?.let {
                shouldClusterMessages(message, it, currentDay, olderDay ?: -1)
            } == true
            val isGroupedWithNewer = newerMessage?.let {
                shouldClusterMessages(message, it, currentDay, messageDays[effectiveNewerIndex])
            } == true
            val clusterPosition = when {
                isGroupedWithOlder && isGroupedWithNewer -> MsgBubbleClusterPosition.Middle
                isGroupedWithOlder -> MsgBubbleClusterPosition.Bottom
                isGroupedWithNewer -> MsgBubbleClusterPosition.Top
                else -> MsgBubbleClusterPosition.Single
            }
            val showDateHeader = olderDay == null || olderDay != currentDay

            val replyTo = message.replyMsgId?.let { messagesById[it] }
            dbItems.add(MessageRenderItem(
                message = message,
                sender = message.senderId?.let(users::get),
                replyToMessage = replyTo,
                replySender = replyTo?.senderId?.let(users::get),
                clusterPosition = clusterPosition,
                showDateHeader = showDateHeader,
                mediaGroupMessages = mediaGroupMessages
            ))

            index += groupSize
        }

        return pendingItems + dbItems
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

private const val MESSAGE_CLUSTER_WINDOW_MS = 5 * 60 * 1000L

/** Решает, нужно ли визуально склеить соседние сообщения в один кластер. */
private fun shouldClusterMessages(
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
private fun messageDayEpochDay(timestamp: Long): Long =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
