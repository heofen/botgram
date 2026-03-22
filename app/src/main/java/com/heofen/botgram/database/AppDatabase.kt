package com.heofen.botgram.database

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.heofen.botgram.database.dao.ChatDao
import com.heofen.botgram.database.dao.MessageDao
import com.heofen.botgram.database.dao.UserDao
import com.heofen.botgram.database.tables.Chat
import com.heofen.botgram.database.tables.Message
import com.heofen.botgram.database.tables.User

/** Основная Room-база приложения. */
@Database(
    entities = [Message::class, Chat::class, User::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    /** DAO сообщений. */
    abstract fun messageDao(): MessageDao
    /** DAO чатов. */
    abstract fun chatDao(): ChatDao
    /** DAO пользователей. */
    abstract fun userDao(): UserDao

    companion object {
        /** Миграция, добавляющая координаты в сущность сообщения. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE messages ADD COLUMN longitude REAL")
            }
        }

        /** Миграция, добавляющая в профиль пользователя username и languageCode. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN username TEXT")
                db.execSQL("ALTER TABLE users ADD COLUMN languageCode TEXT")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** Возвращает singleton-экземпляр Room-базы. */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "botgram_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
