package com.heofen.botgram.di

import android.content.Context
import com.heofen.botgram.data.auth.TelegramTokenValidator
import com.heofen.botgram.data.MediaManager
import com.heofen.botgram.data.local.TokenManager
import com.heofen.botgram.data.remote.TelegramGateway
import com.heofen.botgram.data.remote.TelegramGatewayFactory
import com.heofen.botgram.data.repository.ChatRepository
import com.heofen.botgram.data.repository.MessageRepository
import com.heofen.botgram.data.repository.UserRepository
import com.heofen.botgram.database.AppDatabase
import com.heofen.botgram.ui.screens.chatlist.ChatListViewModel
import com.heofen.botgram.ui.screens.group.GroupViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

object AppModule {
    fun provideDatabase(context: Context): AppDatabase =
        AppDatabase.getDatabase(context)

    fun provideTokenManager(context: Context): TokenManager =
        TokenManager(context)

    fun provideTelegramTokenValidator(): TelegramTokenValidator =
        TelegramTokenValidator()

    fun provideTelegramGateway(context: Context, token: String): TelegramGateway =
        TelegramGatewayFactory.create(context, token)

    fun provideMediaManager(gateway: TelegramGateway): MediaManager =
        MediaManager(gateway)

    fun provideChatRepository(
        database: AppDatabase,
        mediaManager: MediaManager
    ): ChatRepository = ChatRepository(database.chatDao(), mediaManager)

    fun provideMessageRepository(
        database: AppDatabase,
        gateway: TelegramGateway,
        mediaManager: MediaManager
    ): MessageRepository = MessageRepository(database.messageDao(), gateway, mediaManager)

    fun provideUserRepository(
        database: AppDatabase,
        mediaManager: MediaManager
    ): UserRepository = UserRepository(database.userDao(), mediaManager)
}

fun appModule(appContainer: AppContainer) = module {
    single { appContainer }
    single { SessionManager(get()) }
    single { appContainer.database }
    single { appContainer.tokenManager }
    single { appContainer.tokenValidator }

    factory<SessionContainer> {
        requireNotNull(get<SessionManager>().currentSession()) {
            "Session dependencies are unavailable without a saved token."
        }
    }
    factory { get<SessionContainer>().chatRepository }
    factory { get<SessionContainer>().messageRepository }
    factory { get<SessionContainer>().userRepository }

    viewModel { ChatListViewModel(get()) }
    viewModel { params ->
        GroupViewModel(
            chatId = params.get(),
            chatRepository = get(),
            messageRepository = get(),
            userRepository = get()
        )
    }
}
