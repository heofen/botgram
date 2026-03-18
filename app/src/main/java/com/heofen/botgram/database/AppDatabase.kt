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

@Database(
    entities = [Message::class, Chat::class, User::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun chatDao(): ChatDao
    abstract fun userDao(): UserDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE messages ADD COLUMN longitude REAL")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "botgram_database"
                ).addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
