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
