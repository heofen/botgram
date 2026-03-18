package com.heofen.botgram.ui.screens.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heofen.botgram.data.repository.ChatRepository
import com.heofen.botgram.database.tables.ChatListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel списка чатов: поиск, подгрузка аватаров и поток элементов списка. */
class ChatListViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {
    private val avatarLoadRequested = mutableSetOf<Long>()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive = _isSearchActive.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val chatListState: StateFlow<List<ChatListItem>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                chatRepository.getAllChats()
            } else {
                chatRepository.searchChats(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            chatListState.collectLatest { chats ->
                // Ленивая догрузка аватаров только для первых видимых/актуальных элементов списка.
                chats.take(20).forEach { chat ->
                    if (avatarLoadRequested.add(chat.chat.id)) {
                        chatRepository.loadAvatarIfMissing(chat.chat.id)
                    }
                }
            }
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun toggleSearch(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }
}
