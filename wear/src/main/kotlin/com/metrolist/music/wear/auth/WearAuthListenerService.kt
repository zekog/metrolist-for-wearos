/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.wear.auth

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.metrolist.music.constants.AccountChannelHandleKey
import com.metrolist.music.constants.AccountEmailKey
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.DataSyncIdKey
import com.metrolist.music.constants.InnerTubeAuthUserKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.VisitorDataKey
import com.metrolist.music.utils.safeDataStoreEdit
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/**
 * Receives the auth payload sent by the paired phone when the user approves the sign-in
 * handoff. Writing to the shared preference store is enough: [com.metrolist.music.App]
 * already observes these keys and pushes them into InnerTubeX, so the watch session
 * becomes authenticated as soon as the values land.
 */
class WearAuthListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != WearAuthProtocol.RESPONSE_PATH) return

        val payload = WearAuthProtocol.decode(event.data) ?: return
        val cookie = payload.cookie
        if (payload.status != WearAuthProtocol.STATUS_OK || cookie.isNullOrBlank()) {
            Timber.tag("WearAuth").i("Sign-in handoff completed with status=${payload.status}")
            return
        }

        val context = applicationContext
        runBlocking {
            context.safeDataStoreEdit { settings ->
                settings[InnerTubeCookieKey] = cookie
                payload.visitorData?.let { settings[VisitorDataKey] = it }
                payload.dataSyncId?.let { settings[DataSyncIdKey] = it }
                payload.authUser?.let { settings[InnerTubeAuthUserKey] = it }
                payload.accountName?.let { settings[AccountNameKey] = it }
                payload.accountEmail?.let { settings[AccountEmailKey] = it }
                payload.accountChannelHandle?.let { settings[AccountChannelHandleKey] = it }
            }
        }
        Timber.tag("WearAuth").i("Received account session from phone")
    }
}
