package com.heofen.botgram.di

import android.content.Context
import com.heofen.botgram.data.MediaManager
import com.heofen.botgram.data.local.TokenManager
import com.heofen.botgram.data.remote.TelegramGateway
import com.heofen.botgram.data.repository.ChatRepository
import com.heofen.botgram.data.repository.MessageRepository
import com.heofen.botgram.data.repository.UserRepository
import com.heofen.botgram.database.AppDatabase

/**
 * Корневой контейнер зависимостей приложения.
 *
 * Хранит singleton-объекты и умеет собирать зависимости, привязанные к текущему токену.
 */
class AppContainer(
    private val appContext: Context
) {
    /** База данных приложения. */
    val database: AppDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppModule.provideDatabase(appContext)
    }

    /** Менеджер сохранённого токена. */
    val tokenManager: TokenManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppModule.provideTokenManager(appContext)
    }

    /** Валидатор токена для экрана логина. */
    val tokenValidator by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppModule.provideTelegramTokenValidator()
    }

    @Volatile
    private var session: SessionContainer? = null

    /** Возвращает session-scoped зависимости для сохранённого токена. */
    fun currentSession(): SessionContainer? {
        val token = tokenManager.getToken()?.takeIf { it.isNotBlank() } ?: return null
        return currentSessionForToken(token)
    }

    /** Возвращает или создаёт сессию для указанного токена. */
    fun currentSessionForToken(token: String): SessionContainer {
        val existing = session
        if (existing?.token == token) return existing

        return synchronized(this) {
            val latest = session
            if (latest?.token == token) {
                latest
            } else {
                latest?.close()
                SessionContainer(
                    token = token,
                    gateway = AppModule.provideTelegramGateway(appContext, token),
                    mediaManagerFactory = AppModule::provideMediaManager,
                    database = database
                ).also { session = it }
            }
        }
    }

    /** Очищает активную сессию и освобождает связанные ресурсы. */
    fun clearSession() {
        synchronized(this) {
            session?.close()
            session = null
        }
    }
}

/** Контейнер зависимостей, жизненный цикл которого привязан к одному bot token. */
class SessionContainer internal constructor(
    val token: String,
    val gateway: TelegramGateway,
    mediaManagerFactory: (TelegramGateway) -> MediaManager,
    database: AppDatabase
) {
    private val mediaManager = mediaManagerFactory(gateway)

    val chatRepository: ChatRepository = AppModule.provideChatRepository(database, mediaManager, gateway)
    val messageRepository: MessageRepository = AppModule.provideMessageRepository(database, gateway, mediaManager)
    val userRepository: UserRepository = AppModule.provideUserRepository(database, mediaManager, gateway)

    /** Закрывает сетевой транспорт, связанный с текущей сессией. */
    fun close() {
        gateway.close()
    }
}
