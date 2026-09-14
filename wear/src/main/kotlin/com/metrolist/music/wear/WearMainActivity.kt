/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.wear

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.LocalSyncUtils
import com.metrolist.music.constants.ShowLyricsKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.playback.DownloadUtil
import com.metrolist.music.playback.MusicService
import com.metrolist.music.playback.MusicService.MusicBinder
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.utils.SyncUtils
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.safeDataStoreEdit
import com.metrolist.music.wear.navigation.WearApp
import com.metrolist.music.wear.remote.WearRemoteController
import com.metrolist.music.wear.theme.WearTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class WearMainActivity : ComponentActivity() {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var syncUtils: SyncUtils

    private var playerConnection: PlayerConnection? = null
    private var playerConnectionSnapshot by mutableStateOf<PlayerConnection?>(null)
    private var isServiceBound = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Timber.tag("WearMainActivity").d("POST_NOTIFICATIONS granted=$granted")
        }

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                if (service is MusicBinder) {
                    playerConnection = PlayerConnection(this@WearMainActivity, service, database, lifecycleScope)
                    playerConnectionSnapshot = playerConnection
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                playerConnection?.dispose()
                // Keep the disconnected PlayerConnection around so the UI does not
                // flash an empty player while the foreground service reconnects.
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WearRemoteController.init(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // The shared MusicService only fetches lyrics when this preference is enabled
        // (default false on the phone). Wear users expect lyrics to work out of the box.
        lifecycleScope.launch(Dispatchers.IO) {
            val settings = dataStore.data.first()
            if (settings[ShowLyricsKey] == null) {
                safeDataStoreEdit { it[ShowLyricsKey] = true }
            }
        }

        setContent {
            WearTheme {
                CompositionLocalProvider(
                    LocalDatabase provides database,
                    LocalPlayerConnection provides playerConnectionSnapshot,
                    LocalDownloadUtil provides downloadUtil,
                    LocalSyncUtils provides syncUtils,
                    LocalPlayerAwareWindowInsets provides WindowInsets(0, 0, 0, 0),
                ) {
                    WearApp()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!MusicService.isRunning) {
            val serviceIntent = Intent(this, MusicService::class.java)
            try {
                ContextCompat.startForegroundService(this, serviceIntent)
            } catch (e: IllegalStateException) {
                Timber.w(e, "Failed to start playback service")
            } catch (e: Exception) {
                Timber.w(e, "Failed to start playback service")
            }
        }
        if (!isServiceBound) {
            bindService(Intent(this, MusicService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
            isServiceBound = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerConnection?.dispose()
        playerConnection = null
        playerConnectionSnapshot = null
        if (isServiceBound) {
            try {
                unbindService(serviceConnection)
            } catch (e: IllegalArgumentException) {
                Timber.tag("WearMainActivity").w(e, "Service was not bound on unbind")
            } finally {
                isServiceBound = false
            }
        }
    }
}
