/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.wear

import android.content.ComponentName
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.metrolist.music.constants.MediaSessionConstants
import com.metrolist.music.playback.MusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Lets the paired watch act as a remote for the phone's playback. Uses a local Media3
 * [MediaController] connected to [MusicService]'s session, so it shares the exact same queue,
 * metadata and controls as the phone UI.
 */
class PhoneRemoteService : WearableListenerService() {
    private var controller: MediaController? = null
    private var building = false
    private val pendingCommands = ArrayDeque<String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var positionJob: Job? = null

    private val playerListener =
        object : Player.Listener {
            override fun onEvents(
                player: Player,
                events: Player.Events,
            ) {
                pushState()
            }
        }

    override fun onCreate() {
        super.onCreate()
        buildController()
    }

    private fun buildController() {
        if (controller != null || building) return
        building = true
        val token = SessionToken(this, ComponentName(this, MusicService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        future.addListener(
            {
                building = false
                runCatching {
                    val c = future.get()
                    controller = c
                    c.addListener(playerListener)
                    startPositionUpdates()
                    pushState()
                    // Flush commands that arrived before the controller finished connecting.
                    while (pendingCommands.isNotEmpty()) {
                        handleCommand(pendingCommands.removeFirst())
                    }
                }.onFailure { Timber.tag("PhoneRemote").w(it, "Failed to connect controller") }
            },
            ContextCompat.getMainExecutor(this),
        )
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob =
            scope.launch {
                while (isActive) {
                    val c = controller ?: break
                    if (c.isPlaying) pushState()
                    delay(1000)
                }
            }
    }

    override fun onMessageReceived(event: MessageEvent) {
        // WearableListenerService delivers on a binder thread, but MediaController must be
        // used from the application main thread.
        scope.launch {
            when (event.path) {
                PhoneRemoteProtocol.COMMAND_PATH -> handleCommand(String(event.data))
                PhoneRemoteProtocol.REQUEST_STATE_PATH -> pushState()
            }
        }
    }

    private fun handleCommand(raw: String) {
        val parts = raw.split(":", limit = 2)
        val cmd = parts[0]
        val arg = parts.getOrNull(1)
        Timber.tag("PhoneRemote").d("command=%s arg=%s controller=%s", cmd, arg, controller != null)
        val c = controller
        if (c == null) {
            // Controller is still connecting (or the service was just recreated); retry later.
            pendingCommands.addLast(raw)
            buildController()
            return
        }
        when (cmd) {
            PhoneRemoteProtocol.CMD_TOGGLE -> if (c.isPlaying) c.pause() else c.play()
            PhoneRemoteProtocol.CMD_PLAY -> c.play()
            PhoneRemoteProtocol.CMD_PAUSE -> c.pause()
            PhoneRemoteProtocol.CMD_NEXT -> c.seekToNextMediaItem()
            PhoneRemoteProtocol.CMD_PREV -> c.seekToPreviousMediaItem()
            PhoneRemoteProtocol.CMD_LIKE ->
                c.sendCustomCommand(MediaSessionConstants.CommandToggleLike, Bundle.EMPTY)
            PhoneRemoteProtocol.CMD_SHUFFLE ->
                c.sendCustomCommand(MediaSessionConstants.CommandToggleShuffle, Bundle.EMPTY)
            PhoneRemoteProtocol.CMD_REPEAT ->
                c.sendCustomCommand(MediaSessionConstants.CommandToggleRepeatMode, Bundle.EMPTY)
            PhoneRemoteProtocol.CMD_SEEK -> arg?.toLongOrNull()?.let { c.seekTo(it) }
            PhoneRemoteProtocol.CMD_SEEK_INDEX -> arg?.toIntOrNull()?.let { c.seekTo(it, 0L) }
            PhoneRemoteProtocol.CMD_VOLUME -> arg?.toIntOrNull()?.let { c.volume = (it / 100f).coerceIn(0f, 1f) }
            PhoneRemoteProtocol.CMD_PLAY_ID ->
                arg?.takeIf { it.isNotBlank() }?.let {
                    c.sendCustomCommand(
                        MediaSessionConstants.CommandPlayRadio,
                        Bundle().apply { putString(MediaSessionConstants.EXTRA_VIDEO_ID, it) },
                    )
                }

            PhoneRemoteProtocol.CMD_PLAY_PLAYLIST ->
                arg?.takeIf { it.isNotBlank() }?.let {
                    c.sendCustomCommand(
                        MediaSessionConstants.CommandPlayRadio,
                        Bundle().apply { putString(MediaSessionConstants.EXTRA_PLAYLIST_ID, it) },
                    )
                }
        }
    }

    private fun buildState(): RemoteState {
        val c = controller ?: return RemoteState(hasSession = false)
        val metadata = c.mediaMetadata
        val duration = c.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L
        val artwork =
            metadata.artworkUri?.toString() ?: metadata.extras?.getString("artwork_uri")
        val queue =
            (0 until c.mediaItemCount).take(200).map { index ->
                val item = c.getMediaItemAt(index)
                RemoteQueueItem(
                    id = item.mediaId,
                    title = item.mediaMetadata.title?.toString().orEmpty(),
                    artist = (item.mediaMetadata.artist ?: item.mediaMetadata.subtitle)?.toString(),
                    artwork =
                        item.mediaMetadata.artworkUri?.toString()
                            ?: item.mediaMetadata.extras?.getString("artwork_uri"),
                )
            }
        return RemoteState(
            hasSession = c.currentMediaItem != null || c.mediaItemCount > 0,
            isPlaying = c.isPlaying,
            title = metadata.title?.toString(),
            artist = (metadata.artist ?: metadata.subtitle)?.toString(),
            artwork = artwork,
            duration = duration,
            position = c.currentPosition.coerceAtLeast(0L),
            mediaId = c.currentMediaItem?.mediaId,
            currentIndex = c.currentMediaItemIndex,
            shuffle = c.shuffleModeEnabled,
            repeatMode = c.repeatMode,
            volume = c.volume,
            queue = queue,
        )
    }

    private fun pushState() {
        val bytes = PhoneRemoteProtocol.json.encodeToString(buildState()).toByteArray(Charsets.UTF_8)
        Wearable
            .getNodeClient(this)
            .connectedNodes
            .addOnSuccessListener { nodes ->
                val messageClient = Wearable.getMessageClient(this)
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, PhoneRemoteProtocol.STATE_PATH, bytes)
                }
            }
    }

    override fun onDestroy() {
        positionJob?.cancel()
        scope.cancel()
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
        super.onDestroy()
    }
}
