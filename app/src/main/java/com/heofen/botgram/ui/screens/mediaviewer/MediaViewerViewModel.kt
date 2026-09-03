package com.heofen.botgram.ui.screens.mediaviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heofen.botgram.data.repository.MessageRepository
import com.heofen.botgram.data.repository.UserRepository
import com.heofen.botgram.database.tables.Message
import com.heofen.botgram.database.tables.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Состояние экрана просмотра медиа. */
data class MediaViewerUiState(
    val isLoading: Boolean = true,
    val mediaMessages: List<Message> = emptyList(),
    val initialIndex: Int = 0,
    val senders: Map<Long, User> = emptyMap()
)

/** ViewModel экрана просмотра медиафайлов. */
class MediaViewerViewModel(
    val chatId: Long,
    val initialMessageId: Long,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaViewerUiState())
    val uiState: StateFlow<MediaViewerUiState> = _uiState.asStateFlow()
    
    private val _senders = mutableMapOf<Long, User>()

    init {
        loadMedia()
    }

    private fun loadMedia() {
        viewModelScope.launch {
            messageRepository.getMediaMessagesForChat(chatId).collect { messages ->
                val initialIndex = messages.indexOfFirst { it.messageId == initialMessageId }.coerceAtLeast(0)
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        mediaMessages = messages,
                        initialIndex = initialIndex,
                        senders = _senders
                    )
                }
                
                // Подгружаем пользователей (отправителей)
                loadSenders(messages.mapNotNull { it.senderId }.distinct())
            }
        }
    }

    private fun loadSenders(userIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            val users = userRepository.getByIds(userIds)
            var changed = false
            for (user in users) {
                if (_senders[user.id] != user) {
                    _senders[user.id] = user
                    changed = true
                }
            }
            if (changed) {
                _uiState.update { it.copy(senders = _senders.toMap()) }
            }
        }
    }
}
