package com.heofen.botgram.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.heofen.botgram.database.tables.User
import kotlinx.coroutines.flow.Flow

/** DAO для пользователей и их аватаров. */
@Dao
interface UserDao {
    /** Возвращает пользователя по идентификатору. */
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: Long): User?

    /** Наблюдает за изменениями конкретного пользователя. */
    @Query("SELECT * FROM users WHERE id = :id")
    fun observeById(id: Long): Flow<User?>

    /** Проверяет наличие пользователя в локальной базе. */
    @Query("SELECT EXISTS(SELECT * FROM users WHERE id = :userId)")
    suspend fun userExists(userId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: User)

    /** Ищет уже скачанный аватар по `fileUniqueId`. */
    @Query("SELECT * FROM users WHERE avatarFileUniqueId = :uniqueId AND avatarLocalPath IS NOT NULL LIMIT 1")
    suspend fun findByAvatarUniqueId(uniqueId: String): User?

    /** Обновляет данные локального аватара пользователя. */
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
