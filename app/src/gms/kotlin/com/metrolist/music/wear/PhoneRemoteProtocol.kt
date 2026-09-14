/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.wear

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Phone <-> watch remote control wire format. The watch sends short command strings and the
 * phone replies with a [RemoteState] snapshot serialized as JSON.
 */
object PhoneRemoteProtocol {
    const val COMMAND_PATH = "/metrolist/remote/command"
    const val STATE_PATH = "/metrolist/remote/state"
    const val REQUEST_STATE_PATH = "/metrolist/remote/request_state"

    const val CMD_TOGGLE = "toggle"
    const val CMD_PLAY = "play"
    const val CMD_PAUSE = "pause"
    const val CMD_NEXT = "next"
    const val CMD_PREV = "prev"
    const val CMD_LIKE = "like"
    const val CMD_SHUFFLE = "shuffle"
    const val CMD_REPEAT = "repeat"
    const val CMD_SEEK = "seek"
    const val CMD_SEEK_INDEX = "seekIndex"
    const val CMD_VOLUME = "volume"
    const val CMD_PLAY_ID = "playId"
    const val CMD_PLAY_PLAYLIST = "playPlaylist"

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
