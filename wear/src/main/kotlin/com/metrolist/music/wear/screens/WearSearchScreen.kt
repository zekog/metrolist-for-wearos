/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.wear.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.pages.SearchSummary
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.wear.components.WearAlbumItemRow
import com.metrolist.music.wear.components.WearArtistItemRow
import com.metrolist.music.wear.components.WearEmpty
import com.metrolist.music.wear.components.WearIcons
import com.metrolist.music.wear.components.WearLoading
import com.metrolist.music.wear.components.WearPlaylistItemRow
import com.metrolist.music.wear.components.WearSectionHeader
import com.metrolist.music.wear.components.WearSongItemRow
import com.metrolist.music.wear.player.playSongItem
import com.metrolist.music.wear.player.playYouTubeAlbum
import com.metrolist.music.wear.player.playYouTubePlaylist
import com.metrolist.music.wear.remote.WearRemoteController
import com.metrolist.music.wear.remote.rememberRemoteActive
import kotlinx.coroutines.delay

@Composable
fun WearSearchScreen(navController: NavHostController) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val remoteActive = rememberRemoteActive()

    var query by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var summaries by remember { mutableStateOf<List<SearchSummary>>(emptyList()) }

    val speechLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data
                    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.firstOrNull()
                    ?.let { query = it }
            }
        }

    fun launchVoiceSearch() {
        val intent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Search music")
            }
        try {
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            // No speech recognizer available; the user can still type.
        }
    }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            summaries = emptyList()
            return@LaunchedEffect
        }
        loading = true
        delay(350)
        summaries = YouTube.searchSummary(query.trim()).getOrNull()?.summaries.orEmpty()
        loading = false
    }

    val listState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = listState, edgeButton = {}) { contentPadding ->
        ScalingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxWidth(),
        ) {
            item {
                WearSectionHeader("Search")
            }

            if (remoteActive) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Playing on phone",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            item {
                Card(onClick = { launchVoiceSearch() }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = WearIcons.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(text = "Voice search", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle =
                            MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    RoundedCornerShape(16.dp),
                                ).padding(12.dp),
                        decorationBox = { innerTextField ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search music",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                            innerTextField()
                        },
                    )
                }
            }

            if (loading) {
                item { WearLoading() }
            } else if (query.isNotBlank() && summaries.isEmpty()) {
                item { WearEmpty("No results") }
            }

            summaries.forEach { summary ->
                item { WearSectionHeader(summary.title) }
                items(summary.items, key = { it.id }) { item ->
                    when (item) {
                        is SongItem ->
                            WearSongItemRow(
                                item = item,
                                onClick = {
                                    if (remoteActive) {
                                        WearRemoteController.send("playId:${item.id}")
                                    } else {
                                        playerConnection?.playSongItem(item)
                                    }
                                },
                            )

                        is AlbumItem ->
                            WearAlbumItemRow(
                                item = item,
                                onClick = {
                                    if (remoteActive) {
                                        WearRemoteController.send("playPlaylist:${item.playlistId}")
                                    } else {
                                        playerConnection?.playYouTubeAlbum(item.playlistId)
                                    }
                                },
                            )

                        is ArtistItem ->
                            WearArtistItemRow(
                                item = item,
                                onClick = {
                                    val radioPlaylistId = item.radioEndpoint?.playlistId
                                    if (remoteActive && radioPlaylistId != null) {
                                        WearRemoteController.send("playPlaylist:$radioPlaylistId")
                                    } else {
                                        item.radioEndpoint?.let { endpoint ->
                                            playerConnection?.playQueue(
                                                com.metrolist.music.playback.queues.YouTubeQueue(endpoint),
                                            )
                                        }
                                    }
                                },
                            )

                        is PlaylistItem ->
                            WearPlaylistItemRow(
                                item = item,
                                onClick = {
                                    if (remoteActive) {
                                        WearRemoteController.send("playPlaylist:${item.id}")
                                    } else {
                                        playerConnection?.playYouTubePlaylist(item.id, item.title)
                                    }
                                },
                            )

                        is EpisodeItem ->
                            WearSongItemRow(
                                item = item.asSongItem(),
                                onClick = {
                                    if (remoteActive) {
                                        WearRemoteController.send("playId:${item.id}")
                                    } else {
                                        playerConnection?.playSongItem(item.asSongItem())
                                    }
                                },
                            )

                        else -> Unit
                    }
                }
            }
        }
    }
}
