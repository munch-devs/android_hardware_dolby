/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.dolby.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.dolby.R
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/** All user intents of the screen. Defaults make previews/tests trivial. */
@Immutable
internal data class DolbyActions(
    val onEnabledChange: (Boolean) -> Unit = {},
    val onProfileChange: (Int) -> Unit = {},
    val onIeqChange: (Int) -> Unit = {},
    val onDialogueChange: (Int) -> Unit = {},
    val onStereoChange: (Int) -> Unit = {},
    val onHpVirtChange: (Boolean) -> Unit = {},
    val onSpkVirtChange: (Boolean) -> Unit = {},
    val onBassChange: (Boolean) -> Unit = {},
    val onVolumeChange: (Boolean) -> Unit = {},
    val onReset: () -> Unit = {},
)

private const val TAB_HOME = 0
private const val TAB_EQUALIZER = 1
private const val TAB_ADVANCED = 2

private val Tabs =
    listOf(
        TabItem(R.string.dolby_tab_home, Icons.Default.Home),
        TabItem(R.string.dolby_tab_equalizer, Icons.Default.Equalizer),
        TabItem(R.string.dolby_tab_advanced, Icons.Default.Settings),
    )

// Space reserved under scrolling content so the floating bar never hides the last item.
private val FloatingBarClearance = 96.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DolbyScreen(
    state: DolbyUiState,
    stereoSupported: Boolean,
    actions: DolbyActions,
    equalizer: @Composable (enabled: Boolean) -> Unit,
) {
    val direction = LocalLayoutDirection.current
    var showResetDialog by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(TAB_HOME) }

    val profiles = rememberChoices(R.array.dolby_profile_entries, R.array.dolby_profile_values)
    val profileName = profiles.labels.getOrNull(profiles.values.indexOf(state.profile)) ?: state.presetName
    val heroSummary =
        when {
            !state.dsOn -> stringResource(R.string.dolby_off)
            profileName.isNotEmpty() -> stringResource(R.string.dolby_on_with_profile, profileName)
            else -> stringResource(R.string.dolby_on)
        }
    val canReset = state.dsOn && state.profile != -1

    // Dolby off -> pages are dimmed (and disabled), not hidden; the switch and tab bar stay usable.
    val contentAlpha by animateFloatAsState(if (state.dsOn) 1f else 0.5f, label = "contentAlpha")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.dolby_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            stringResource(R.string.dolby_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                expandedHeight = 92.dp,
                actions = {
                    // The Equalizer page has its own reset (resets gains, not the profile).
                    if (tab != TAB_EQUALIZER) {
                        IconButton(
                            onClick = { showResetDialog = true },
                            enabled = canReset,
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Icon(
                                Icons.Default.RestartAlt,
                                contentDescription = stringResource(R.string.dolby_reset_profile),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            FloatingTabBar(tabs = Tabs, selectedIndex = tab, onSelect = { tab = it })
        },
        floatingActionButtonPosition = FabPosition.Center,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { pad ->
        // Not swipeable between tabs on purpose: the profile carousel owns horizontal drags.
        Crossfade(
            targetState = tab,
            label = "tab",
            modifier =
                Modifier.fillMaxSize()
                    .padding(
                        top = pad.calculateTopPadding(),
                        start = pad.calculateStartPadding(direction),
                        end = pad.calculateEndPadding(direction),
                    ),
        ) { current ->
            Page(bottomPadding = pad.calculateBottomPadding() + FloatingBarClearance) {
                when (current) {
                    TAB_HOME ->
                        HomePage(state, canReset, heroSummary, contentAlpha, actions)
                    TAB_EQUALIZER ->
                        Column(
                            Modifier.padding(horizontal = 16.dp).graphicsLayer { alpha = contentAlpha }
                        ) {
                            equalizer(canReset)
                        }
                    TAB_ADVANCED ->
                        Column(Modifier.graphicsLayer { alpha = contentAlpha }) {
                            AdvancedPage(state, canReset, stereoSupported, actions)
                        }
                }
            }
        }
    }

    if (showResetDialog) {
        ConfirmDialog(
            title = stringResource(R.string.dolby_reset_profile),
            icon = Icons.Default.RestartAlt,
            onConfirm = {
                actions.onReset()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false },
        )
    }
}

@Composable
private fun Page(bottomPadding: Dp, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        content()
    }
}

/** Hero card with the main switch, the profile carousel and the intelligent EQ. */
@Composable
private fun HomePage(
    state: DolbyUiState,
    enabled: Boolean,
    heroSummary: String,
    contentAlpha: Float,
    actions: DolbyActions,
) {
    val ieq = rememberChoices(R.array.dolby_ieq_entries, R.array.dolby_ieq_values)

    // The hero stays fully opaque: it holds the switch that brings everything else back.
    Column(Modifier.padding(horizontal = 16.dp)) {
        HeroCard(on = state.dsOn, summary = heroSummary, onChange = actions.onEnabledChange)
    }

    Column(
        Modifier.graphicsLayer { alpha = contentAlpha },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            SettingsCard(title = stringResource(R.string.dolby_profile_title), icon = Icons.Default.Style) {
                ProfileCarousel(
                    profile = state.profile,
                    enabled = state.dsOn,
                    onProfileChange = actions.onProfileChange,
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                )
            }
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            SettingsCard(title = stringResource(R.string.dolby_ieq), icon = Icons.Default.GraphicEq) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(ieq.size) { i ->
                        OptionTile(
                            label = ieq.labels[i],
                            icon = ieqIcon(ieq.values[i]),
                            selected = ieq.values[i] == state.ieq,
                            enabled = enabled,
                            onClick = { actions.onIeqChange(ieq.values[i]) },
                            modifier = Modifier.weight(1f),
                            iconSize = 32.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(on: Boolean, summary: String, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val waveAlpha by animateFloatAsState(if (on) 1f else 0.3f, label = "wave")

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            Modifier.fillMaxWidth()
                .height(168.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(colors.primaryContainer.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
        ) {
            Waveform(
                color = colors.primary,
                modifier =
                    Modifier.fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 20.dp)
                        .graphicsLayer { alpha = waveAlpha },
            )
        }
        ListItem(
            headlineContent = {
                Text(stringResource(R.string.dolby_enable), style = MaterialTheme.typography.headlineSmall)
            },
            supportingContent = { Text(summary) },
            // The whole row toggles; the Switch itself is display-only (better for TalkBack).
            trailingContent = { Switch(checked = on, onCheckedChange = null) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.toggleable(value = on, role = Role.Switch, onValueChange = onChange),
        )
    }
}

/** Purely decorative: a symmetric bar waveform that peaks in the middle. */
@Composable
private fun Waveform(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val bars = 43
        val step = size.width / bars
        val stroke = (step * 0.45f).coerceAtLeast(2.dp.toPx())
        for (i in 0 until bars) {
            val t = i / (bars - 1f)
            val envelope = exp(-((t - 0.5f) * (t - 0.5f)) / 0.06f)
            val ripple = 0.65f + 0.35f * abs(sin(i * 1.7f))
            val h = size.height * (0.08f + 0.92f * envelope * ripple)
            val x = step * (i + 0.5f)
            drawLine(
                color = color,
                start = Offset(x, (size.height - h) / 2f),
                end = Offset(x, (size.height + h) / 2f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
                alpha = 0.45f + 0.55f * envelope,
            )
        }
    }
}

/** Everything else, grouped like the reference design: tuning, volume leveler, surround. */
@Composable
private fun AdvancedPage(
    state: DolbyUiState,
    enabled: Boolean,
    stereoSupported: Boolean,
    actions: DolbyActions,
) {
    val dialogue = rememberChoices(R.array.dolby_dialogue_entries, R.array.dolby_dialogue_values)
    val stereo = rememberChoices(R.array.dolby_stereo_entries, R.array.dolby_stereo_values)
    val headphonesOnly = enabled && !state.isOnSpeaker
    val headphonesHint = if (state.isOnSpeaker) stringResource(R.string.dolby_connect_headphones) else null

    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingsCard(title = stringResource(R.string.dolby_audio_tuning), icon = Icons.Default.Tune) {
            SwitchRow(
                R.string.dolby_bass_enhancer,
                state.bass,
                enabled,
                stringResource(R.string.dolby_bass_enhancer_desc),
                actions.onBassChange,
                icon = Icons.Default.MusicNote,
            )
            StepSliderRow(
                title = stringResource(R.string.dolby_dialogue_enhancer),
                choices = dialogue,
                selected = state.dialogue,
                enabled = enabled,
                onSelect = actions.onDialogueChange,
                supporting = stringResource(R.string.dolby_dialogue_enhancer_desc),
                icon = Icons.Default.RecordVoiceOver,
            )
        }
        SettingsCard(title = stringResource(R.string.dolby_volume_leveler), icon = Icons.Default.VolumeUp) {
            SwitchRow(
                R.string.dolby_volume_leveler,
                state.volume,
                enabled,
                stringResource(R.string.dolby_volume_leveler_desc),
                actions.onVolumeChange,
                icon = Icons.Default.BarChart,
            )
        }
        SettingsCard(
            title = stringResource(R.string.dolby_surround_virtualizer),
            icon = Icons.Default.Headphones,
        ) {
            SwitchRow(
                R.string.dolby_spk_virtualizer,
                state.spkVirt,
                enabled,
                stringResource(R.string.dolby_spk_virtualizer_desc),
                actions.onSpkVirtChange,
                icon = Icons.Default.Speaker,
            )
            SwitchRow(
                R.string.dolby_hp_virtualizer,
                state.hpVirt,
                headphonesOnly,
                headphonesHint ?: stringResource(R.string.dolby_hp_virtualizer_desc),
                actions.onHpVirtChange,
                icon = Icons.Default.Headphones,
            )
            if (stereoSupported) {
                StepSliderRow(
                    title = stringResource(R.string.dolby_stereo_widening),
                    choices = stereo,
                    selected = state.stereo,
                    enabled = headphonesOnly,
                    supporting = headphonesHint,
                    onSelect = actions.onStereoChange,
                    icon = Icons.Default.SurroundSound,
                )
            }
        }
    }
}

/** IEQ id (dolby_ieq_values, mirrors the ieq presets of dax-default.xml) -> existing drawable. */
private fun ieqIcon(value: Int): Int =
    when (value) {
        1 -> R.drawable.ic_ieq_detailed
        2 -> R.drawable.ic_ieq_balanced
        3 -> R.drawable.ic_ieq_warm
        else -> R.drawable.ic_ieq_off
    }
