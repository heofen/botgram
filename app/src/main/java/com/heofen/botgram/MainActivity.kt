package com.heofen.botgram

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.heofen.botgram.data.auth.TelegramTokenValidator
import com.heofen.botgram.data.local.TokenManager
import com.heofen.botgram.di.SessionManager
import com.heofen.botgram.services.GetUpdates
import com.heofen.botgram.ui.BotgramNavHost
import com.heofen.botgram.ui.screens.login.LoginScreen
import com.heofen.botgram.ui.theme.BotgramTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

/**
 * Корневая `Activity`, которая выбирает между экраном логина и основной навигацией.
 */
class MainActivity : ComponentActivity() {
    private val sessionManager: SessionManager by inject()
    private val tokenManager: TokenManager by inject()
    private val tokenValidator: TelegramTokenValidator by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val token = tokenManager.getToken()

        if (token.isNullOrBlank()) {
            setContent {
                LoginScreen(
                    onCheckToken = { inputToken ->
                        withContext(Dispatchers.IO) {
                            try {
                                // Проверяем токен через Telegram API до сохранения.
                                val isValid = tokenValidator.validate(inputToken)
                                if (isValid) {
                                    tokenManager.saveToken(inputToken)
                                    sessionManager.clearSession()
                                    Log.i("Login", "Token is valid. Saved.")
                                } else {
                                    Log.w("Login", "Telegram API returned invalid token")
                                }
                                isValid
                            } catch (e: Exception) {
                                Log.e("Login", "Network error: ${e.message}")
                                false
                            }
                        }
                    },
                    onLoginSuccess = ::restartApp
                )
            }
            return
        }

        startUpdateService()

        setContent {
            BotgramTheme {
                BotgramNavHost(onLogOut = ::logOut)
            }
        }
    }

    private fun restartApp() {
        val restartIntent = intent
        finish()
        startActivity(restartIntent)
    }

    /** Очищает токен и session-scoped зависимости, затем перезапускает приложение. */
    private fun logOut() {
        tokenManager.clearToken()
        sessionManager.clearSession()
        stopService(Intent(applicationContext, GetUpdates::class.java))
        restartApp()
    }

    /** Запускает foreground-сервис, отвечающий за long polling Telegram API. */
    private fun startUpdateService() {
        val intent = Intent(this, GetUpdates::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}
