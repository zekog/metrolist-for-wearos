/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.wear

import android.util.Base64

/**
 * Phone-side counterpart of the watch's [com.metrolist.music.wear.auth.WearAuthProtocol].
 * Kept as a separate copy because the phone (gms flavor) and wear modules do not share a
 * common source set for this companion-only code path.
 */
object PhoneAuthProtocol {
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
}
