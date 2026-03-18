package com.heofen.botgram.utils.extensions

import com.heofen.botgram.database.tables.Chat

/** Возвращает инициалы чата для отображения в заглушке аватара. */
fun Chat.getInitials(): String {
    if (this.title != null) return this.title.firstOrNull()?.toString()?.uppercase() ?: ""
    val f = this.firstName?.firstOrNull()?.toString() ?: ""
    val l = this.lastName?.firstOrNull()?.toString() ?: ""
    return (f + l).uppercase()
}
