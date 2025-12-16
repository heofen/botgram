package com.heofen.botgram.ui.screens.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.viewModelFactory
import com.heofen.botgram.data.repository.ChatRepository
import com.heofen.botgram.database.tables.Chat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatListViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    val chatListState: StateFlow<List<Chat>> = chatRepository.getAllChats()
        .onEach { chats ->
            viewModelScope.launch(Dispatchers.IO) {
                chats.forEach { chat ->
                    chatRepository.loadAvatarIfMissing(chat.id)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}