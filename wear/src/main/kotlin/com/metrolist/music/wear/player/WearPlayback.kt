/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.wear.player

import com.metrolist.innertube.models.SongItem
import com.metrolist.music.db.entities.AlbumWithSongs
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.LocalAlbumRadio
import com.metrolist.music.playback.queues.YouTubeAlbumRadio
import com.metrolist.music.playback.queues.YouTubePlaylistQueue
import com.metrolist.music.playback.queues.YouTubeQueue

fun PlayerConnection.playSongs(
    songs: List<Song>,
    title: String? = null,
    startIndex: Int = 0,
) {
    if (songs.isEmpty()) return
    playQueue(
        ListQueue(
            title = title ?: songs.getOrNull(startIndex)?.title,
            items = songs.map { it.toMediaItem() },
            startIndex = startIndex,
        ),
    )
}

fun PlayerConnection.playSong(song: Song) = playSongs(listOf(song), song.title)

fun PlayerConnection.playSongItem(
    item: SongItem,
    radio: Boolean = true,
) {
    if (radio) {
        playQueue(YouTubeQueue.radio(item.toMediaMetadata()))
    } else {
        playQueue(ListQueue(title = item.title, items = listOf(item.toMediaItem())))
    }
}

fun PlayerConnection.playYouTubePlaylist(
    id: String,
    title: String? = null,
) = playQueue(YouTubePlaylistQueue(playlistId = id, playlistTitle = title))

fun PlayerConnection.playYouTubeAlbum(playlistId: String) = playQueue(YouTubeAlbumRadio(playlistId))

fun PlayerConnection.playLocalAlbum(album: AlbumWithSongs) = playQueue(LocalAlbumRadio(album))
