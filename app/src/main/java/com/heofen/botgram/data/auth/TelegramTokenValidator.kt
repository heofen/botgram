package com.heofen.botgram.data.auth

import com.heofen.botgram.data.remote.telegramapi.TelegramBotApiClient

/** Проверяет bot token через вызов `getMe` в Telegram Bot API. */
class TelegramTokenValidator {
    /** Возвращает `true`, если Telegram принимает токен и отвечает без ошибки. */
    suspend fun validate(token: String): Boolean {
        val client = TelegramBotApiClient(token = token)
        return try {
            client.getMe()
            true
        } finally {
            client.close()
        }
    }
}
