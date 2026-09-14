/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.wear.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import coil3.compose.AsyncImage
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.db.entities.Song
import com.metrolist.music.wear.components.WearIcons
import com.metrolist.music.wear.navigation.WearRoutes
import com.metrolist.music.wear.remote.WearRemoteController
import com.metrolist.music.wear.remote.rememberRemoteActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun WearPlayerScreen(navController: NavHostController) {
    val playerConnection = LocalPlayerConnection.current
    val remoteActive = rememberRemoteActive()
    val remoteState by WearRemoteController.state.collectAsState()

    val fallbackSong = remember { MutableStateFlow<Song?>(null) }
    val fallbackPlaying = remember { MutableStateFlow(false) }
    val localSong by (playerConnection?.currentSong ?: fallbackSong).collectAsState()
    val localPlaying by (playerConnection?.isPlaying ?: fallbackPlaying).collectAsState()

    var localPosition by remember { mutableLongStateOf(0L) }
    var localDuration by remember { mutableLongStateOf(0L) }
    var localVolume by remember { mutableFloatStateOf(1f) }
    var showVolume by remember { mutableStateOf(false) }

    LaunchedEffect(playerConnection, remoteActive) {
        if (remoteActive) return@LaunchedEffect
        val player = runCatching { playerConnection?.player }.getOrNull()
        if (player != null) localVolume = player.volume
        while (true) {
            val p = runCatching { playerConnection?.player }.getOrNull()
            if (p != null) {
                localPosition = p.currentPosition.coerceAtLeast(0L)
                localDuration = p.duration.coerceAtLeast(0L)
            }
            delay(500)
        }
    }

    LaunchedEffect(remoteActive) {
        if (remoteActive) WearRemoteController.requestState()
    }

    LaunchedEffect(localVolume, remoteState?.volume) {
        showVolume = true
        delay(1200)
        showVolume = false
    }

    val title = if (remoteActive) remoteState?.title else localSong?.title
    val artist =
        if (remoteActive) {
            remoteState?.artist
        } else {
            localSong?.artists?.joinToString { it.name }
        }
    val artwork = if (remoteActive) remoteState?.artwork else localSong?.thumbnailUrl
    val isPlaying = if (remoteActive) remoteState?.isPlaying == true else localPlaying
    val duration = if (remoteActive) remoteState?.duration ?: 0L else localDuration
    val position = if (remoteActive) remoteState?.position ?: 0L else localPosition
    val liked = if (remoteActive) remoteState?.liked == true else localSong?.song?.liked == true
    val volume = if (remoteActive) remoteState?.volume ?: 1f else localVolume

    fun togglePlay() {
        if (remoteActive) WearRemoteController.send("toggle") else playerConnection?.togglePlayPause()
    }

    fun next() {
        if (remoteActive) WearRemoteController.send("next") else playerConnection?.seekToNext()
    }

    fun previous() {
        if (remoteActive) WearRemoteController.send("prev") else playerConnection?.seekToPrevious()
    }

    fun toggleLike() {
        if (remoteActive) WearRemoteController.send("like") else playerConnection?.toggleLike()
    }

    fun setVolume(value: Float) {
        val coerced = value.coerceIn(0f, 1f)
        if (remoteActive) {
            WearRemoteController.send("volume:${(coerced * 100).toInt()}")
        } else {
            runCatching { playerConnection?.player?.volume = coerced }
            localVolume = coerced
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    val fraction = if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onRotaryScrollEvent { event ->
                    if (remoteActive) {
                        setVolume(volume + event.verticalScrollPixels / 320f)
                    } else {
                        setVolume(localVolume + event.verticalScrollPixels / 320f)
                    }
                    true
                },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = artwork,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))

        CircularProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxSize().padding(5.dp),
            strokeWidth = 4.dp,
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title ?: "Nothing playing",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp),
            )
            Text(
                text = artist.orEmpty(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp),
            )
            FilledTonalIconButton(
                onClick = { togglePlay() },
                modifier = Modifier.size(52.dp),
            ) {
                Icon(
                    imageVector = if (isPlaying) WearIcons.Pause else WearIcons.Play,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                )
            }
            if (showVolume) {
                Text(text = "Volume ${(volume * 100).toInt()}%", color = Color.White)
            }
        }

        FilledIconButton(
            onClick = { previous() },
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp).size(40.dp),
        ) {
            Icon(WearIcons.SkipPrevious, contentDescription = "Previous")
        }
        FilledIconButton(
            onClick = { next() },
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp).size(40.dp),
        ) {
            Icon(WearIcons.SkipNext, contentDescription = "Next")
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
        ) {
            FilledIconButton(
                onClick = { toggleLike() },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = if (liked) WearIcons.Favorite else WearIcons.FavoriteBorder,
                    contentDescription = "Like",
                )
            }
            FilledIconButton(
                onClick = { navController.navigate(WearRoutes.LYRICS) },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(WearIcons.Lyrics, contentDescription = "Lyrics")
            }
            FilledIconButton(
                onClick = { navController.navigate(WearRoutes.QUEUE) },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(WearIcons.QueueMusic, contentDescription = "Queue")
            }
        }
    }
}
