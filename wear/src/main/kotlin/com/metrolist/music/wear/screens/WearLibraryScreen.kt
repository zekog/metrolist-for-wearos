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
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.constants.AlbumSortType
import com.metrolist.music.constants.ArtistSortType
import com.metrolist.music.constants.ArtistSongSortType
import com.metrolist.music.constants.PlaylistSortType
import com.metrolist.music.constants.SongSortType
import com.metrolist.music.wear.components.WearAlbumRow
import com.metrolist.music.wear.components.WearArtistRow
import com.metrolist.music.wear.components.WearEmpty
import com.metrolist.music.wear.components.WearIcons
import com.metrolist.music.wear.components.WearPlaylistRow
import com.metrolist.music.wear.components.WearSectionHeader
import com.metrolist.music.wear.components.WearSongRow
import com.metrolist.music.wear.navigation.WearRoutes
import com.metrolist.music.wear.player.playSongs

@Composable
fun WearLibraryScreen(navController: NavHostController) {
    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState, edgeButton = {}) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding, modifier = Modifier.fillMaxWidth()) {
            item { WearSectionHeader("Library") }
            item { NavRow("Songs", WearIcons.MusicNote) { navController.navigate(WearRoutes.SONGS) } }
            item { NavRow("Albums", WearIcons.LibraryMusic) { navController.navigate(WearRoutes.ALBUMS) } }
            item { NavRow("Artists", WearIcons.Stats) { navController.navigate(WearRoutes.ARTISTS) } }
            item { NavRow("Playlists", WearIcons.QueueMusic) { navController.navigate(WearRoutes.PLAYLISTS) } }
            item { NavRow("Downloads", WearIcons.Download) { navController.navigate(WearRoutes.DOWNLOADS) } }
        }
    }
}

@Composable
private fun NavRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

@Composable
fun WearSongsScreen() {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val songs by remember { database.songs(SongSortType.CREATE_DATE, true) }.collectAsState(emptyList())
    val listState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = listState, edgeButton = {}) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding, modifier = Modifier.fillMaxWidth()) {
            item { WearSectionHeader("Songs") }
            if (songs.isEmpty()) {
                item { WearEmpty("Nothing here yet") }
            } else {
                item {
                    PlayAllRow { playerConnection?.playSongs(songs, "Songs", 0) }
                }
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    WearSongRow(song = song, onClick = { playerConnection?.playSongs(songs, "Songs", index) })
                }
            }
        }
    }
}

@Composable
fun WearDownloadsScreen() {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val songs by remember { database.downloadedSongs(SongSortType.CREATE_DATE, true) }.collectAsState(emptyList())
    val listState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = listState, edgeButton = {}) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding, modifier = Modifier.fillMaxWidth()) {
            item { WearSectionHeader("Downloads") }
            if (songs.isEmpty()) {
                item { WearEmpty("No downloads") }
            } else {
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    WearSongRow(song = song, onClick = { playerConnection?.playSongs(songs, "Downloads", index) })
                }
            }
        }
    }
}

@Composable
fun WearAlbumsScreen(navController: NavHostController) {
    val database = LocalDatabase.current
    val albums by remember { database.albums(AlbumSortType.CREATE_DATE, true) }.collectAsState(emptyList())
    val listState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = listState, edgeButton = {}) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding, modifier = Modifier.fillMaxWidth()) {
            item { WearSectionHeader("Albums") }
            if (albums.isEmpty()) {
                item { WearEmpty("Nothing here yet") }
            } else {
                items(albums, key = { it.id }) { album ->
                    WearAlbumRow(album = album, onClick = { navController.navigate(WearRoutes.album(album.id)) })
                }
            }
        }
    }
}

@Composable
fun WearArtistsScreen(navController: NavHostController) {
    val database = LocalDatabase.current
    val artists by remember { database.artists(ArtistSortType.CREATE_DATE, true) }.collectAsState(emptyList())
    val listState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = listState, edgeButton = {}) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding, modifier = Modifier.fillMaxWidth()) {
            item { WearSectionHeader("Artists") }
            if (artists.isEmpty()) {
                item { WearEmpty("Nothing here yet") }
            } else {
                items(artists, key = { it.id }) { artist ->
                    WearArtistRow(artist = artist, onClick = { navController.navigate(WearRoutes.artist(artist.id)) })
                }
            }
        }
    }
}

@Composable
fun WearPlaylistsScreen(navController: NavHostController) {
    val database = LocalDatabase.current
    val playlists by remember { database.playlists(PlaylistSortType.CREATE_DATE, true) }.collectAsState(emptyList())
    val listState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = listState, edgeButton = {}) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding, modifier = Modifier.fillMaxWidth()) {
            item { WearSectionHeader("Playlists") }
            if (playlists.isEmpty()) {
                item { WearEmpty("Nothing here yet") }
            } else {
                items(playlists, key = { it.id }) { playlist ->
                    WearPlaylistRow(playlist = playlist, onClick = { navController.navigate(WearRoutes.playlist(playlist.id)) })
                }
            }
        }
    }
}

@Composable
fun WearAlbumScreen(albumId: String) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val album by remember(albumId) { database.albumWithSongs(albumId) }.collectAsState(null)
    val listState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = listState, edgeButton = {}) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding, modifier = Modifier.fillMaxWidth()) {
            val songs = album?.songs.orEmpty()
            item { WearSectionHeader(album?.album?.title ?: "Album") }
            if (songs.isEmpty()) {
                item { WearEmpty("Nothing here yet") }
            } else {
                item { PlayAllRow { playerConnection?.playSongs(songs, album?.album?.title, 0) } }
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    WearSongRow(song = song, onClick = { playerConnection?.playSongs(songs, album?.album?.title, index) })
                }
            }
        }
    }
}

@Composable
fun WearArtistScreen(artistId: String) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val songs by
        remember(artistId) { database.artistSongs(artistId, ArtistSongSortType.CREATE_DATE, true) }
            .collectAsState(emptyList())
    val listState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = listState, edgeButton = {}) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding, modifier = Modifier.fillMaxWidth()) {
            item { WearSectionHeader("Artist") }
            if (songs.isEmpty()) {
                item { WearEmpty("Nothing here yet") }
            } else {
                item { PlayAllRow { playerConnection?.playSongs(songs, "Artist", 0) } }
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    WearSongRow(song = song, onClick = { playerConnection?.playSongs(songs, "Artist", index) })
                }
            }
        }
    }
}

@Composable
fun WearPlaylistScreen(playlistId: String) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val playlist by remember(playlistId) { database.playlist(playlistId) }.collectAsState(null)
    val playlistSongs by remember(playlistId) { database.playlistSongs(playlistId) }.collectAsState(emptyList())
    val listState = rememberScalingLazyListState()

    val songs = playlistSongs.map { it.song }

    ScreenScaffold(scrollState = listState, edgeButton = {}) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding, modifier = Modifier.fillMaxWidth()) {
            item { WearSectionHeader(playlist?.title ?: "Playlist") }
            if (songs.isEmpty()) {
                item { WearEmpty("Nothing here yet") }
            } else {
                item { PlayAllRow { playerConnection?.playSongs(songs, playlist?.title, 0) } }
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    WearSongRow(song = song, onClick = { playerConnection?.playSongs(songs, playlist?.title, index) })
                }
            }
        }
    }
}

@Composable
private fun PlayAllRow(onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(imageVector = WearIcons.Play, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text = "Play all", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
