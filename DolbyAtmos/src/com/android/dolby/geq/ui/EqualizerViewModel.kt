/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.dolby.geq.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.android.dolby.DolbyConstants.Companion.dlog
import com.android.dolby.geq.data.EQ_BAND_COUNTS
import com.android.dolby.geq.data.EqualizerRepository
import com.android.dolby.geq.data.Preset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

const val TAG = "EqViewModel"

/** Fills the bands between consecutive [points] with a straight line between their gains. */
private fun interpolate(gains: MutableList<Int>, points: List<Int>) {
    for (k in 0 until points.size - 1) {
        val a = points[k]
        val b = points[k + 1]
        for (i in a + 1 until b) {
            gains[i] = (gains[a] + (gains[b] - gains[a]) * (i - a) / (b - a).toFloat()).roundToInt()
        }
    }
}

class EqualizerViewModel(private val repository: EqualizerRepository) : ViewModel() {

    private val _presets = MutableStateFlow(repository.builtInPresets)
    val presets = _presets.asStateFlow()

    private val _preset = MutableStateFlow(repository.defaultPreset)
    val preset = _preset.asStateFlow()

    private val _bandCount = MutableStateFlow(repository.bandCount)
    val bandCount = _bandCount.asStateFlow()

    private val _curveView = MutableStateFlow(repository.curveView)
    val curveView = _curveView.asStateFlow()

    private var presetRestored = false

    init {
        // Update the list of presets: combined list of user defined presets if any,
        // and then the built in presets.
        repository.userPresets
            .onEach { presets ->
                dlog(TAG, "updated userPresets: $presets")
                _presets.value =
                    mutableListOf<Preset>()
                        .apply {
                            addAll(presets)
                            addAll(repository.builtInPresets)
                        }
                        .toList()

                // We can restore the active preset only after the presets list is populated,
                // since we do not save the preset name but only its gains.
                if (!presetRestored) {
                    val bandGains = repository.getBandGains()
                    _preset.value =
                        _presets.value.find { bandGains == it.bandGains }
                            ?: Preset(name = "Custom", bandGains = bandGains)
                    dlog(TAG, "restored preset: ${_preset.value}")
                    presetRestored = true
                }
            }
            .launchIn(viewModelScope)

        // Update the preset in repository everytime we set it here
        _preset
            .drop(1) // skip the initial value
            .onEach {
                // wait till the active preset is restored
                if (!presetRestored) {
                    return@onEach
                }
                dlog(TAG, "updated preset: $it")
                repository.setBandGains(it.bandGains)
                if (it.isUserDefined) {
                    repository.addPreset(it)
                }
            }
            .launchIn(viewModelScope)
    }

    fun reset() {
        dlog(TAG, "reset()")
        if (_preset.value.isUserDefined) {
            // Reset gains to 0
            _preset.value = _preset.value.copy(bandGains = repository.defaultPreset.bandGains)
        } else {
            // Switch to flat preset
            _preset.value = repository.defaultPreset
        }
    }

    fun setPreset(preset: Preset) {
        dlog(TAG, "setPreset($preset)")
        _preset.value = preset
    }

    fun setBandCount(count: Int) {
        if (count !in EQ_BAND_COUNTS) return
        _bandCount.value = count
        repository.bandCount = count
    }

    fun setCurveView(curve: Boolean) {
        _curveView.value = curve
        repository.curveView = curve
    }

    /**
     * Sets the gain of band [index] (a position in the 20-band backend array). When fewer bands
     * are shown, [controlPoints] lists the positions of the visible ones and the bands between
     * them follow by linear interpolation, so the curve stays smooth on the backend.
     */
    fun setGain(index: Int, gain: Int, controlPoints: List<Int> = emptyList()) {
        dlog(TAG, "setGain($index, $gain)")
        _preset.value =
            _preset.value.run {
                val gains = bandGains.map { it.gain }.toMutableList()
                gains[index] = gain
                if (controlPoints.size in 2 until gains.size) interpolate(gains, controlPoints)
                copy(
                    name = if (!isUserDefined) "Custom" else name,
                    // create new objects to ensure the flow emits an update.
                    bandGains = bandGains.mapIndexed { i, band -> band.copy(gain = gains[i]) },
                    isMutated = true,
                )
            }
    }

    // Returns string containing the error message if it failed, otherwise null
    private fun validatePresetName(name: String): PresetNameValidationError? {
        // Ensure we don't have another preset with the same name
        return if (_presets.value.any { it.name.equals(name.trim(), ignoreCase = true) }) {
            PresetNameValidationError.NAME_EXISTS
        } else if (name.length > 50) {
            PresetNameValidationError.NAME_TOO_LONG
        } else null
    }

    fun createNewPreset(name: String): PresetNameValidationError? {
        dlog(TAG, "createNewPreset($name)")
        validatePresetName(name)?.let {
            dlog(TAG, "createNewPreset failed: $it")
            return it
        }
        _preset.value =
            _preset.value.copy(name = name.trim(), isUserDefined = true, isMutated = false)
        return null
    }

    fun renamePreset(preset: Preset, name: String): PresetNameValidationError? {
        dlog(TAG, "renamePreset($preset, $name)")
        // create a preset with the new name and same gains
        createNewPreset(name = name)?.let {
            dlog(TAG, "renamePreset failed")
            return it
        }
        // and delete the old one.
        deletePreset(preset, shouldReset = false)
        return null
    }

    fun deletePreset(preset: Preset, shouldReset: Boolean = true) {
        dlog(TAG, "deletePreset($preset)")
        viewModelScope.launch { repository.removePreset(preset) }
        if (shouldReset) {
            _preset.value = repository.defaultPreset
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                EqualizerViewModel(
                    repository =
                        EqualizerRepository(
                            this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                        )
                )
            }
        }
    }
}
