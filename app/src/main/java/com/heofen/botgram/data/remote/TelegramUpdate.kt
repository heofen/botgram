package com.heofen.botgram.data.remote

/** Нормализованная модель входящих обновлений Telegram. */
sealed interface TelegramUpdate {
    val updateId: Long

    /** Новое сообщение. */
    data class NewMessage(
        override val updateId: Long,
        val message: TelegramIncomingMessage
    ) : TelegramUpdate

    /** Отредактированное сообщение. */
    data class EditedMessage(
        override val updateId: Long,
        val message: TelegramIncomingMessage
    ) : TelegramUpdate

    /** Неподдерживаемый тип обновления, который клиент пропускает. */
    data class Unsupported(
        override val updateId: Long
    ) : TelegramUpdate
}
