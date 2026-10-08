/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.dolby.geq.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.android.dolby.geq.data.EQ_BAND_FREQUENCIES
import com.android.dolby.geq.data.bandControlPoints

@Composable
fun EqualizerBands(
    viewModel: EqualizerViewModel,
    controlPoints: List<Int> = bandControlPoints(EQ_BAND_FREQUENCIES.size),
) {
    val preset by viewModel.preset.collectAsState()
    val bandGains = preset.bandGains

    LazyRow(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        item { BandGainSliderLabels() }
        items(controlPoints.size) { i ->
            val index = controlPoints[i]
            BandGainSlider(
                bandGains[index],
                onValueChangeFinished = { viewModel.setGain(index, it, controlPoints) },
            )
        }
    }
}
