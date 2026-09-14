/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.wear.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import com.metrolist.music.BuildConfig
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.AudioNormalizationKey
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.wear.components.WearIcons
import com.metrolist.music.wear.components.WearSectionHeader
import com.metrolist.music.wear.navigation.WearRoutes
import com.metrolist.music.wear.remote.REMOTE_MODE_AUTO
import com.metrolist.music.wear.remote.REMOTE_MODE_PHONE
import com.metrolist.music.wear.remote.REMOTE_MODE_WATCH
import com.metrolist.music.wear.remote.WearRemoteModeKey

@Composable
fun WearSettingsScreen(navController: NavHostController) {
    val context = LocalContext.current

    val (darkMode, setDarkMode) = rememberEnumPreference(DarkModeKey, DarkMode.AUTO)
    val (pureBlack, setPureBlack) = rememberPreference(PureBlackKey, false)
    val (hideExplicit, setHideExplicit) = rememberPreference(HideExplicitKey, false)
    val (normalization, setNormalization) = rememberPreference(AudioNormalizationKey, false)
    val (remoteMode, setRemoteMode) = rememberPreference(WearRemoteModeKey, REMOTE_MODE_AUTO)

    val prefs by remember { context.dataStore.data }.collectAsState(initial = null)
    val accountName = prefs?.get(AccountNameKey) ?: "Guest"

    val listState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = listState, edgeButton = {}) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding, modifier = Modifier.fillMaxWidth()) {
            item { WearSectionHeader("Settings") }

            item {
                Text(
                    text = "Signed in as $accountName",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (accountName == "Guest") {
                item {
                    Card(
                        onClick = { navController.navigate(WearRoutes.LOGIN) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = "Sign in from phone")
                    }
                }
            }

            item { WearSectionHeader("Display") }

            item {
                SwitchButton(
                    checked = darkMode != DarkMode.OFF,
                    onCheckedChange = { checked -> setDarkMode(if (checked) DarkMode.ON else DarkMode.OFF) },
                    label = { Text("Dark theme") },
                )
            }
            item {
                SwitchButton(
                    checked = pureBlack,
                    onCheckedChange = { setPureBlack(it) },
                    label = { Text("Pure black") },
                )
            }
            item {
                SwitchButton(
                    checked = hideExplicit,
                    onCheckedChange = { setHideExplicit(it) },
                    label = { Text("Hide explicit songs") },
                )
            }

            item { WearSectionHeader("Playback") }

            item {
                SelectableRow(
                    label = "Automatic (phone when playing)",
                    selected = remoteMode == REMOTE_MODE_AUTO,
                    onClick = { setRemoteMode(REMOTE_MODE_AUTO) },
                )
            }
            item {
                SelectableRow(
                    label = "Control phone",
                    selected = remoteMode == REMOTE_MODE_PHONE,
                    onClick = { setRemoteMode(REMOTE_MODE_PHONE) },
                )
            }
            item {
                SelectableRow(
                    label = "Play on watch",
                    selected = remoteMode == REMOTE_MODE_WATCH,
                    onClick = { setRemoteMode(REMOTE_MODE_WATCH) },
                )
            }

            item { WearSectionHeader("Audio") }

            item {
                SwitchButton(
                    checked = normalization,
                    onCheckedChange = { setNormalization(it) },
                    label = { Text("Audio normalization") },
                )
            }
            item {
                Card(
                    onClick = {
                        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(context, "System equalizer is not available", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Equalizer")
                }
            }

            item { WearSectionHeader("More") }

            item {
                Card(
                    onClick = { navController.navigate(WearRoutes.STATS) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Your stats")
                }
            }
            item {
                Text(text = "Metrolist Wear ${BuildConfig.BASE_VERSION_NAME}", modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SelectableRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    imageVector = WearIcons.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
