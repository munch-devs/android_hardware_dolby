/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.dolby.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.dolby.R
import com.android.dolby.geq.data.EQ_BAND_COUNTS
import com.android.dolby.geq.data.EQ_BAND_FREQUENCIES
import com.android.dolby.geq.data.bandControlPoints
import com.android.dolby.geq.data.formatHzLong
import com.android.dolby.geq.ui.EqualizerBands
import com.android.dolby.geq.ui.EqualizerViewModel
import com.android.dolby.geq.ui.PresetSelector

/**
 * The graphic equalizer, embedded in the main screen as four cards: preset, band configuration
 * (10/15/20), view (curve/sliders) and the frequency response itself. The backend always has 20
 * bands; fewer visible bands are control points whose in-between bands are interpolated.
 */
@Composable
internal fun EqualizerPanel(
    viewModel: EqualizerViewModel,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val preset by viewModel.preset.collectAsState()
    val bandCount by viewModel.bandCount.collectAsState()
    val curveView by viewModel.curveView.collectAsState()

    val points = remember(bandCount) { bandControlPoints(bandCount) }
    val shown = remember(preset, points) { points.map { preset.bandGains[it] } }

    Box(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsCard(
                title = stringResource(R.string.dolby_geq_preset),
                icon = Icons.Default.LibraryMusic,
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    PresetSelector(viewModel)
                }
            }

            SettingsCard(
                title = stringResource(R.string.dolby_geq_band_config),
                icon = Icons.Default.Tune,
            ) {
                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(R.string.dolby_geq_band_config_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        EQ_BAND_COUNTS.forEachIndexed { i, count ->
                            SegmentedButton(
                                selected = bandCount == count,
                                onClick = { viewModel.setBandCount(count) },
                                shape = SegmentedButtonDefaults.itemShape(i, EQ_BAND_COUNTS.size),
                            ) {
                                Text(stringResource(R.string.dolby_geq_bands_count, count), maxLines = 1)
                            }
                        }
                    }
                }
            }

            SettingsCard(
                title = stringResource(R.string.dolby_geq_view),
                icon = Icons.Default.Visibility,
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = curveView,
                            onClick = { viewModel.setCurveView(true) },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                            icon = { Icon(Icons.Default.ShowChart, null, Modifier.height(18.dp)) },
                        ) {
                            Text(stringResource(R.string.dolby_geq_view_curve), maxLines = 1)
                        }
                        SegmentedButton(
                            selected = !curveView,
                            onClick = { viewModel.setCurveView(false) },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                            icon = { Icon(Icons.Default.BarChart, null, Modifier.height(18.dp)) },
                        ) {
                            Text(stringResource(R.string.dolby_geq_view_bars), maxLines = 1)
                        }
                    }
                }
            }

            SettingsCard(
                title = stringResource(R.string.dolby_geq_response_title),
                icon = Icons.Default.GraphicEq,
            ) {
                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(
                            if (curveView) R.string.dolby_geq_response_desc_curve
                            else R.string.dolby_geq_response_desc_sliders,
                            10,
                            formatHzLong(EQ_BAND_FREQUENCIES.first()),
                            formatHzLong(EQ_BAND_FREQUENCIES.last()),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (curveView) {
                        EqualizerCurve(
                            gains = shown,
                            onGainChangeFinished = { i, gain ->
                                viewModel.setGain(points[i], gain, points)
                            },
                        )
                    } else {
                        EqualizerBands(viewModel, points)
                    }
                }
            }
        }
        // Dolby off: the page is dimmed by the screen; this makes it non-interactive too.
        if (!enabled) {
            Box(
                Modifier.matchParentSize().pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            )
        }
    }
}
