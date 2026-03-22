package com.heofen.botgram.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heofen.botgram.ChatType
import com.heofen.botgram.data.repository.ChatRepository
import com.heofen.botgram.data.repository.UserRepository
import com.heofen.botgram.database.tables.Chat
import com.heofen.botgram.database.tables.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val chat: Chat? = null,
    val user: User? = null,
    val isLoading: Boolean = true
)

enum class ProfileTarget {
    CHAT,
    USER
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val target: ProfileTarget,
    private val profileId: Long,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private var chatAvatarLoadRequested = false
    private var userAvatarLoadRequested = false

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeProfile()
    }

    private fun observeProfile() {
        viewModelScope.launch {
            when (target) {
                ProfileTarget.CHAT -> {
                    chatRepository.observeById(profileId)
                        .flatMapLatest { chat ->
                            if (chat?.type == ChatType.PRIVATE) {
                                userRepository.observeById(chat.id).map { user -> chat to user }
                            } else {
                                flowOf(chat to null)
                            }
                        }
                        .collectLatest { (chat, user) ->
                            _uiState.update {
                                it.copy(
                                    chat = chat,
                                    user = user,
                                    isLoading = false
                                )
                            }

                            requestProfileAssets(chat, user)
                        }
                }

                ProfileTarget.USER -> {
                    userRepository.observeById(profileId)
                        .collectLatest { user ->
                            _uiState.update {
                                it.copy(
                                    chat = null,
                                    user = user,
                                    isLoading = false
                                )
                            }

                            requestProfileAssets(chat = null, user = user)
                        }
                }
            }
        }
    }

    private fun requestProfileAssets(chat: Chat?, user: User?) {
        if (chat != null && !chatAvatarLoadRequested) {
            chatAvatarLoadRequested = true
            viewModelScope.launch(Dispatchers.IO) {
                chatRepository.loadAvatarIfMissing(chat.id)
            }
        }

        val userId = user?.id ?: chat?.takeIf { it.type == ChatType.PRIVATE }?.id
        if (userId != null && !userAvatarLoadRequested) {
            userAvatarLoadRequested = true
            viewModelScope.launch(Dispatchers.IO) {
                userRepository.loadAvatarIfMissing(userId)
            }
        }
    }
}
