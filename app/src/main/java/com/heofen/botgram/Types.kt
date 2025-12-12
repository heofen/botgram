package com.heofen.botgram

enum class ChatType {
    PRIVATE,
    GROUP,
    CHANNEL,
    SUPERGROUP,
}

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