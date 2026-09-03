package com.heofen.botgram.ui.screens.mediaviewer

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.heofen.botgram.MessageType
import com.heofen.botgram.database.tables.Message
import com.heofen.botgram.database.tables.User
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    viewModel: MediaViewerViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    if (!uiState.isLoading && uiState.mediaMessages.isNotEmpty()) {
                        val currentIndex = uiState.initialIndex // TODO: sync with pager
                        // We will sync title based on current pager page later in the UI
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.mediaMessages.isEmpty()) {
                Text(
                    text = "No media found",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val pagerState = rememberPagerState(
                    initialPage = uiState.initialIndex,
                    pageCount = { uiState.mediaMessages.size }
                )
                
                val currentMessage = uiState.mediaMessages.getOrNull(pagerState.currentPage)
                val sender = currentMessage?.senderId?.let { uiState.senders[it] }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val message = uiState.mediaMessages[page]
                    MediaViewerItem(message = message)
                }

                // Overlay Top Header for current message
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column {
                        val senderName = sender?.let { "${it.firstName} ${it.lastName ?: ""}".trim() } ?: "Unknown"
                        Text(
                            text = senderName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        currentMessage?.let { msg ->
                            val timeStr = remember(msg.timestamp) {
                                Instant.ofEpochMilli(msg.timestamp)
                                    .atZone(ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))
                            }
                            Text(
                                text = timeStr,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediaViewerItem(message: Message) {
    val file = message.fileLocalPath?.let(::File)
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (file != null && file.exists()) {
            when (message.type) {
                MessageType.PHOTO -> {
                    ZoomableAsyncImage(
                        model = file,
                        contentDescription = "Photo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MessageType.VIDEO, MessageType.ANIMATION -> {
                    val zoomableState = rememberZoomableState()
                    ExoPlayerVideoView(
                        file = file,
                        modifier = Modifier
                            .fillMaxSize()
                            .zoomable(zoomableState)
                    )
                }
                else -> {
                    Text("Unsupported media type", color = Color.White)
                }
            }
        } else {
            CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
fun ExoPlayerVideoView(file: File, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val fileUri = remember(file.absolutePath) { Uri.fromFile(file) }
    
    val exoPlayer = remember(file.absolutePath) {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            val mediaItem = MediaItem.fromUri(fileUri)
            setMediaItem(mediaItem)
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    exoPlayer.play()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
    }
    
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        onRelease = { playerView ->
            playerView.player = null
        },
        modifier = modifier
    )
}
