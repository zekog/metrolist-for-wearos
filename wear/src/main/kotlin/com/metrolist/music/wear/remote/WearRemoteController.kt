/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.wear.remote

import android.content.Context
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton that mirrors the phone's playback state and forwards control commands over the
 * Wearable Data Layer. Initialized lazily from whichever component touches it first (activity
 * or listener service), since both run in the same process.
 */
object WearRemoteController {
    private val _state = MutableStateFlow<RemoteState?>(null)
    val state: StateFlow<RemoteState?> = _state.asStateFlow()

    private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    fun onState(state: RemoteState) {
        _state.value = state
    }

    fun send(command: String) {
        sendOnPath(WearRemoteProtocol.COMMAND_PATH, command)
    }

    fun requestState() {
        sendOnPath(WearRemoteProtocol.REQUEST_STATE_PATH, "1")
    }

    private fun sendOnPath(
        path: String,
        payload: String,
    ) {
        val context = appContext ?: return
        Wearable
            .getNodeClient(context)
            .connectedNodes
            .addOnSuccessListener { nodes ->
                val messageClient = Wearable.getMessageClient(context)
                val bytes = payload.toByteArray(Charsets.UTF_8)
                nodes.forEach { node -> messageClient.sendMessage(node.id, path, bytes) }
            }
    }
}
