/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.wear.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.SelectedThemeColorKey
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.theme.DefaultThemeColor
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import androidx.compose.ui.graphics.toArgb

@Composable
fun WearTheme(content: @Composable () -> Unit) {
    val darkMode by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val systemDark = isSystemInDarkTheme()
    val useDark =
        remember(darkMode, systemDark) {
            if (darkMode == DarkMode.AUTO) systemDark else darkMode == DarkMode.ON
        }
    val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
    val pureBlack = pureBlackEnabled && useDark
    val (selectedColorInt) = rememberPreference(SelectedThemeColorKey, defaultValue = DefaultThemeColor.toArgb())
    val seed = Color(selectedColorInt)

    val colorScheme =
        remember(useDark, pureBlack, seed) {
            val base = ColorScheme()
            val onSeed = if (seed.luminance() > 0.5f) Color.Black else Color.White
            val accent =
                base.copy(
                    primary = seed,
                    onPrimary = onSeed,
                    primaryContainer = seed,
                    onPrimaryContainer = onSeed,
                    secondary = seed,
                    onSecondary = onSeed,
                    secondaryContainer = seed,
                    onSecondaryContainer = onSeed,
                    tertiary = seed,
                    onTertiary = onSeed,
                )
            if (pureBlack) {
                accent.copy(
                    background = Color.Black,
                    onBackground = Color.White,
                    surfaceContainerLow = Color.Black,
                    surfaceContainer = Color.Black,
                    surfaceContainerHigh = Color(0xFF101010),
                )
            } else {
                accent
            }
        }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
