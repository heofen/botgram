package com.heofen.botgram.ui.screens.group

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class GroupUiState(
    val chat: Chat? = null,
    val messages: List<Message> = emptyList(),
    val users: Map<Long, User> = emptyMap(),
    val isLoading: Boolean = true,
    val messageText: String = ""
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

    fun sendMessage() {
        val text = _uiState.value.messageText
        if (text.isBlank()) return

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(messageText = "") }

                val sentMessage = messageRepository.sendTextMessage(chatId, text)
                if (sentMessage != null) {
                    chatRepository.updateLastMessage(
                        chatId = sentMessage.chatId,
                        type = sentMessage.type,
                        text = sentMessage.text ?: sentMessage.caption,
                        time = sentMessage.timestamp,
                        senderId = sentMessage.senderId
                    )
                }

            } catch (e: Exception) {
                Log.e("GroupViewModel", "Failed to send message", e)
            }
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
