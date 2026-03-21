package com.heofen.botgram.ui.components

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.heofen.botgram.database.tables.Message
import java.io.File
import kotlin.math.roundToLong
import kotlinx.coroutines.delay

internal data class VoicePlaybackTarget(
    val chatId: Long,
    val messageId: Long,
    val filePath: String
)

@Stable
class VoiceMessagePlaybackState(context: Context) {
    private val player = ExoPlayer.Builder(context.applicationContext).build()
    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            this@VoiceMessagePlaybackState.isPlaying = isPlaying
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            isBuffering = playbackState == Player.STATE_BUFFERING
            refreshPosition()

            if (playbackState == Player.STATE_ENDED) {
                isPlaying = false
                currentPositionMs = resolvedDurationMs().takeIf { it > 0L } ?: currentPositionMs
            }
        }
    }

    private var activeTarget by mutableStateOf<VoicePlaybackTarget?>(null)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var isBuffering by mutableStateOf(false)
        private set

    var currentPositionMs by mutableLongStateOf(0L)
        private set

    var totalDurationMs by mutableLongStateOf(0L)
        private set

    init {
        player.addListener(listener)
    }

    fun isActive(message: Message): Boolean =
        activeTarget?.let { target ->
            target.chatId == message.chatId &&
                target.messageId == message.messageId &&
                target.filePath == message.fileLocalPath
        } == true

    fun toggle(message: Message) {
        val file = message.fileLocalPath?.let(::File)
        if (file?.exists() != true) return

        val target = VoicePlaybackTarget(
            chatId = message.chatId,
            messageId = message.messageId,
            filePath = file.absolutePath
        )

        if (activeTarget != target) {
            activeTarget = target
            currentPositionMs = 0L
            totalDurationMs = (message.duration ?: 0L) * 1000L
            isBuffering = true
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            player.prepare()
            player.playWhenReady = true
            player.play()
            return
        }

        if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0L)
            currentPositionMs = 0L
        }

        if (player.isPlaying) {
            player.pause()
        } else {
            player.playWhenReady = true
            player.play()
        }
    }

    fun seekToFraction(message: Message, fraction: Float) {
        if (!isActive(message)) return

        val duration = resolvedDurationMs().takeIf { it > 0L } ?: return
        val targetPosition = (duration * fraction.coerceIn(0f, 1f)).roundToLong()
        player.seekTo(targetPosition)
        currentPositionMs = targetPosition
    }

    fun progressFor(message: Message): Float {
        if (!isActive(message)) return 0f

        val duration = resolvedDurationMs().takeIf { it > 0L } ?: return 0f
        return (currentPositionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }

    fun elapsedSecondsFor(message: Message): Long =
        if (isActive(message)) currentPositionMs / 1000L else 0L

    internal val hasActivePlayback: Boolean
        get() = activeTarget != null

    fun release() {
        player.removeListener(listener)
        player.release()
    }

    internal fun refreshPosition() {
        currentPositionMs = player.currentPosition.coerceAtLeast(0L)
        totalDurationMs = player.duration.takeIf { it > 0L } ?: totalDurationMs
    }

    private fun resolvedDurationMs(): Long =
        player.duration.takeIf { it > 0L } ?: totalDurationMs
}

@Composable
fun rememberVoiceMessagePlaybackState(): VoiceMessagePlaybackState {
    val context = LocalContext.current.applicationContext
    val playbackState = remember(context) { VoiceMessagePlaybackState(context) }

    LaunchedEffect(playbackState, playbackState.hasActivePlayback, playbackState.isPlaying, playbackState.isBuffering) {
        while (playbackState.hasActivePlayback) {
            playbackState.refreshPosition()
            delay(if (playbackState.isPlaying || playbackState.isBuffering) 90L else 220L)
        }
    }

    return playbackState
}
