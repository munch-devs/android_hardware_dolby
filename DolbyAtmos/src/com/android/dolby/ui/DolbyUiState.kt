/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.dolby.ui

import androidx.compose.runtime.Immutable

@Immutable
internal data class DolbyUiState(
    val dsOn: Boolean = true,
    val profile: Int = 0,
    val isOnSpeaker: Boolean = true,
    val ieq: Int = 0,
    val dialogue: Int = 0,
    val stereo: Int = 0,
    val hpVirt: Boolean = false,
    val spkVirt: Boolean = false,
    val bass: Boolean = false,
    val volume: Boolean = false,
    val presetName: String = "",
)
