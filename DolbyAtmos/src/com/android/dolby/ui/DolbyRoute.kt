/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.dolby.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.dolby.DolbyController
import com.android.dolby.R
import com.android.dolby.geq.ui.EqualizerViewModel

/** The only composable that knows about the ViewModel; everything below is stateless. */
@Composable
internal fun DolbyRoute() {
    val ctx = LocalContext.current
    val vm: DolbyViewModel = viewModel {
        val app = ctx.applicationContext
        DolbyViewModel(
            controller = DolbyController.getInstance(app),
            audioOutput = AudioOutputMonitor(app),
            stereoSupported = app.resources.getBoolean(R.bool.dolby_stereo_widening_supported),
        )
    }
    // Same view model the standalone Graphic EQ screen used; now hosted by this screen.
    val eqVm: EqualizerViewModel = viewModel(factory = EqualizerViewModel.Factory)
    val state by vm.ui.collectAsStateWithLifecycle()

    // Pick up changes made elsewhere (QS tile, Graphic EQ screen).
    LifecycleResumeEffect(vm) {
        vm.refresh()
        onPauseOrDispose {}
    }

    val actions =
        remember(vm) {
            DolbyActions(
                onEnabledChange = vm::setEnabled,
                onProfileChange = vm::setProfile,
                onIeqChange = vm::setIeq,
                onDialogueChange = vm::setDialogue,
                onStereoChange = vm::setStereo,
                onHpVirtChange = vm::setHpVirt,
                onSpkVirtChange = vm::setSpkVirt,
                onBassChange = vm::setBass,
                onVolumeChange = vm::setVolume,
                onReset = vm::reset,
            )
        }

    DolbyScreen(
        state = state,
        stereoSupported = vm.stereoSupported,
        actions = actions,
        equalizer = { enabled -> EqualizerPanel(eqVm, enabled) },
    )
}
