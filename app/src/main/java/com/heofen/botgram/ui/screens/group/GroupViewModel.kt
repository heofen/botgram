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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupUiState(
    val chat: Chat? = null,
    val messages: List<Message> = emptyList(),
    val users: Map<Long, User> = emptyMap(),
    val isLoading: Boolean = true,
    val messageText: String = "" // Added field for input text
)

class GroupViewModel(
    private val chatId: Long,
    private val chatRepository: ChatRepository,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    init {
        loadGroupData()
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

                messageRepository.sendTextMessage(chatId, text)


            } catch (e: Exception) {
                Log.e("GroupViewModel", "Failed to send message", e)
            }
        }
    }

    private fun loadGroupData() {
        viewModelScope.launch {
            val chat = chatRepository.getById(chatId)
            _uiState.update { it.copy(chat = chat) }

            if (chat != null) {
                launch(Dispatchers.IO) {
                    chatRepository.loadAvatarIfMissing(chatId)
                }
            }

            messageRepository.getChatMessages(chatId).collect { messages ->
                val userIds = messages.mapNotNull { it.senderId }.distinct()
                val users = mutableMapOf<Long, User>()

                userIds.forEach { userId ->
                    val user = userRepository.getById(userId)

                    if (user != null) {
                        users[userId] = user
                        viewModelScope.launch(Dispatchers.IO) {
                            userRepository.loadAvatarIfMissing(userId)
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        messages = messages,
                        users = users,
                        isLoading = false
                    )
                }
            }
        }
    }
}