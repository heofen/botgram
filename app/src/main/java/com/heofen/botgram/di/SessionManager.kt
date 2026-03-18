package com.heofen.botgram.di

/** Фасад для доступа к текущей session-scoped сборке зависимостей. */
class SessionManager(
    private val appContainer: AppContainer
) {
    /** Возвращает активную сессию, если токен уже сохранён. */
    fun currentSession(): SessionContainer? = appContainer.currentSession()

    /** Возвращает или создаёт сессию для конкретного токена. */
    fun currentSessionForToken(token: String): SessionContainer =
        appContainer.currentSessionForToken(token)

    /** Закрывает и очищает текущую сессию. */
    fun clearSession() {
        appContainer.clearSession()
    }
}
