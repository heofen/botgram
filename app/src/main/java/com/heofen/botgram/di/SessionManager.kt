package com.heofen.botgram.di

class SessionManager(
    private val appContainer: AppContainer
) {
    fun currentSession(): SessionContainer? = appContainer.currentSession()

    fun currentSessionForToken(token: String): SessionContainer =
        appContainer.currentSessionForToken(token)

    fun clearSession() {
        appContainer.clearSession()
    }
}
