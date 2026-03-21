package com.heofen.botgram.ui.components

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.heofen.botgram.R
import com.heofen.botgram.ui.theme.BotgramBackdrop
import com.heofen.botgram.ui.theme.botgramLiquidGlass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AttachmentGalleryItem(
    val contentUri: Uri,
    val mimeType: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentSheet(
    backdrop: BotgramBackdrop,
    hasMediaPermission: Boolean,
    onDismissRequest: () -> Unit,
    onGrantMediaPermissionClick: () -> Unit,
    onMediaClick: (AttachmentGalleryItem) -> Unit,
    onFileClick: () -> Unit,
    onLocationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val canReadImages = remember(context, hasMediaPermission) {
        hasMediaPermission && context.canReadImageMedia()
    }
    val canReadVideos = remember(context, hasMediaPermission) {
        hasMediaPermission && context.canReadVideoMedia()
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    var mediaItems by remember(hasMediaPermission) { mutableStateOf<List<AttachmentGalleryItem>?>(null) }

    LaunchedEffect(context, hasMediaPermission, canReadImages, canReadVideos) {
        mediaItems = if (!hasMediaPermission) {
            emptyList()
        } else {
            withContext(Dispatchers.IO) {
                queryRecentMedia(
                    contentResolver = context.contentResolver,
                    includeImages = canReadImages,
                    includeVideos = canReadVideos
                )
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        dragHandle = null,
        shape = sheetShape,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.52f),
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .clip(sheetShape)
                .botgramLiquidGlass(
                    backdrop = backdrop,
                    shape = sheetShape,
                    blurRadius = 12.dp
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(26.dp))
                            .botgramLiquidGlass(
                                backdrop = backdrop,
                                shape = RoundedCornerShape(26.dp),
                                blurRadius = 8.dp
                            )
                    ) {
                        Text(
                            text = stringResource(R.string.attachment_sheet_gallery_title),
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = AttachmentSheetForeground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                when {
                    !hasMediaPermission -> {
                        AttachmentPermissionState(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 28.dp, vertical = 12.dp)
                                .padding(bottom = 126.dp),
                            onGrantMediaPermissionClick = onGrantMediaPermissionClick
                        )
                    }

                    mediaItems == null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AttachmentSheetForeground)
                        }
                    }

                    mediaItems.isNullOrEmpty() -> {
                        AttachmentEmptyState(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(bottom = 126.dp)
                        )
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(start = 2.dp, end = 2.dp, bottom = 126.dp)
                        ) {
                            items(mediaItems ?: emptyList()) { item ->
                                AttachmentMediaTile(
                                    item = item,
                                    onClick = { onMediaClick(item) }
                                )
                            }
                        }
                    }
                }
            }

            AttachmentQuickActions(
                backdrop = backdrop,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp),
                onFileClick = onFileClick,
                onLocationClick = onLocationClick
            )
        }
    }
}

@Composable
private fun AttachmentPermissionState(
    modifier: Modifier = Modifier,
    onGrantMediaPermissionClick: () -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.attachment_sheet_media_permission_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = AttachmentSheetForeground.copy(alpha = 0.85f)
            )
            Button(onClick = onGrantMediaPermissionClick) {
                Text(text = stringResource(R.string.attachment_sheet_allow_media_access))
            }
        }
    }
}

@Composable
private fun AttachmentEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.attachment_sheet_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = AttachmentSheetForeground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun AttachmentMediaTile(
    item: AttachmentGalleryItem,
    onClick: () -> Unit
) {
    val thumbnail = rememberMediaThumbnail(item.contentUri)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.Black.copy(alpha = 0.2f))
            .clickable(onClick = onClick)
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            AsyncImage(
                model = item.contentUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        if (item.mimeType.startsWith("video/")) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.52f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = stringResource(R.string.attachment_sheet_video_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun rememberMediaThumbnail(uri: Uri, sizePx: Int = 512): Bitmap? {
    val context = LocalContext.current
    val thumbnailState = produceState<Bitmap?>(initialValue = null, context, uri, sizePx) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.loadThumbnail(uri, Size(sizePx, sizePx), null)
            }.getOrNull()
        }
    }
    return thumbnailState.value
}

@Composable
private fun AttachmentQuickActions(
    backdrop: BotgramBackdrop,
    modifier: Modifier = Modifier,
    onFileClick: () -> Unit,
    onLocationClick: () -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(34.dp))
                .botgramLiquidGlass(
                    backdrop = backdrop,
                    shape = RoundedCornerShape(34.dp),
                    blurRadius = 10.dp
                )
                .padding(horizontal = 22.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AttachmentQuickAction(
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                        contentDescription = null,
                        tint = AttachmentSheetForeground,
                        modifier = Modifier.size(30.dp)
                    )
                },
                label = stringResource(R.string.attachment_sheet_file_label),
                onClick = onFileClick
            )
            AttachmentQuickAction(
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = AttachmentSheetForeground,
                        modifier = Modifier.size(30.dp)
                    )
                },
                label = stringResource(R.string.attachment_sheet_geo_label),
                onClick = onLocationClick
            )
        }
    }
}

@Composable
private fun AttachmentQuickAction(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        icon()
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = AttachmentSheetForeground
        )
    }
}

private fun queryRecentMedia(
    contentResolver: ContentResolver,
    includeImages: Boolean,
    includeVideos: Boolean,
    limit: Int = 60
): List<AttachmentGalleryItem> {
    val mediaTypes = buildList {
        if (includeImages) {
            add(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
        }
        if (includeVideos) {
            add(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
        }
    }
    if (mediaTypes.isEmpty()) {
        return emptyList()
    }

    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.MEDIA_TYPE,
        MediaStore.MediaColumns.MIME_TYPE
    )
    val selection = buildString {
        append(MediaStore.Files.FileColumns.MEDIA_TYPE)
        append(" IN (")
        append(mediaTypes.joinToString(",") { "?" })
        append(")")
    }
    val selectionArgs = mediaTypes.toTypedArray()
    val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"
    val media = mutableListOf<AttachmentGalleryItem>()

    contentResolver.query(
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
        projection,
        selection,
        selectionArgs,
        sortOrder
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val mediaTypeIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
        val mimeTypeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

        while (cursor.moveToNext() && media.size < limit) {
            val id = cursor.getLong(idIndex)
            val mediaType = cursor.getInt(mediaTypeIndex)
            val mimeType = cursor.getString(mimeTypeIndex) ?: continue
            val baseUri = when (mediaType) {
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            media += AttachmentGalleryItem(
                contentUri = ContentUris.withAppendedId(baseUri, id),
                mimeType = mimeType
            )
        }
    }

    return media
}

private fun Context.canReadImageMedia(): Boolean {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ) == PackageManager.PERMISSION_GRANTED
        }

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        }

        else -> {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}

private fun Context.canReadVideoMedia(): Boolean {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ) == PackageManager.PERMISSION_GRANTED
        }

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
        }

        else -> {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}

private val AttachmentSheetForeground = Color(0xFFF6D6CC)
