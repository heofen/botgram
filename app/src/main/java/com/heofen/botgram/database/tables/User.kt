package com.heofen.botgram.database.tables

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: Long,
    val firstName: String,
    val lastName: String?,
    val bio: String?,

    val avatarFileId: String?,
    val avatarFileUniqueId: String?,
    val avatarLocalPath: String?,

    val canWriteMsgToPm: Boolean = false
)