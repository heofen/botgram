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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class GroupUiState(
    val chat: Chat? = null,
    val messages: List<Message> = emptyList(),
    val users: Map<Long, User> = emptyMap(),
    val isLoading: Boolean = true,
    val messageText: String = "",
    val replyToMessageId: Long? = null,
    val pendingMedia: List<ComposerMediaItem> = emptyList()
)

data class ComposerMediaItem(
    val localPath: String,
    val mimeType: String,
    val fileName: String
)

class GroupViewModel(
    private val chatId: Long,
    private val chatRepository: ChatRepository,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val mediaLoadRequested = mutableSetOf<Pair<Long, Long>>()
    private val userAvatarLoadRequested = mutableSetOf<Long>()
    private var chatAvatarLoadRequested = false

    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    init {
        observeGroupData()
    }

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

        viewModelScope.launch {
            try {
                if (pendingMedia.isNotEmpty()) {
                    val sentMessages = messageRepository.sendVisualMediaMessages(
                        chatId = chatId,
                        media = pendingMedia.map {
                            OutgoingVisualMedia(
                                file = File(it.localPath),
                                mimeType = it.mimeType
                            )
                        },
                        caption = text.ifBlank { null },
                        replyToMessageId = replyToMessageId
                    )
                    val lastSentMessage = sentMessages.lastOrNull()
                    if (lastSentMessage != null) {
                        _uiState.update {
                            it.copy(
                                messageText = "",
                                replyToMessageId = null,
                                pendingMedia = emptyList()
                            )
                        }
                        chatRepository.updateLastMessage(
                            chatId = lastSentMessage.chatId,
                            type = lastSentMessage.type,
                            text = lastSentMessage.text ?: lastSentMessage.caption,
                            time = lastSentMessage.timestamp,
                            senderId = lastSentMessage.senderId
                        )
                    }
                } else {
                    val sentMessage = messageRepository.sendTextMessage(
                        chatId = chatId,
                        text = text,
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
                    }
                }
            } catch (e: Exception) {
                Log.e("GroupViewModel", "Failed to send message", e)
            }
        }
    }

    fun addPendingMedia(items: List<ComposerMediaItem>) {
        if (items.isEmpty()) return
        _uiState.update { state ->
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
                messageRepository.getChatMessages(chatId).collectLatest { messages ->
                    val orderedMessages = messages.asReversed()
                    val userIds = messages.mapNotNull { it.senderId }.distinct()
                    val users = withContext(Dispatchers.IO) {
                        val loaded = mutableMapOf<Long, User>()
                        userIds.forEach { userId ->
                            userRepository.getById(userId)?.let { user ->
                                loaded[userId] = user
                            }
                        }
                        loaded
                    }

                    _uiState.update {
                        it.copy(
                            messages = orderedMessages,
                            users = users,
                            isLoading = false
                        )
                    }

                    withContext(Dispatchers.IO) {
                        messages.forEach { message ->
                            val key = message.chatId to message.messageId
                            if (mediaLoadRequested.add(key)) {
                                messageRepository.ensureMediaDownloaded(message)
                            }
                        }
                        userIds.forEach { userId ->
                            if (userAvatarLoadRequested.add(userId)) {
                                userRepository.loadAvatarIfMissing(userId)
                            }
                        }
                    }
                }
            }
        }
    }

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
