/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.wear

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import com.metrolist.music.constants.AccountChannelHandleKey
import com.metrolist.music.constants.AccountEmailKey
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.DataSyncIdKey
import com.metrolist.music.constants.InnerTubeAuthUserKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.VisitorDataKey
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

class PhoneAuthActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val nodeId = intent.getStringExtra(EXTRA_NODE_ID) ?: return
        val allow = intent.action == PhoneAuthListenerService.ACTION_ALLOW
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val payload =
                    if (allow) {
                        buildPayload(context.applicationContext)
                    } else {
                        PhoneAuthProtocol.AuthPayload(PhoneAuthProtocol.STATUS_DENIED)
                    }
                val bytes = PhoneAuthProtocol.encode(payload)
                runCatching {
                    Tasks.await(
                        Wearable
                            .getMessageClient(context.applicationContext)
                            .sendMessage(nodeId, PhoneAuthProtocol.RESPONSE_PATH, bytes),
                        10,
                        TimeUnit.SECONDS,
                    )
                }.onFailure { Timber.tag("PhoneAuth").w(it, "Failed to send auth response to watch") }
            } finally {
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
                    ?.cancel(PhoneAuthListenerService.NOTIFICATION_ID)
                pendingResult.finish()
            }
        }
    }

    private fun buildPayload(context: Context): PhoneAuthProtocol.AuthPayload {
        val cookie = context.dataStore[InnerTubeCookieKey]
        if (cookie.isNullOrBlank()) {
            return PhoneAuthProtocol.AuthPayload(PhoneAuthProtocol.STATUS_NOT_LOGGED_IN)
        }
        return PhoneAuthProtocol.AuthPayload(
            status = PhoneAuthProtocol.STATUS_OK,
            cookie = cookie,
            visitorData = context.dataStore[VisitorDataKey],
            dataSyncId = context.dataStore[DataSyncIdKey],
            authUser = context.dataStore[InnerTubeAuthUserKey],
            accountName = context.dataStore[AccountNameKey],
            accountEmail = context.dataStore[AccountEmailKey],
            accountChannelHandle = context.dataStore[AccountChannelHandleKey],
        )
    }

    companion object {
        const val EXTRA_NODE_ID = "node_id"
    }
}
