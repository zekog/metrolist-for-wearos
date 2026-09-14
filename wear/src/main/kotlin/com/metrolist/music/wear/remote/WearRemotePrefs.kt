/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.wear.remote

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.datastore.preferences.core.stringPreferencesKey
import com.metrolist.music.utils.rememberPreference

val WearRemoteModeKey = stringPreferencesKey("wearRemoteMode")

const val REMOTE_MODE_AUTO = "auto"
const val REMOTE_MODE_PHONE = "phone"
const val REMOTE_MODE_WATCH = "watch"

/**
 * Whether playback actions should target the phone instead of the watch's own player.
 *
 * `auto` follows the phone: as soon as the phone reports an active session the watch becomes a
 * remote, and falls back to standalone playback otherwise. `phone`/`watch` force a target.
 */
@Composable
fun rememberRemoteActive(): Boolean {
    val state by WearRemoteController.state.collectAsState()
    val (mode) = rememberPreference(WearRemoteModeKey, REMOTE_MODE_AUTO)
    return when (mode) {
        REMOTE_MODE_PHONE -> true
        REMOTE_MODE_WATCH -> false
        else -> state?.hasSession == true
    }
}
