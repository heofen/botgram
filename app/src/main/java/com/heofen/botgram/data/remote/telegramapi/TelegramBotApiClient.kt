package com.heofen.botgram.data.remote.telegramapi

import android.util.Log
import com.heofen.botgram.data.remote.OutgoingVisualMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/** Исключение сетевого слоя Telegram с HTTP- и API-метаданными. */
class TelegramApiException(
    message: String,
    val statusCode: Int? = null,
    val errorCode: Int? = null,
    val description: String? = null
) : Exception(message)

/**
 * Низкоуровневый клиент Telegram Bot API.
 *
 * Отвечает за HTTP-запросы, разбор JSON и преобразование ответов в DTO.
 */
class TelegramBotApiClient(
    private val token: String,
    private val client: OkHttpClient = OkHttpClient()
) {
    private val isClosed = AtomicBoolean(false)
    private val longPollingClient = client.newBuilder()
        .readTimeout(65, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://api.telegram.org/bot$token/"
    private val fileBaseUrl = "https://api.telegram.org/file/bot$token/"

    /** Выполняет long polling запрос `getUpdates`. */
    suspend fun getUpdates(offset: Long?, timeout: Int): List<UpdateDto> {
        val bodyBuilder = FormBody.Builder()
            .add("timeout", timeout.toString())

        if (offset != null) {
            bodyBuilder.add("offset", offset.toString())
        }

        val json = postJson("getUpdates", bodyBuilder.build(), useLongPollingClient = true)
        return parseUpdates(requireResultArray(parseApiResponse(json), "getUpdates"))
    }

    /** Отправляет обычное текстовое сообщение. */
    suspend fun sendMessage(
        chatId: Long,
        text: String,
        replyToMessageId: Long? = null
    ): MessageDto {
        val bodyBuilder = FormBody.Builder()
            .add("chat_id", chatId.toString())
            .add("text", text)

        if (replyToMessageId != null) {
            bodyBuilder.add("reply_to_message_id", replyToMessageId.toString())
        }

        val body = bodyBuilder.build()

        val json = postJson("sendMessage", body)
        return parseMessage(requireResultObject(parseApiResponse(json), "sendMessage"))
    }

    /** Отправляет геолокацию. */
    suspend fun sendLocation(
        chatId: Long,
        latitude: Double,
        longitude: Double,
        replyToMessageId: Long? = null
    ): MessageDto {
        val bodyBuilder = FormBody.Builder()
            .add("chat_id", chatId.toString())
            .add("latitude", latitude.toString())
            .add("longitude", longitude.toString())

        if (replyToMessageId != null) {
            bodyBuilder.add("reply_to_message_id", replyToMessageId.toString())
        }

        val json = postJson("sendLocation", bodyBuilder.build())
        return parseMessage(requireResultObject(parseApiResponse(json), "sendLocation"))
    }

    /** Отправляет файл как документ multipart-запросом. */
    suspend fun sendDocument(
        chatId: Long,
        file: File,
        mimeType: String,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): MessageDto {
        val body = buildMultipartMediaBody(
            fieldName = "document",
            chatId = chatId,
            file = file,
            mimeType = mimeType,
            caption = caption,
            replyToMessageId = replyToMessageId
        )
        val json = postMultipartJson("sendDocument", body)
        return parseMessage(requireResultObject(parseApiResponse(json), "sendDocument"))
    }

    /** Отправляет фотографию multipart-запросом. */
    suspend fun sendPhoto(
        chatId: Long,
        file: File,
        mimeType: String,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): MessageDto {
        val body = buildMultipartMediaBody(
            fieldName = "photo",
            chatId = chatId,
            file = file,
            mimeType = mimeType,
            caption = caption,
            replyToMessageId = replyToMessageId
        )
        val json = postMultipartJson("sendPhoto", body)
        return parseMessage(requireResultObject(parseApiResponse(json), "sendPhoto"))
    }

    /** Отправляет видео multipart-запросом. */
    suspend fun sendVideo(
        chatId: Long,
        file: File,
        mimeType: String,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): MessageDto {
        val body = buildMultipartMediaBody(
            fieldName = "video",
            chatId = chatId,
            file = file,
            mimeType = mimeType,
            caption = caption,
            replyToMessageId = replyToMessageId
        )
        val json = postMultipartJson("sendVideo", body)
        return parseMessage(requireResultObject(parseApiResponse(json), "sendVideo"))
    }

    /** Отправляет альбом из фото и/или видео. */
    suspend fun sendMediaGroup(
        chatId: Long,
        media: List<OutgoingVisualMedia>,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): List<MessageDto> {
        val body = buildMediaGroupBody(
            chatId = chatId,
            media = media,
            caption = caption,
            replyToMessageId = replyToMessageId
        )
        val json = postMultipartJson("sendMediaGroup", body)
        return parseMessages(requireResultArray(parseApiResponse(json), "sendMediaGroup"))
    }

    /** Удаляет сообщение через Telegram API. */
    suspend fun deleteMessage(chatId: Long, messageId: Long): Boolean {
        val body = FormBody.Builder()
            .add("chat_id", chatId.toString())
            .add("message_id", messageId.toString())
            .build()

        val json = postJson("deleteMessage", body)
        return requireResultBoolean(parseApiResponse(json), "deleteMessage")
    }

    /** Возвращает сведения о текущем боте для проверки токена. */
    suspend fun getMe(): UserDto {
        val httpUrl = buildMethodUrl(method = "getMe", queryParams = emptyMap())
        val json = getJson(httpUrl)
        return parseUser(requireResultObject(parseApiResponse(json), "getMe"))
    }

    /** Получает метаданные файла по `fileId`. */
    suspend fun getFile(fileId: String): TelegramFileDto {
        val httpUrl = buildMethodUrl(
            method = "getFile",
            queryParams = mapOf("file_id" to fileId)
        )
        val json = getJson(httpUrl)
        return parseTelegramFile(requireResultObject(parseApiResponse(json), "getFile"))
    }

    /** Получает аватары пользователя. */
    suspend fun getUserProfilePhotos(userId: Long, limit: Int): UserProfilePhotosDto {
        val httpUrl = buildMethodUrl(
            method = "getUserProfilePhotos",
            queryParams = mapOf(
                "user_id" to userId.toString(),
                "limit" to limit.toString()
            )
        )
        val json = getJson(httpUrl)
        return parseUserProfilePhotos(requireResultObject(parseApiResponse(json), "getUserProfilePhotos"))
    }

    /** Получает сведения о чате. */
    suspend fun getChat(chatId: Long): ChatDto {
        val httpUrl = buildMethodUrl(
            method = "getChat",
            queryParams = mapOf("chat_id" to chatId.toString())
        )
        val json = getJson(httpUrl)
        return parseChat(requireResultObject(parseApiResponse(json), "getChat"))
    }

    /** Выполняет обычный GET-запрос и возвращает тело ответа как строку. */
    suspend fun getText(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android) botgram")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw TelegramApiException(
                    message = "GET $url failed with HTTP ${response.code}",
                    statusCode = response.code
                )
            }

            response.body?.string().orEmpty()
        }
    }

    /** Скачивает файл Telegram по `file_path` в локальный файл. */
    suspend fun downloadFile(filePath: String, destination: File): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(fileBaseUrl + filePath)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw TelegramApiException("downloadFile failed with HTTP ${response.code}")
            }

            destination.parentFile?.mkdirs()
            response.body?.byteStream()?.use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext false

            destination.exists() && destination.length() > 0
        }
    }

    /** Асинхронно освобождает ресурсы `OkHttpClient`. */
    fun close() {
        if (!isClosed.compareAndSet(false, true)) return

        thread(
            start = true,
            isDaemon = true,
            name = "TelegramBotApiClient-close"
        ) {
            runCatching {
                shutdownResources(longPollingClient, client)
            }.onFailure {
                Log.w("TelegramBotApiClient", "Failed to close clients cleanly", it)
            }
        }
    }

    /** Корректно завершает dispatcher, pool и cache у всех переданных клиентов. */
    private fun shutdownResources(vararg clients: OkHttpClient) {
        val dispatchers = linkedSetOf<Dispatcher>()
        val pools = linkedSetOf<ConnectionPool>()
        val caches = linkedSetOf<Cache>()

        clients.forEach { httpClient ->
            dispatchers += httpClient.dispatcher
            pools += httpClient.connectionPool
            httpClient.cache?.let(caches::add)
        }

        dispatchers.forEach { dispatcher ->
            dispatcher.cancelAll()
            dispatcher.executorService.shutdown()
        }
        pools.forEach(ConnectionPool::evictAll)
        caches.forEach(Cache::close)
    }

    /** Выполняет GET-запрос и парсит тело как JSON-объект. */
    private suspend fun getJson(httpUrl: okhttp3.HttpUrl): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(httpUrl)
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
                ?: throw TelegramApiException("Empty response body for ${httpUrl.encodedPath}")
            val json = parseJsonBody(body, httpUrl.encodedPath)

            if (!response.isSuccessful) {
                throwTelegramApiException(
                    statusCode = response.code,
                    path = httpUrl.encodedPath,
                    json = json
                )
            }

            json
        }
    }

    private suspend fun postJson(
        method: String,
        body: FormBody,
        useLongPollingClient: Boolean = false
    ): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(baseUrl + method)
            .post(body)
            .build()

        val httpClient = if (useLongPollingClient) longPollingClient else client

        httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string()
                ?: throw TelegramApiException("Empty response body for $method")
            val json = parseJsonBody(responseBody, method)

            if (!response.isSuccessful) {
                throwTelegramApiException(
                    statusCode = response.code,
                    path = method,
                    json = json
                )
            }

            json
        }
    }

    private suspend fun postMultipartJson(
        method: String,
        body: MultipartBody
    ): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(baseUrl + method)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string()
                ?: throw TelegramApiException("Empty response body for $method")
            val json = parseJsonBody(responseBody, method)

            if (!response.isSuccessful) {
                throwTelegramApiException(
                    statusCode = response.code,
                    path = method,
                    json = json
                )
            }

            json
        }
    }

    private fun buildMultipartMediaBody(
        fieldName: String,
        chatId: Long,
        file: File,
        mimeType: String,
        caption: String?,
        replyToMessageId: Long?
    ): MultipartBody {
        return MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId.toString())
            .apply {
                caption?.takeIf { it.isNotBlank() }?.let { addFormDataPart("caption", it) }
                replyToMessageId?.let { addFormDataPart("reply_to_message_id", it.toString()) }
                addFormDataPart(
                    fieldName,
                    file.name,
                    file.asRequestBody(mimeType.toMediaTypeOrNull())
                )
            }
            .build()
    }

    private fun buildMediaGroupBody(
        chatId: Long,
        media: List<OutgoingVisualMedia>,
        caption: String?,
        replyToMessageId: Long?
    ): MultipartBody {
        val bodyBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId.toString())
        val mediaJson = JSONArray()

        replyToMessageId?.let {
            bodyBuilder.addFormDataPart("reply_to_message_id", it.toString())
        }

        media.forEachIndexed { index, item ->
            val attachName = "media$index"
            val type = when {
                item.mimeType.startsWith("image/") -> "photo"
                item.mimeType.startsWith("video/") -> "video"
                else -> throw TelegramApiException("Unsupported media type for sendMediaGroup: ${item.mimeType}")
            }
            val mediaItem = JSONObject()
                .put("type", type)
                .put("media", "attach://$attachName")

            if (index == 0 && !caption.isNullOrBlank()) {
                mediaItem.put("caption", caption)
            }

            mediaJson.put(mediaItem)
            bodyBuilder.addFormDataPart(
                attachName,
                item.file.name,
                item.file.asRequestBody(item.mimeType.toMediaTypeOrNull())
            )
        }

        bodyBuilder.addFormDataPart("media", mediaJson.toString())
        return bodyBuilder.build()
    }

    private fun buildMethodUrl(method: String, queryParams: Map<String, String>): okhttp3.HttpUrl {
        val urlBuilder = (baseUrl + method).toHttpUrl().newBuilder()
        queryParams.forEach { (key, value) ->
            urlBuilder.addQueryParameter(key, value)
        }
        return urlBuilder.build()
    }

    private fun parseApiResponse(json: JSONObject): Any? {
        val response = TelegramApiResponse(
            ok = json.optBoolean("ok", false),
            result = json.opt("result"),
            description = json.optString("description").takeIf { it.isNotBlank() },
            errorCode = json.optInt("error_code").takeIf { it != 0 }
        )

        if (!response.ok) {
            throw TelegramApiException(
                message = buildString {
                    append("Telegram API error")
                    response.errorCode?.let { append(" $it") }
                    response.description?.let { append(": ").append(it) }
                },
                errorCode = response.errorCode,
                description = response.description
            )
        }

        return response.result
    }

    private fun parseJsonBody(body: String, requestName: String): JSONObject {
        return try {
            JSONObject(body)
        } catch (_: JSONException) {
            throw TelegramApiException("Invalid JSON response for $requestName: $body")
        }
    }

    private fun throwTelegramApiException(
        statusCode: Int,
        path: String,
        json: JSONObject
    ): Nothing {
        val errorCode = json.optInt("error_code").takeIf { it != 0 }
        val description = json.optString("description").takeIf { it.isNotBlank() }
        val message = buildString {
            append("HTTP ").append(statusCode).append(" for ").append(path)
            errorCode?.let { append(" (Telegram ").append(it).append(")") }
            description?.let { append(": ").append(it) }
        }
        throw TelegramApiException(
            message = message,
            statusCode = statusCode,
            errorCode = errorCode,
            description = description
        )
    }

    private fun requireResultObject(result: Any?, method: String): JSONObject {
        return result as? JSONObject
            ?: throw TelegramApiException("Unexpected result type for $method")
    }

    private fun requireResultArray(result: Any?, method: String): JSONArray {
        return result as? JSONArray
            ?: throw TelegramApiException("Unexpected result type for $method")
    }

    private fun requireResultBoolean(result: Any?, method: String): Boolean {
        return result as? Boolean
            ?: throw TelegramApiException("Unexpected result type for $method")
    }

    private fun parseUpdates(updatesArray: JSONArray): List<UpdateDto> {
        return List(updatesArray.length()) { index ->
            parseUpdate(updatesArray.getJSONObject(index))
        }
    }

    private fun parseUpdate(json: JSONObject): UpdateDto {
        return UpdateDto(
            updateId = json.getLong("update_id"),
            message = json.optJSONObject("message")?.let { parseMessage(it) },
            editedMessage = json.optJSONObject("edited_message")?.let { parseMessage(it) }
        )
    }

    private fun parseMessages(json: JSONArray): List<MessageDto> {
        return List(json.length()) { index ->
            parseMessage(json.getJSONObject(index))
        }
    }

    private fun parseMessage(json: JSONObject, includeReply: Boolean = true): MessageDto {
        return MessageDto(
            messageId = json.getLong("message_id"),
            date = json.getLong("date"),
            editDate = json.optLong("edit_date").takeIf { it != 0L },
            messageThreadId = json.optLong("message_thread_id").takeIf { it != 0L },
            mediaGroupId = json.optString("media_group_id").takeIf { it.isNotBlank() },
            chat = parseChat(json.getJSONObject("chat")),
            from = json.optJSONObject("from")?.let { parseUser(it) },
            replyToMessage = if (includeReply) {
                json.optJSONObject("reply_to_message")?.let { parseMessage(it, includeReply = false) }
            } else {
                null
            },
            text = json.optString("text").takeIf { it.isNotBlank() },
            caption = json.optString("caption").takeIf { it.isNotBlank() },
            photo = json.optJSONArray("photo")?.let { parsePhotoArray(it) },
            video = json.optJSONObject("video")?.let { parseVideo(it) },
            animation = json.optJSONObject("animation")?.let { parseAnimation(it) },
            audio = json.optJSONObject("audio")?.let { parseAudio(it) },
            voice = json.optJSONObject("voice")?.let { parseVoice(it) },
            videoNote = json.optJSONObject("video_note")?.let { parseVideoNote(it) },
            document = json.optJSONObject("document")?.let { parseDocument(it) },
            sticker = json.optJSONObject("sticker")?.let { parseSticker(it) },
            contact = json.optJSONObject("contact")?.let { parseContact(it) },
            location = json.optJSONObject("location")?.let { parseLocation(it) },
            newChatPhoto = json.optJSONArray("new_chat_photo")?.let { parsePhotoArray(it) },
            deleteChatPhoto = json.optBoolean("delete_chat_photo", false)
        )
    }

    private fun parseChat(json: JSONObject): ChatDto {
        return ChatDto(
            id = json.getLong("id"),
            type = json.getString("type"),
            title = json.optString("title").takeIf { it.isNotBlank() },
            username = json.optString("username").takeIf { it.isNotBlank() },
            firstName = json.optString("first_name").takeIf { it.isNotBlank() },
            lastName = json.optString("last_name").takeIf { it.isNotBlank() },
            description = json.optString("description").takeIf { it.isNotBlank() },
            photo = json.optJSONObject("photo")?.let { parseChatPhoto(it) }
        )
    }

    private fun parseUser(json: JSONObject): UserDto {
        return UserDto(
            id = json.getLong("id"),
            firstName = json.getString("first_name"),
            lastName = json.optString("last_name").takeIf { it.isNotBlank() },
            username = json.optString("username").takeIf { it.isNotBlank() },
            languageCode = json.optString("language_code").takeIf { it.isNotBlank() }
        )
    }

    private fun parsePhotoArray(json: JSONArray): List<PhotoSizeDto> {
        return List(json.length()) { index ->
            parsePhotoSize(json.getJSONObject(index))
        }
    }

    private fun parsePhotoSize(json: JSONObject): PhotoSizeDto {
        return PhotoSizeDto(
            fileId = json.getString("file_id"),
            fileUniqueId = json.getString("file_unique_id"),
            fileSize = json.optLong("file_size").takeIf { it != 0L },
            width = json.getInt("width"),
            height = json.getInt("height")
        )
    }

    private fun parseThumbnail(json: JSONObject): ThumbnailDto {
        return ThumbnailDto(
            fileId = json.getString("file_id"),
            fileUniqueId = json.getString("file_unique_id"),
            fileSize = json.optLong("file_size").takeIf { it != 0L },
            width = json.getInt("width"),
            height = json.getInt("height")
        )
    }

    private fun parseVideo(json: JSONObject): VideoDto {
        return VideoDto(
            fileId = json.getString("file_id"),
            fileUniqueId = json.getString("file_unique_id"),
            fileSize = json.optLong("file_size").takeIf { it != 0L },
            width = json.getInt("width"),
            height = json.getInt("height"),
            duration = json.optLong("duration").takeIf { it != 0L },
            thumbnail = json.optJSONObject("thumbnail")?.let { parseThumbnail(it) },
            mimeType = json.optString("mime_type").takeIf { it.isNotBlank() }
        )
    }

    private fun parseAnimation(json: JSONObject): AnimationDto {
        return AnimationDto(
            fileId = json.getString("file_id"),
            fileUniqueId = json.getString("file_unique_id"),
            fileSize = json.optLong("file_size").takeIf { it != 0L },
            width = json.getInt("width"),
            height = json.getInt("height"),
            duration = json.optLong("duration").takeIf { it != 0L },
            fileName = json.optString("file_name").takeIf { it.isNotBlank() },
            mimeType = json.optString("mime_type").takeIf { it.isNotBlank() },
            thumbnail = json.optJSONObject("thumbnail")?.let { parseThumbnail(it) }
        )
    }

    private fun parseAudio(json: JSONObject): AudioDto {
        return AudioDto(
            fileId = json.getString("file_id"),
            fileUniqueId = json.getString("file_unique_id"),
            fileSize = json.optLong("file_size").takeIf { it != 0L },
            duration = json.optLong("duration").takeIf { it != 0L },
            fileName = json.optString("file_name").takeIf { it.isNotBlank() },
            mimeType = json.optString("mime_type").takeIf { it.isNotBlank() },
            thumbnail = json.optJSONObject("thumbnail")?.let { parseThumbnail(it) }
        )
    }

    private fun parseVoice(json: JSONObject): VoiceDto {
        return VoiceDto(
            fileId = json.getString("file_id"),
            fileUniqueId = json.getString("file_unique_id"),
            fileSize = json.optLong("file_size").takeIf { it != 0L },
            duration = json.optLong("duration").takeIf { it != 0L },
            mimeType = json.optString("mime_type").takeIf { it.isNotBlank() }
        )
    }

    private fun parseVideoNote(json: JSONObject): VideoNoteDto {
        return VideoNoteDto(
            fileId = json.getString("file_id"),
            fileUniqueId = json.getString("file_unique_id"),
            fileSize = json.optLong("file_size").takeIf { it != 0L },
            width = json.getInt("length"),
            height = json.getInt("length"),
            duration = json.optLong("duration").takeIf { it != 0L },
            thumbnail = json.optJSONObject("thumbnail")?.let { parseThumbnail(it) }
        )
    }

    private fun parseDocument(json: JSONObject): DocumentDto {
        return DocumentDto(
            fileId = json.getString("file_id"),
            fileUniqueId = json.getString("file_unique_id"),
            fileSize = json.optLong("file_size").takeIf { it != 0L },
            fileName = json.optString("file_name").takeIf { it.isNotBlank() },
            mimeType = json.optString("mime_type").takeIf { it.isNotBlank() },
            thumbnail = json.optJSONObject("thumbnail")?.let { parseThumbnail(it) }
        )
    }

    private fun parseSticker(json: JSONObject): StickerDto {
        return StickerDto(
            fileId = json.getString("file_id"),
            fileUniqueId = json.getString("file_unique_id"),
            fileSize = json.optLong("file_size").takeIf { it != 0L },
            width = json.getInt("width"),
            height = json.getInt("height"),
            isAnimated = json.optBoolean("is_animated", false),
            isVideo = json.optBoolean("is_video", false),
            thumbnail = json.optJSONObject("thumbnail")?.let { parseThumbnail(it) }
        )
    }

    private fun parseContact(json: JSONObject): ContactDto {
        return ContactDto(
            phoneNumber = json.optString("phone_number").takeIf { it.isNotBlank() },
            firstName = json.optString("first_name").takeIf { it.isNotBlank() },
            lastName = json.optString("last_name").takeIf { it.isNotBlank() },
            userId = json.optLong("user_id").takeIf { it != 0L }
        )
    }

    private fun parseLocation(json: JSONObject): LocationDto {
        return LocationDto(
            longitude = json.optDouble("longitude").takeUnless { it.isNaN() },
            latitude = json.optDouble("latitude").takeUnless { it.isNaN() }
        )
    }

    private fun parseTelegramFile(json: JSONObject): TelegramFileDto {
        return TelegramFileDto(
            fileId = json.getString("file_id"),
            fileUniqueId = json.getString("file_unique_id"),
            fileSize = json.optLong("file_size").takeIf { it != 0L },
            filePath = json.optString("file_path").takeIf { it.isNotBlank() }
        )
    }

    private fun parseUserProfilePhotos(json: JSONObject): UserProfilePhotosDto {
        val photosJson = json.optJSONArray("photos") ?: JSONArray()
        val photos = List(photosJson.length()) { rowIndex ->
            parsePhotoArray(photosJson.getJSONArray(rowIndex))
        }
        return UserProfilePhotosDto(
            totalCount = json.optInt("total_count", 0),
            photos = photos
        )
    }

    private fun parseChatPhoto(json: JSONObject): ChatPhotoDto {
        return ChatPhotoDto(
            smallFileId = json.getString("small_file_id"),
            smallFileUniqueId = json.getString("small_file_unique_id"),
            bigFileId = json.getString("big_file_id"),
            bigFileUniqueId = json.getString("big_file_unique_id")
        )
    }
}
