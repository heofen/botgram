package com.heofen.botgram.data.remote

import android.content.Context
import com.heofen.botgram.data.remote.telegramapi.HttpTelegramGateway

object TelegramGatewayFactory {
    fun create(context: Context, token: String): TelegramGateway =
        HttpTelegramGateway(context = context, token = token)
}
