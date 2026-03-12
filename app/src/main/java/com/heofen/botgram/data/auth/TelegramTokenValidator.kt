package com.heofen.botgram.data.auth

import com.heofen.botgram.data.remote.telegramapi.TelegramBotApiClient

class TelegramTokenValidator {
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
