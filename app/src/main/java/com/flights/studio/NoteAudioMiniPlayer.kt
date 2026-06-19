package com.flights.studio

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private enum class NoteAudioPlaybackState { Stopped, Playing, Paused }

@Composable
fun NoteAudioMiniPlayer(
    uri: Uri,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onRemove: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var state by remember(uri) { mutableStateOf(NoteAudioPlaybackState.Stopped) }
    var player by remember(uri) { mutableStateOf<MediaPlayer?>(null) }
    var progress by remember(uri) { mutableFloatStateOf(0f) }
    var durationMs by remember(uri) { mutableStateOf(0) }

    fun releasePlayer(resetProgress: Boolean) {
        runCatching { player?.pause() }
        runCatching { player?.release() }
        player = null
        state = NoteAudioPlaybackState.Stopped
        if (resetProgress) progress = 0f
    }

    fun ensurePlayer(): MediaPlayer? {
        player?.let { return it }
        val next = MediaPlayer.create(context, uri) ?: return null
        durationMs = next.duration.coerceAtLeast(1)
        next.setOnCompletionListener {
            releasePlayer(resetProgress = true)
        }
        player = next
        return next
    }

    fun togglePlayPause() {
        when (state) {
            NoteAudioPlaybackState.Playing -> {
                runCatching { player?.pause() }
                state = NoteAudioPlaybackState.Paused
            }
            NoteAudioPlaybackState.Paused -> {
                player?.start()
                state = NoteAudioPlaybackState.Playing
            }
            NoteAudioPlaybackState.Stopped -> {
                ensurePlayer()?.let {
                    it.start()
                    state = NoteAudioPlaybackState.Playing
                }
            }
        }
    }

    DisposableEffect(uri) {
        onDispose { releasePlayer(resetProgress = true) }
    }

    LaunchedEffect(state, player) {
        while (state == NoteAudioPlaybackState.Playing) {
            val current = player?.currentPosition ?: 0
            val duration = (durationMs.takeIf { it > 0 } ?: player?.duration ?: 1).coerceAtLeast(1)
            progress = (current.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            delay(90L)
        }
    }

    val cs = MaterialTheme.colorScheme
    val active = state != NoteAudioPlaybackState.Stopped

    Surface(
        modifier = modifier
            .height(42.dp),
        shape = RoundedCornerShape(999.dp),
        color = cs.primary.copy(alpha = if (active) 0.15f else 0.10f),
        border = BorderStroke(1.dp, cs.primary.copy(alpha = if (active) 0.34f else 0.18f)),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                modifier = Modifier
                    .height(28.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { togglePlayPause() },
                shape = RoundedCornerShape(999.dp),
                color = cs.primary.copy(alpha = if (state == NoteAudioPlaybackState.Playing) 0.24f else 0.14f)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state == NoteAudioPlaybackState.Playing) "Pause" else "Play",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = cs.primary,
                        maxLines = 1
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                    color = cs.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                NoteThinWaveLine(
                    progress = progress,
                    active = state == NoteAudioPlaybackState.Playing,
                    level = 0.58f,
                    color = cs.primary,
                    trackColor = cs.primary.copy(alpha = 0.18f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                )
            }

            if (active) {
                Text(
                    text = "Stop",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                    color = cs.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { releasePlayer(resetProgress = true) }
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    maxLines = 1
                )
            } else if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                    color = cs.primary,
                    maxLines = 1
                )
            }

            if (onRemove != null) {
                Text(
                    text = "Remove",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = cs.error,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable {
                            releasePlayer(resetProgress = true)
                            onRemove()
                        }
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NoteThinWaveLine(
    progress: Float,
    active: Boolean,
    level: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    if (active) {
        LinearWavyProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = modifier.height(3.dp),
            color = color,
            trackColor = trackColor,
            amplitude = { (0.10f + level.coerceIn(0f, 1f) * 0.18f).coerceIn(0.10f, 0.28f) },
            wavelength = 14.dp,
            waveSpeed = 16.dp
        )
    } else {
        Box(
            modifier = modifier
                .height(2.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(trackColor)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(2.dp)
                    .background(color.copy(alpha = 0.72f))
            )
        }
    }
}
