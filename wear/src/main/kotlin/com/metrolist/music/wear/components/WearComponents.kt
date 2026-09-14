/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.wear.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import coil3.compose.AsyncImage
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.Song

@Composable
fun WearThumbnail(
    url: String?,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(size / 4)
    if (url.isNullOrBlank()) {
        Box(
            modifier =
                modifier
                    .size(size)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = WearIcons.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(size / 2),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = modifier.size(size).clip(shape),
        )
    }
}

@Composable
private fun WearRow(
    title: String,
    subtitle: String?,
    thumbnail: String?,
    onClick: () -> Unit,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            WearThumbnail(thumbnail)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (trailingIcon != null) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun artistsText(artists: List<com.metrolist.innertube.models.Artist>?): String? =
    artists?.joinToString { it.name }?.takeIf { it.isNotBlank() }

fun Song.subtitleText(): String = artists.joinToString { it.name }

@Composable
fun WearSongRow(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = WearRow(
    title = song.title,
    subtitle = song.subtitleText(),
    thumbnail = song.thumbnailUrl,
    onClick = onClick,
    modifier = modifier,
)

@Composable
fun WearSongItemRow(
    item: SongItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = WearRow(
    title = item.title,
    subtitle = artistsText(item.artists),
    thumbnail = item.thumbnail,
    onClick = onClick,
    modifier = modifier,
)

@Composable
fun WearAlbumRow(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = WearRow(
    title = album.title,
    subtitle = album.artists.joinToString { it.name },
    thumbnail = album.thumbnailUrl,
    onClick = onClick,
    trailingIcon = WearIcons.ChevronRight,
    modifier = modifier,
)

@Composable
fun WearAlbumItemRow(
    item: AlbumItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = WearRow(
    title = item.title,
    subtitle = artistsText(item.artists),
    thumbnail = item.thumbnail,
    onClick = onClick,
    trailingIcon = WearIcons.ChevronRight,
    modifier = modifier,
)

@Composable
fun WearArtistRow(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = WearRow(
    title = artist.title,
    subtitle = if (artist.songCount > 0) "${artist.songCount} songs" else null,
    thumbnail = artist.thumbnailUrl,
    onClick = onClick,
    trailingIcon = WearIcons.ChevronRight,
    modifier = modifier,
)

@Composable
fun WearArtistItemRow(
    item: ArtistItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = WearRow(
    title = item.title,
    subtitle = null,
    thumbnail = item.thumbnail,
    onClick = onClick,
    trailingIcon = WearIcons.ChevronRight,
    modifier = modifier,
)

@Composable
fun WearPlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = WearRow(
    title = playlist.title,
    subtitle = if (playlist.songCount > 0) "${playlist.songCount} songs" else null,
    thumbnail = playlist.thumbnails.firstOrNull(),
    onClick = onClick,
    trailingIcon = WearIcons.ChevronRight,
    modifier = modifier,
)

@Composable
fun WearPlaylistItemRow(
    item: PlaylistItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = WearRow(
    title = item.title,
    subtitle = item.songCountText ?: item.author?.name,
    thumbnail = item.thumbnail,
    onClick = onClick,
    trailingIcon = WearIcons.ChevronRight,
    modifier = modifier,
)

@Composable
fun WearSectionHeader(text: String) {
    ListHeader {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun WearEmpty(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
fun WearLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
