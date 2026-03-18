package com.heofen.botgram

/** Типы чатов, поддерживаемые приложением. */
enum class ChatType {
    PRIVATE,
    GROUP,
    CHANNEL,
    SUPERGROUP,
}

/** Типы сообщений, с которыми умеет работать локальная модель. */
enum class MessageType {
    TEXT,
    PHOTO,
    VIDEO,
    ANIMATION, // гифки
    AUDIO,
    VOICE,
    VIDEO_NOTE, // кружок
    DOCUMENT,
    STICKER, // обычный
    ANIMATED_STICKER, // tgs стикер
    VIDEO_STICKER, // webm стикер
    CONTACT,
    LOCATION,
}
