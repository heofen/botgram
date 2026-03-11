package com.heofen.botgram.data.remote.telegramapi

data class TelegramApiResponse<T>(
    val ok: Boolean,
    val result: T?,
    val description: String?,
    val errorCode: Int?
)

data class UpdateDto(
    val updateId: Long,
    val message: MessageDto?,
    val editedMessage: MessageDto?
)

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

data class ChatDto(
    val id: Long,
    val type: String,
    val title: String?,
    val username: String?,
    val firstName: String?,
    val lastName: String?,
    val photo: ChatPhotoDto?
)

data class UserDto(
    val id: Long,
    val firstName: String,
    val lastName: String?
)

data class PhotoSizeDto(
    val fileId: String,
    val fileUniqueId: String,
    val fileSize: Long?,
    val width: Int,
    val height: Int
)

data class ThumbnailDto(
    val fileId: String,
    val fileUniqueId: String,
    val fileSize: Long?,
    val width: Int,
    val height: Int
)

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

data class AudioDto(
    val fileId: String,
    val fileUniqueId: String,
    val fileSize: Long?,
    val duration: Long?,
    val fileName: String?,
    val mimeType: String?,
    val thumbnail: ThumbnailDto?
)

data class VoiceDto(
    val fileId: String,
    val fileUniqueId: String,
    val fileSize: Long?,
    val duration: Long?,
    val mimeType: String?
)

data class VideoNoteDto(
    val fileId: String,
    val fileUniqueId: String,
    val fileSize: Long?,
    val width: Int,
    val height: Int,
    val duration: Long?,
    val thumbnail: ThumbnailDto?
)

data class DocumentDto(
    val fileId: String,
    val fileUniqueId: String,
    val fileSize: Long?,
    val fileName: String?,
    val mimeType: String?,
    val thumbnail: ThumbnailDto?
)

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

data class ContactDto(
    val phoneNumber: String?,
    val firstName: String?,
    val lastName: String?,
    val userId: Long?
)

data class LocationDto(
    val longitude: Double?,
    val latitude: Double?
)

data class TelegramFileDto(
    val fileId: String,
    val fileUniqueId: String,
    val fileSize: Long?,
    val filePath: String?
)

data class UserProfilePhotosDto(
    val totalCount: Int,
    val photos: List<List<PhotoSizeDto>>
)

data class ChatPhotoDto(
    val smallFileId: String,
    val smallFileUniqueId: String,
    val bigFileId: String,
    val bigFileUniqueId: String
)
