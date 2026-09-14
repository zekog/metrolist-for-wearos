/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.wear.remote

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import timber.log.Timber

class WearRemoteListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != WearRemoteProtocol.STATE_PATH) return
        val state =
            runCatching {
                WearRemoteProtocol.json.decodeFromString<RemoteState>(String(event.data, Charsets.UTF_8))
            }.getOrNull() ?: return
        WearRemoteController.init(applicationContext)
        WearRemoteController.onState(state)
        Timber.tag("WearRemote").d("Phone state: ${state.title} playing=${state.isPlaying}")
    }
}
