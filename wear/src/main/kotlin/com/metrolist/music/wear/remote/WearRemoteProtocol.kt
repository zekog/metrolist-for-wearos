/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.wear.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Watch-side counterpart of the phone's remote protocol. Paths, commands and payloads must stay
 * in sync with `com.metrolist.music.wear.PhoneRemoteProtocol` in the phone app.
 */
object WearRemoteProtocol {
    const val COMMAND_PATH = "/metrolist/remote/command"
    const val STATE_PATH = "/metrolist/remote/state"
    const val REQUEST_STATE_PATH = "/metrolist/remote/request_state"

    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
}

@Serializable
data class RemoteQueueItem(
    val id: String,
    val title: String,
    val artist: String? = null,
    val artwork: String? = null,
)

@Serializable
data class RemoteState(
    val hasSession: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String? = null,
    val artist: String? = null,
    val artwork: String? = null,
    val duration: Long = 0,
    val position: Long = 0,
    val mediaId: String? = null,
    val currentIndex: Int = -1,
    val liked: Boolean = false,
    val shuffle: Boolean = false,
    val repeatMode: Int = 0,
    val volume: Float = 1f,
    val queue: List<RemoteQueueItem> = emptyList(),
)
