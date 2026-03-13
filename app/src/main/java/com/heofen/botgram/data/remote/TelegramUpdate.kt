package com.heofen.botgram.data.remote

sealed interface TelegramUpdate {
    val updateId: Long

    data class NewMessage(
        override val updateId: Long,
        val message: TelegramIncomingMessage
    ) : TelegramUpdate

    data class EditedMessage(
        override val updateId: Long,
        val message: TelegramIncomingMessage
    ) : TelegramUpdate

    data class Unsupported(
        override val updateId: Long
    ) : TelegramUpdate
}
