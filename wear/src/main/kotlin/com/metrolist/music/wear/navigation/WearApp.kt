/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.wear.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.metrolist.music.wear.screens.WearAlbumScreen
import com.metrolist.music.wear.screens.WearAlbumsScreen
import com.metrolist.music.wear.screens.WearArtistScreen
import com.metrolist.music.wear.screens.WearArtistsScreen
import com.metrolist.music.wear.screens.WearDownloadsScreen
import com.metrolist.music.wear.screens.WearHomeScreen
import com.metrolist.music.wear.screens.WearLibraryScreen
import com.metrolist.music.wear.screens.WearLoginScreen
import com.metrolist.music.wear.screens.WearLyricsScreen
import com.metrolist.music.wear.screens.WearPlayerScreen
import com.metrolist.music.wear.screens.WearPlaylistScreen
import com.metrolist.music.wear.screens.WearPlaylistsScreen
import com.metrolist.music.wear.screens.WearQueueScreen
import com.metrolist.music.wear.screens.WearSearchScreen
import com.metrolist.music.wear.screens.WearSettingsScreen
import com.metrolist.music.wear.screens.WearSongsScreen
import com.metrolist.music.wear.screens.WearStatsScreen
import com.metrolist.music.wear.remote.WearRemoteController
import kotlinx.coroutines.delay

object WearRoutes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val SONGS = "songs"
    const val ALBUMS = "albums"
    const val ARTISTS = "artists"
    const val PLAYLISTS = "playlists"
    const val DOWNLOADS = "downloads"
    const val SETTINGS = "settings"
    const val STATS = "stats"
    const val PLAYER = "player"
    const val QUEUE = "queue"
    const val LYRICS = "lyrics"
    const val LOGIN = "login"

    const val ALBUM = "album/{id}"
    const val ARTIST = "artist/{id}"
    const val PLAYLIST = "playlist/{id}"

    const val ARG_ID = "id"

    fun album(id: String) = "album/$id"

    fun artist(id: String) = "artist/$id"

    fun playlist(id: String) = "playlist/$id"
}

@Composable
fun WearApp() {
    val navController = rememberSwipeDismissableNavController()

    // Discover the phone's session so "Automatic" mode can switch to remote control.
    LaunchedEffect(Unit) {
        while (true) {
            WearRemoteController.requestState()
            delay(5000)
        }
    }

    AppScaffold {
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = WearRoutes.HOME,
        ) {
            composable(WearRoutes.HOME) { WearHomeScreen(navController) }
            composable(WearRoutes.SEARCH) { WearSearchScreen(navController) }
            composable(WearRoutes.LIBRARY) { WearLibraryScreen(navController) }
            composable(WearRoutes.SONGS) { WearSongsScreen() }
            composable(WearRoutes.ALBUMS) { WearAlbumsScreen(navController) }
            composable(WearRoutes.ARTISTS) { WearArtistsScreen(navController) }
            composable(WearRoutes.PLAYLISTS) { WearPlaylistsScreen(navController) }
            composable(WearRoutes.DOWNLOADS) { WearDownloadsScreen() }
            composable(WearRoutes.SETTINGS) { WearSettingsScreen(navController) }
            composable(WearRoutes.STATS) { WearStatsScreen(navController) }
            composable(WearRoutes.PLAYER) { WearPlayerScreen(navController) }
            composable(WearRoutes.QUEUE) { WearQueueScreen() }
            composable(WearRoutes.LYRICS) { WearLyricsScreen() }
            composable(WearRoutes.LOGIN) { WearLoginScreen(navController) }

            composable(
                route = WearRoutes.ALBUM,
                arguments = listOf(navArgument(WearRoutes.ARG_ID) { type = NavType.StringType }),
            ) { entry ->
                WearAlbumScreen(entry.arguments?.getString(WearRoutes.ARG_ID).orEmpty())
            }

            composable(
                route = WearRoutes.ARTIST,
                arguments = listOf(navArgument(WearRoutes.ARG_ID) { type = NavType.StringType }),
            ) { entry ->
                WearArtistScreen(entry.arguments?.getString(WearRoutes.ARG_ID).orEmpty())
            }

            composable(
                route = WearRoutes.PLAYLIST,
                arguments = listOf(navArgument(WearRoutes.ARG_ID) { type = NavType.StringType }),
            ) { entry ->
                WearPlaylistScreen(entry.arguments?.getString(WearRoutes.ARG_ID).orEmpty())
            }
        }
    }
}
