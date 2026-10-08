/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.dolby.geq.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.android.dolby.DolbyConstants.Companion.PREF_PRESET
import com.android.dolby.DolbyConstants.Companion.dlog
import com.android.dolby.DolbyController
import com.android.dolby.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

class EqualizerRepository(private val context: Context) {

    private val dolbyController by lazy { DolbyController.getInstance(context) }

    // Preset is saved as a string of comma separated gains in SharedPreferences
    // and is unique to each profile ID
    private val profile = dolbyController.profile
    private val profileSharedPrefs by lazy {
        context.getSharedPreferences("profile_$profile", Context.MODE_PRIVATE)
    }

    private val presetsSharedPrefs by lazy {
        context.getSharedPreferences("presets", Context.MODE_PRIVATE)
    }

    val builtInPresets: List<Preset> by lazy {
        val names = context.resources.getStringArray(R.array.dolby_preset_entries)
        val presets = context.resources.getStringArray(R.array.dolby_preset_values)
        List(names.size) { index ->
            Preset(name = names[index], bandGains = deserializeGains(presets[index]))
        }
    }

    val defaultPreset by lazy { builtInPresets[0] } // Flat

    // UI-only choices, kept apart from "presets" (every entry of that file is a user preset).
    private val uiPrefs by lazy {
        context.getSharedPreferences("equalizer_ui", Context.MODE_PRIVATE)
    }

    var bandCount: Int
        get() = uiPrefs.getInt(KEY_BAND_COUNT, 10).takeIf { it in EQ_BAND_COUNTS } ?: 10
        set(value) = uiPrefs.edit().putInt(KEY_BAND_COUNT, value).apply()

    var curveView: Boolean
        get() = uiPrefs.getBoolean(KEY_CURVE_VIEW, true)
        set(value) = uiPrefs.edit().putBoolean(KEY_CURVE_VIEW, value).apply()

    // User defined presets are stored in a SharedPreferences as
    // key - preset name
    // value - comma separated string of gains
    val userPresets: Flow<List<Preset>> = callbackFlow {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                dlog(TAG, "presetsSharedPrefs changed")
                trySend(
                    presetsSharedPrefs.all.map { (key, value) ->
                        Preset(
                            name = key,
                            bandGains = deserializeGains(value.toString()),
                            isUserDefined = true,
                        )
                    }
                )
            }

        presetsSharedPrefs.registerOnSharedPreferenceChangeListener(listener)
        dlog(TAG, "presetsSharedPrefs registered listener")
        // trigger an initial emission
        listener.onSharedPreferenceChanged(presetsSharedPrefs, null)

        awaitClose {
            presetsSharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
            dlog(TAG, "presetsSharedPrefs unregistered listener")
        }
    }

    suspend fun getBandGains(): List<BandGain> =
        withContext(Dispatchers.IO) {
            val gains = profileSharedPrefs.getString(PREF_PRESET, dolbyController.getPreset())
            return@withContext if (gains.isNullOrEmpty()) {
                    defaultPreset.bandGains
                } else {
                    deserializeGains(gains)
                }
                .also { dlog(TAG, "getBandGains: $it") }
        }

    suspend fun setBandGains(bandGains: List<BandGain>) =
        withContext(Dispatchers.IO) {
            dlog(TAG, "setBandGains($bandGains)")
            val gains = serializeGains(bandGains)
            dolbyController.setPreset(gains)
            profileSharedPrefs.edit().putString(PREF_PRESET, gains).apply()
        }

    suspend fun addPreset(preset: Preset) =
        withContext(Dispatchers.IO) {
            dlog(TAG, "addPreset($preset)")
            presetsSharedPrefs
                .edit()
                .putString(preset.name, serializeGains(preset.bandGains))
                .apply()
        }

    suspend fun removePreset(preset: Preset) =
        withContext(Dispatchers.IO) {
            dlog(TAG, "removePreset($preset)")
            presetsSharedPrefs.edit().remove(preset.name).apply()
        }

    private companion object {
        const val TAG = "EqRepository"
        const val KEY_BAND_COUNT = "band_count"
        const val KEY_CURVE_VIEW = "curve_view"

        // The backend works with 20 bands and so do we; the UI only decides how many of them
        // are shown as control points (see bandControlPoints).
        fun deserializeGains(bandGains: String): List<BandGain> {
            val gains: List<Int> =
                bandGains
                    .split(",")
                    .runCatching {
                        require(size == EQ_BAND_FREQUENCIES.size) {
                            "Preset must have ${EQ_BAND_FREQUENCIES.size} elements, has only $size!"
                        }
                        map { it.trim().toInt() }
                    }
                    .onFailure { exception -> Log.e(TAG, "Failed to parse preset", exception) }
                    .getOrDefault(
                        // fallback to flat
                        List(EQ_BAND_FREQUENCIES.size) { 0 }
                    )
            return List(gains.size) { index ->
                BandGain(band = EQ_BAND_FREQUENCIES[index], gain = gains[index])
            }
        }

        fun serializeGains(bandGains: List<BandGain>): String =
            bandGains.joinToString(separator = ",") { it.gain.toString() }
    }
}
