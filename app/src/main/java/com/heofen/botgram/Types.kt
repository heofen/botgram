package com.heofen.botgram

enum class ChatType {
    PRIVATE,
    GROUP,
    CHANNEL,
}

enum class MessageType {
    TEXT,
    PHOTO,
    VIDEO,
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