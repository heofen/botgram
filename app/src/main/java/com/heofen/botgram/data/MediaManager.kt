package com.heofen.botgram.data

import dev.inmo.tgbotapi.bot.TelegramBot
import java.io.File
import android.content.Context
import android.util.Log
import com.heofen.botgram.data.repository.MessageRepository
import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.requests.abstracts.FileId
import dev.inmo.tgbotapi.requests.get.GetFile


class MediaManager (
    private val context: Context,
    private val bot: TelegramBot,
)
{
    private val internalDir: File = context.filesDir


    suspend fun getFile(
        fileId: String,
        fileExtension: String,
        fileUniqueId: String,
        isAvatar: Boolean = false
    ): String? {
        try {

            val path = "${if (isAvatar) "avatar" else "media"}/$fileUniqueId.$fileExtension"
            val file = File(context.cacheDir, path)

            file.parentFile?.mkdirs()

            if (file.exists()) {
                return file.absolutePath
            }

            val fileInfo = bot.execute(GetFile(FileId(fileId)))
            bot.downloadFile(fileInfo, file)

            if (file.exists() && file.length() > 0) {
                return file.absolutePath
            } else {
                return null
            }
        } catch (e: Exception) {
            Log.e("Media Manager Exception", e.toString())
            return null
        }
    }
}