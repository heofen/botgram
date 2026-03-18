package com.heofen.botgram.utils.extensions

import com.heofen.botgram.database.tables.User

/** Возвращает инициалы пользователя для отображения в заглушке аватара. */
fun User.getInitials(): String {
    val f = this.firstName.firstOrNull()?.toString() ?: ""
    val l = this.lastName?.firstOrNull()?.toString() ?: ""
    return (f + l).uppercase()
}
