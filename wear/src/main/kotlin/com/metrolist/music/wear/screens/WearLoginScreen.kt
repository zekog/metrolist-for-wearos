/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.wear.screens

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import com.metrolist.music.LocalSyncUtils
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.utils.dataStore
import com.metrolist.music.wear.auth.WearAuthProtocol
import com.metrolist.music.wear.components.WearSectionHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

private suspend fun sendAuthRequest(context: Context): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes, 10, TimeUnit.SECONDS)
            if (nodes.isEmpty()) return@withContext false
            val messageClient = Wearable.getMessageClient(context)
            var sent = false
            nodes.forEach { node ->
                runCatching {
                    Tasks.await(
                        messageClient.sendMessage(
                            node.id,
                            WearAuthProtocol.REQUEST_PATH,
                            byteArrayOf(1),
                        ),
                        10,
                        TimeUnit.SECONDS,
                    )
                    sent = true
                }
            }
            sent
        } catch (e: Exception) {
            false
        }
    }

@Composable
fun WearLoginScreen(navController: NavHostController) {
    val context = LocalContext.current
    val syncUtils = LocalSyncUtils.current
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf("Requesting sign-in from your phone…") }
    var canRetry by remember { mutableStateOf(false) }

    val prefs by remember { context.dataStore.data }.collectAsState(initial = null)
    val cookie = prefs?.get(InnerTubeCookieKey)

    suspend fun request() {
        status = "Requesting sign-in from your phone…"
        canRetry = false
        val sent = sendAuthRequest(context)
        if (sent) {
            status = "Open Metrolist on your phone and tap Allow."
        } else {
            status = "No paired phone found. Install and open Metrolist on your phone."
            canRetry = true
        }
    }

    LaunchedEffect(Unit) { request() }

    LaunchedEffect(cookie) {
        if (!cookie.isNullOrBlank()) {
            status = "Signed in!"
            runCatching { syncUtils.tryAutoSync() }
            delay(1500)
            navController.popBackStack()
        }
    }

    val listState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = listState, edgeButton = {}) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding, modifier = Modifier.fillMaxWidth()) {
            item { WearSectionHeader("Sign in") }
            item {
                Text(
                    text = status,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (canRetry) {
                item {
                    Card(
                        onClick = { scope.launch { request() } },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = "Try again")
                    }
                }
            }
            item {
                Card(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Cancel")
                }
            }
        }
    }
}
