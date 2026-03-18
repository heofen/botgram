package com.heofen.botgram.data.remote.telegramapi

/** Универсальная оболочка ответа Telegram Bot API. */
data class TelegramApiResponse<T>(
    val ok: Boolean,
    val result: T?,
    val description: String?,
    val errorCode: Int?
)

/** DTO обновления long polling. */
data class UpdateDto(
    val updateId: Long,
    val message: MessageDto?,
    val editedMessage: MessageDto?
)

/** DTO сообщения Telegram Bot API. */
data class MessageDto(
    val messageId: Long,
    val date: Long,
    val editDate: Long?,
    val messageThreadId: Long?,
    val mediaGroupId: String?,
    val chat: ChatDto,
    val from: UserDto?,
    val replyToMessage: MessageDto?,
    val text: String?,
    val caption: String?,
    val photo: List<PhotoSizeDto>?,
    val video: VideoDto?,
    val animation: AnimationDto?,
    val audio: AudioDto?,
    val voice: VoiceDto?,
    val videoNote: VideoNoteDto?,
    val document: DocumentDto?,
    val sticker: StickerDto?,
    val contact: ContactDto?,
    val location: LocationDto?
)

/** DTO чата Telegram. */
data class ChatDto(
    val id: Long,
    val type: String,
    val title: String?,
    val username: String?,
    val firstName: String?,
    val lastName: String?,
    val photo: ChatPhotoDto?
)

/** DTO пользователя Telegram. */
data class UserDto(
    val id: Long,
    val firstName: String,
    val lastName: String?
)

/** DTO изображения в одном из размеров. */
data class PhotoSizeDto(
    val fileId: String,
    val fileUniqueId: String,
    val fileSize: Long?,
    val width: Int,
    val height: Int
)

/** DTO миниатюры медиа. */
data class ThumbnailDto(
    val fileId: String,
    val fileUniqueId: String,
    val fileSize: Long?,
    val width: Int,
    val height: Int
)

/** DTO видео-вложения. */
data class VideoDto(
    val fileId: String,
    val fileUniqueId: String,
    val fileSize: Long?,
    val width: Int,
    val height: Int,
    val duration: Long?,
    val thumbnail: ThumbnailDto?,
    val mimeType: String?
)

/** DTO анимации/GIF. */
data class AnimationDto(
    val fileId: String,
    val fileUniqueId: String,
    val fileSize: Long?,
    val width: Int,
    val height: Int,
    val duration: Long?,
    val fileName: String?,
    val mimeType: String?,
    val thumbnail: ThumbnailDto?
)

/** DTO аудиофайла. */
data class AudioDto(
    val fileId: String,
    val fileUniqueId: String,
    val fileSize: Long?,
    val duration: Long?,
    val fileName: String?,
    val mimeType: String?,
    val thumbnail: ThumbnailDto?
)

/** DTO голосового сообщения. */
data class VoiceDto(
    val fileId: String,
    val fileUniqueId: String,
    val fileSize: Long?,
    val duration: Long?,
    val mimeType: String?
)

/** DTO видеосообщения-кружка. */
data class VideoNoteDto(
    val fileId: String,
    val fileUniqueId: String,
    val fileSize: Long?,
    val width: Int,
    val height: Int,
    val duration: Long?,
    val thumbnail: ThumbnailDto?
)

/** DTO документа. */
data class DocumentDto(
    val fileId: String,
    val fileUniqueId: String,
    val fileSize: Long?,
    val fileName: String?,
    val mimeType: String?,
    val thumbnail: ThumbnailDto?
)

/** DTO стикера. */
data class StickerDto(
    val fileId: String,
    val fileUniqueId: String,
    val fileSize: Long?,
    val width: Int,
    val height: Int,
    val isAnimated: Boolean,
    val isVideo: Boolean,
    val thumbnail: ThumbnailDto?
)

/** DTO контакта. */
data class ContactDto(
    val phoneNumber: String?,
    val firstName: String?,
    val lastName: String?,
    val userId: Long?
)

/** DTO геолокации. */
data class LocationDto(
    val longitude: Double?,
    val latitude: Double?
)

/** DTO файла Telegram, возвращаемый методом `getFile`. */
data class TelegramFileDto(
    val fileId: String,
    val fileUniqueId: String,
    val fileSize: Long?,
    val filePath: String?
)

/** DTO набора пользовательских аватаров. */
data class UserProfilePhotosDto(
    val totalCount: Int,
    val photos: List<List<PhotoSizeDto>>
)

/** DTO фотографии чата. */
data class ChatPhotoDto(
    val smallFileId: String,
    val smallFileUniqueId: String,
    val bigFileId: String,
    val bigFileUniqueId: String
)
