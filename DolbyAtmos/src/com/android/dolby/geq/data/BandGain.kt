/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.dolby.geq.data

import kotlin.math.roundToInt

data class BandGain(val band: Int, var gain: Int = 0)

/** 47 -> "47", 2000 -> "2k", 1313 -> "1.3k", 19688 -> "19.7k". */
fun formatHz(hz: Int): String =
    when {
        hz < 1000 -> "$hz"
        hz % 1000 == 0 -> "${hz / 1000}k"
        else -> String.format(java.util.Locale.US, "%.1fk", hz / 1000f)
    }

/** Centre frequencies (Hz) of the 20 backend bands, as in <band_geq frequency=...> of dax-default.xml. */
val EQ_BAND_FREQUENCIES = intArrayOf(
    47, 141, 234, 328, 469, 656, 844, 1031, 1313, 1688,
    2250, 3000, 3750, 4688, 5813, 7125, 9000, 11250, 13875, 19688,
)

/** Band counts the user can pick in the UI. Each one is a subset of the 20 backend bands. */
val EQ_BAND_COUNTS = listOf(10, 15, 20)

/**
 * Positions, in the 20-band backend array, of the control points shown for [count] bands.
 * Always includes the first and the last band; evenly spread in between.
 */
fun bandControlPoints(count: Int): List<Int> {
    val last = EQ_BAND_FREQUENCIES.size - 1
    if (count > last) return List(last + 1) { it }
    return List(count) { (it * last / (count - 1f)).roundToInt() }
}

/** 47 Hz -> "47 Hz", 19688 Hz -> "19.7 kHz". */
fun formatHzLong(hz: Int): String =
    if (hz < 1000) "$hz Hz" else formatHz(hz).replace("k", " kHz")
