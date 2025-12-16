package com.heofen.botgram.utils.extensions

import com.heofen.botgram.database.tables.User

fun User.getInitials(): String {
    val f = this.firstName.firstOrNull()?.toString() ?: ""
    val l = this.lastName?.firstOrNull()?.toString() ?: ""
    return (f + l).uppercase()
}