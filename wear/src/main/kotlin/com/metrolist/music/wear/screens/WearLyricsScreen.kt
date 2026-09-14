/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.wear.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.db.entities.LyricsEntity
import com.metrolist.music.db.entities.Song
import com.metrolist.music.lyrics.LyricsEntry
import com.metrolist.music.lyrics.LyricsUtils
import com.metrolist.music.wear.components.WearEmpty
import com.metrolist.music.wear.components.WearIcons
import com.metrolist.music.wear.components.WearSectionHeader
import com.metrolist.music.wear.remote.WearRemoteController
import com.metrolist.music.wear.remote.rememberRemoteActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

private val ROMANIZE_LANGUAGES =
    listOf(
        "Japanese",
        "Korean",
        "Chinese",
        "Hindi",
        "Ukrainian",
        "Russian",
        "Serbian",
        "Bulgarian",
        "Belarusian",
        "Kyrgyz",
        "Macedonian",
    )

@Composable
fun WearLyricsScreen() {
    val playerConnection = LocalPlayerConnection.current
    val remoteActive = rememberRemoteActive()

    val fallbackLyrics = remember { MutableStateFlow<LyricsEntity?>(null) }
    val fallbackSong = remember { MutableStateFlow<Song?>(null) }
    val lyricsEntity by (playerConnection?.currentLyrics ?: fallbackLyrics).collectAsState()
    val currentSong by (playerConnection?.currentSong ?: fallbackSong).collectAsState()
    val offset = currentSong?.song?.lyricsOffset ?: 0

    val raw =
        remember(lyricsEntity) {
            lyricsEntity?.lyrics?.takeIf { it.isNotBlank() && it != LyricsEntity.LYRICS_NOT_FOUND }
        }
    val entries = remember(raw) { raw?.let { LyricsUtils.parseLyrics(it) }.orEmpty().sortedBy { it.time } }
    val isSynced = remember(entries) { entries.any { it.time > 0 } }

    val positionState = remember { mutableLongStateOf(0L) }
    LaunchedEffect(playerConnection, remoteActive, isSynced) {
        if (!isSynced || remoteActive) return@LaunchedEffect
        while (true) {
            positionState.longValue =
                runCatching { playerConnection?.player?.currentPosition }.getOrNull()
                    ?: positionState.longValue
            delay(300)
        }
    }

    val activeIndexState =
        remember(entries, isSynced) {
            derivedStateOf {
                if (!isSynced || entries.isEmpty()) {
                    -1
                } else {
                    entries.indexOfLast { it.time <= positionState.longValue }.coerceAtLeast(0)
                }
            }
        }

    // Lazily romanize the active line and a few neighbours so romanization shows up under the
    // lyric without romanizing the whole song up front (which is CPU heavy on a watch).
    val romanizations = remember(entries) { mutableStateMapOf<Int, String>() }
    val activeIndex = activeIndexState.value
    LaunchedEffect(entries, activeIndex) {
        if (entries.isEmpty()) return@LaunchedEffect
        val from = (activeIndex - 2).coerceAtLeast(0)
        val to = (activeIndex + 3).coerceAtMost(entries.size - 1)
        for (i in from..to) {
            if (!romanizations.containsKey(i)) {
                val line = entries[i].text
                if (line.isNotBlank()) {
                    val romanized =
                        runCatching {
                            LyricsUtils.romanize(line, line, ROMANIZE_LANGUAGES, false)
                        }.getOrNull()
                    if (!romanized.isNullOrBlank() && romanized != line) {
                        romanizations[i] = romanized
                    }
                }
            }
        }
    }

    val listState = rememberScalingLazyListState()
    var autoScroll by remember { mutableStateOf(true) }
    var programmaticScroll by remember { mutableStateOf(false) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (scrolling && !programmaticScroll) autoScroll = false
        }
    }

    LaunchedEffect(activeIndex, isSynced, autoScroll) {
        if (isSynced && autoScroll && activeIndex in entries.indices) {
            programmaticScroll = true
            runCatching {
                listState.animateScrollToItem(activeIndex)
                // Visually the active line sits one line too low, so pull it up by a single line:
                // use the distance to the next line as the step.
                val items = listState.layoutInfo.visibleItemsInfo
                val active = items.firstOrNull { it.index == activeIndex }
                val next = items.firstOrNull { it.index == activeIndex + 1 }
                val step = if (active != null && next != null) (next.offset - active.offset).toFloat() else 0f
                if (step > 0f) listState.scrollBy(step)
            }
            // Keep user-scroll detection suppressed until the programmatic scroll has fully settled.
            runCatching { snapshotFlow { listState.isScrollInProgress }.first { !it } }
            programmaticScroll = false
        }
    }

    fun seekTo(timeMs: Long) {
        val target = (timeMs - offset).coerceAtLeast(0L)
        if (remoteActive) {
            WearRemoteController.send("seek:$target")
        } else {
            runCatching { playerConnection?.seekTo(target) }
        }
        autoScroll = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ScreenScaffold(scrollState = listState) { contentPadding ->
            ScalingLazyColumn(
                state = listState,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxWidth(),
            ) {
                item { WearSectionHeader("Lyrics") }

                if (entries.isEmpty()) {
                    item { WearEmpty("No lyrics available") }
                } else {
                    itemsIndexed(entries, key = { index, _ -> index }) { index, entry ->
                        WearLyricLine(
                            index = index,
                            entry = entry,
                            activeIndexState = activeIndexState,
                            positionState = positionState,
                            romanization = romanizations[index],
                            onClick = { if (entry.time > 0) seekTo(entry.time) },
                        )
                    }
                }
            }
        }

        if (isSynced && !autoScroll) {
            FilledIconButton(
                onClick = { autoScroll = true },
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                        .size(40.dp),
            ) {
                Icon(WearIcons.Replay, contentDescription = "Back to synced lyrics")
            }
        }
    }
}

@Composable
private fun WearLyricLine(
    index: Int,
    entry: LyricsEntry,
    activeIndexState: State<Int>,
    positionState: State<Long>,
    romanization: String?,
    onClick: () -> Unit,
) {
    val activeIndex = activeIndexState.value
    val isActive = index == activeIndex
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val variant = MaterialTheme.colorScheme.onSurfaceVariant

    val words = entry.words
    val displayText: AnnotatedString =
        if (isActive && !words.isNullOrEmpty()) {
            val position = positionState.value
            buildAnnotatedString {
                words.forEach { word ->
                    val sung = (word.startTime * 1000).toLong() <= position
                    withStyle(SpanStyle(color = if (sung) primary else variant)) {
                        append(word.text)
                        if (word.hasTrailingSpace) append(" ")
                    }
                }
            }
        } else {
            AnnotatedString(entry.text.ifBlank { "♪" })
        }

    val lineColor =
        when {
            isActive && words.isNullOrEmpty() -> primary
            index < activeIndex -> onSurface
            else -> variant
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = displayText,
            textAlign = TextAlign.Center,
            color = lineColor,
        )
        if (!romanization.isNullOrBlank()) {
            Text(
                text = romanization,
                textAlign = TextAlign.Center,
                color = variant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
