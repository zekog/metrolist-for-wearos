/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.wear.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Timeline
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.wear.components.WearEmpty
import com.metrolist.music.wear.components.WearSectionHeader
import com.metrolist.music.wear.components.WearThumbnail
import com.metrolist.music.wear.remote.WearRemoteController
import com.metrolist.music.wear.remote.rememberRemoteActive
import kotlinx.coroutines.flow.MutableStateFlow

private data class QueueRow(
    val title: String,
    val artist: String?,
    val artwork: String?,
)

@Composable
fun WearQueueScreen() {
    val playerConnection = LocalPlayerConnection.current
    val remoteActive = rememberRemoteActive()
    val remoteState by WearRemoteController.state.collectAsState()

    val fallbackWindows = remember { MutableStateFlow<List<Timeline.Window>>(emptyList()) }
    val fallbackIndex = remember { MutableStateFlow(-1) }
    val windows by (playerConnection?.queueWindows ?: fallbackWindows).collectAsState()
    val localIndex by (playerConnection?.currentWindowIndex ?: fallbackIndex).collectAsState()

    val rows: List<QueueRow>
    val currentIndex: Int
    if (remoteActive) {
        rows =
            remoteState?.queue.orEmpty().map {
                QueueRow(title = it.title, artist = it.artist, artwork = it.artwork)
            }
        currentIndex = remoteState?.currentIndex ?: -1
    } else {
        rows =
            windows.map { window ->
                val metadata = window.mediaItem.mediaMetadata
                QueueRow(
                    title = metadata.title?.toString().orEmpty(),
                    artist = (metadata.artist ?: metadata.subtitle)?.toString(),
                    artwork =
                        metadata.extras?.getString("artwork_uri")
                            ?: metadata.artworkUri?.toString(),
                )
            }
        currentIndex = localIndex
    }

    val listState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = listState, edgeButton = {}) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding, modifier = Modifier.fillMaxWidth()) {
            item { WearSectionHeader(if (remoteActive) "Queue (phone)" else "Queue") }
            if (rows.isEmpty()) {
                item { WearEmpty("Queue is empty") }
            } else {
                itemsIndexed(rows) { index, row ->
                    val isCurrent = index == currentIndex
                    Card(
                        onClick = {
                            if (remoteActive) {
                                WearRemoteController.send("seekIndex:$index")
                            } else {
                                runCatching {
                                    playerConnection?.player?.seekTo(index, 0L)
                                    playerConnection?.play()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            WearThumbnail(row.artwork, size = 36.dp)
                            Text(
                                text = row.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color =
                                    if (isCurrent) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                modifier = Modifier.weight(1f),
                            )
                            if (!row.artist.isNullOrBlank()) {
                                Text(
                                    text = row.artist,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
