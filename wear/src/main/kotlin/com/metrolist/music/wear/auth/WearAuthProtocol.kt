/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.wear.auth

import android.util.Base64

/**
 * Wire format shared between the phone and the watch companion app.
 *
 * The phone's InnerTube auth is cookie based, so the only thing the watch needs is the
 * serialized cookie plus the visitor/data-sync/auth-user ids that InnerTubeX binds a
 * session to. Fields are Base64 encoded line-by-line so cookies (which may contain
 * separators/newlines) survive the trip untouched.
 */
object WearAuthProtocol {
    const val REQUEST_PATH = "/metrolist/auth/request"
    const val RESPONSE_PATH = "/metrolist/auth/response"

    const val STATUS_OK = "ok"
    const val STATUS_NOT_LOGGED_IN = "not_logged_in"
    const val STATUS_DENIED = "denied"

    data class AuthPayload(
        val status: String,
        val cookie: String? = null,
        val visitorData: String? = null,
        val dataSyncId: String? = null,
        val authUser: String? = null,
        val accountName: String? = null,
        val accountEmail: String? = null,
        val accountChannelHandle: String? = null,
    )

    fun encode(payload: AuthPayload): ByteArray {
        val fields =
            listOf(
                payload.status,
                payload.cookie.orEmpty(),
                payload.visitorData.orEmpty(),
                payload.dataSyncId.orEmpty(),
                payload.authUser.orEmpty(),
                payload.accountName.orEmpty(),
                payload.accountEmail.orEmpty(),
                payload.accountChannelHandle.orEmpty(),
            )
        return fields.joinToString("\n") {
            Base64.encodeToString(it.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }.toByteArray(Charsets.UTF_8)
    }

    fun decode(bytes: ByteArray): AuthPayload? {
        if (bytes.isEmpty()) return null
        val parts =
            String(bytes, Charsets.UTF_8).split("\n").map { part ->
                runCatching { String(Base64.decode(part, Base64.NO_WRAP), Charsets.UTF_8) }
                    .getOrDefault("")
            }
        if (parts.isEmpty()) return null
        fun value(index: Int): String? = parts.getOrNull(index)?.takeIf { it.isNotEmpty() }
        return AuthPayload(
            status = parts.getOrNull(0).orEmpty(),
            cookie = value(1),
            visitorData = value(2),
            dataSyncId = value(3),
            authUser = value(4),
            accountName = value(5),
            accountEmail = value(6),
            accountChannelHandle = value(7),
        )
    }
}
