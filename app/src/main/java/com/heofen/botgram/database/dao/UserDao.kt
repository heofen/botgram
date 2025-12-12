package com.heofen.botgram.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.heofen.botgram.database.tables.User

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: Long): User?

    @Query("SELECT EXISTS(SELECT * FROM users WHERE id = :userId)")
    suspend fun userExists(userId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: User)

    @Query("SELECT * FROM users WHERE avatarFileUniqueId = :uniqueId AND avatarLocalPath IS NOT NULL LIMIT 1")
    suspend fun findByAvatarUniqueId(uniqueId: String): User?

    @Query("""
        UPDATE users 
        SET avatarFileId = :fileId,
            avatarFileUniqueId = :fileUniqueId,
            avatarLocalPath = :localPath
        WHERE id = :userId
    """)
    suspend fun updateAvatar(
        userId: Long,
        fileId: String?,
        fileUniqueId: String?,
        localPath: String?
    )
}
