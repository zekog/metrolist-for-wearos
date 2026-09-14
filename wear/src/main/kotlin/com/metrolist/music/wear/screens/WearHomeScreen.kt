/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.wear.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.db.entities.Song
import com.metrolist.music.wear.components.WearEmpty
import com.metrolist.music.wear.components.WearIcons
import com.metrolist.music.wear.components.WearSectionHeader
import com.metrolist.music.wear.components.WearSongRow
import com.metrolist.music.wear.components.WearThumbnail
import com.metrolist.music.wear.navigation.WearRoutes
import com.metrolist.music.wear.player.playSongs
import com.metrolist.music.wear.remote.WearRemoteController
import com.metrolist.music.wear.remote.rememberRemoteActive
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun WearHomeScreen(navController: NavHostController) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val remoteActive = rememberRemoteActive()
    val remoteState by WearRemoteController.state.collectAsState()

    val quickPicks by remember { database.quickPicks() }.collectAsState(emptyList())
    val fallbackSong = remember { MutableStateFlow<Song?>(null) }
    val fallbackPlaying = remember { MutableStateFlow(false) }
    val currentSong by (playerConnection?.currentSong ?: fallbackSong).collectAsState()
    val isPlaying by (playerConnection?.isPlaying ?: fallbackPlaying).collectAsState()

    val nowTitle = if (remoteActive) remoteState?.title else currentSong?.title
    val nowArtwork = if (remoteActive) remoteState?.artwork else currentSong?.thumbnailUrl
    val nowPlaying = if (remoteActive) remoteState?.isPlaying == true else isPlaying

    val listState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = listState, edgeButton = {}) { contentPadding ->
        ScalingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxWidth(),
        ) {
            item { WearSectionHeader("Metrolist") }

            if (nowTitle != null) {
                item {
                    Card(
                        onClick = { navController.navigate(WearRoutes.PLAYER) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            WearThumbnail(nowArtwork, size = 40.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = nowTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text =
                                        if (remoteActive) {
                                            if (nowPlaying) "Now playing on phone" else "Phone paused"
                                        } else {
                                            if (nowPlaying) "Now playing" else "Paused"
                                        },
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Icon(
                                imageVector = if (nowPlaying) WearIcons.Pause else WearIcons.Play,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            item { NavItem("Search", WearIcons.Search) { navController.navigate(WearRoutes.SEARCH) } }
            item { NavItem("Library", WearIcons.LibraryMusic) { navController.navigate(WearRoutes.LIBRARY) } }
            item { NavItem("Your stats", WearIcons.Stats) { navController.navigate(WearRoutes.STATS) } }
            item { NavItem("Settings", WearIcons.Settings) { navController.navigate(WearRoutes.SETTINGS) } }

            item { WearSectionHeader("Quick picks") }

            if (quickPicks.isEmpty()) {
                item { WearEmpty("Nothing here yet") }
            } else {
                itemsIndexed(quickPicks) { index, song ->
                    WearSongRow(
                        song = song,
                        onClick = {
                            if (remoteActive) {
                                WearRemoteController.send("playId:${song.id}")
                            } else {
                                playerConnection?.playSongs(quickPicks, "Quick picks", index)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
