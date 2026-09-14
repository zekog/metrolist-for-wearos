/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.wear

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.metrolist.music.R

/**
 * Receives the watch's sign-in request. Sending the session cookie is a sensitive action,
 * so instead of replying automatically we surface an Allow/Deny notification on the phone
 * and only hand the cookie over when the user approves.
 */
class PhoneAuthListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != PhoneAuthProtocol.REQUEST_PATH) return
        val nodeId = event.sourceNodeId ?: return
        showConfirmation(nodeId)
    }

    private fun showConfirmation(nodeId: String) {
        val context = applicationContext
        val notificationManager = context.getSystemService(NotificationManager::class.java)
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.wear_signin_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ),
            )
        }

        val allowIntent =
            PendingIntent.getBroadcast(
                context,
                1,
                Intent(context, PhoneAuthActionReceiver::class.java).apply {
                    action = ACTION_ALLOW
                    putExtra(PhoneAuthActionReceiver.EXTRA_NODE_ID, nodeId)
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val denyIntent =
            PendingIntent.getBroadcast(
                context,
                2,
                Intent(context, PhoneAuthActionReceiver::class.java).apply {
                    action = ACTION_DENY
                    putExtra(PhoneAuthActionReceiver.EXTRA_NODE_ID, nodeId)
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.small_icon)
                .setContentTitle(context.getString(R.string.wear_signin_title))
                .setContentText(context.getString(R.string.wear_signin_message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(0, context.getString(R.string.wear_signin_allow), allowIntent)
                .addAction(0, context.getString(R.string.wear_signin_deny), denyIntent)
                .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "wear_signin"
        const val NOTIFICATION_ID = 7051
        const val ACTION_ALLOW = "com.metrolist.music.action.WEAR_SIGNIN_ALLOW"
        const val ACTION_DENY = "com.metrolist.music.action.WEAR_SIGNIN_DENY"
    }
}
