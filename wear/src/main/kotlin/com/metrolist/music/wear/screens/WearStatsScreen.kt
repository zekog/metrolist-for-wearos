/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.wear.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.wear.components.WearArtistRow
import com.metrolist.music.wear.components.WearEmpty
import com.metrolist.music.wear.components.WearSectionHeader
import com.metrolist.music.wear.components.WearSongRow
import com.metrolist.music.wear.navigation.WearRoutes
import com.metrolist.music.wear.player.playSong
import java.time.LocalDateTime

@Composable
fun WearStatsScreen(navController: NavHostController) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val from = remember { LocalDateTime.now().minusMonths(12) }

    val topSongs by remember { database.mostPlayedSongs(from, limit = 15) }.collectAsState(emptyList())
    val topArtists by remember { database.mostPlayedArtists(from, limit = 15) }.collectAsState(emptyList())
    val plays by remember { database.eventCount() }.collectAsState(0)

    val listState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = listState, edgeButton = {}) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding, modifier = Modifier.fillMaxWidth()) {
            item { WearSectionHeader("Your stats") }
            item {
                Text(text = "$plays songs played", modifier = Modifier.fillMaxWidth())
            }

            item { WearSectionHeader("Top songs") }
            if (topSongs.isEmpty()) {
                item { WearEmpty("No listening history yet") }
            } else {
                items(topSongs, key = { it.id }) { song ->
                    WearSongRow(song = song, onClick = { playerConnection?.playSong(song) })
                }
            }

            item { WearSectionHeader("Top artists") }
            if (topArtists.isEmpty()) {
                item { WearEmpty("No listening history yet") }
            } else {
                items(topArtists, key = { it.id }) { artist ->
                    WearArtistRow(artist = artist, onClick = { navController.navigate(WearRoutes.artist(artist.id)) })
                }
            }
        }
    }
}
