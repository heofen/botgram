package com.heofen.botgram.data.remote

import android.content.Context
import com.heofen.botgram.data.remote.telegramapi.HttpTelegramGateway

/** Фабрика конкретной реализации шлюза Telegram. */
object TelegramGatewayFactory {
    /** Создаёт HTTP-реализацию транспорта для указанного токена. */
    fun create(context: Context, token: String): TelegramGateway =
        HttpTelegramGateway(context = context, token = token)
}
