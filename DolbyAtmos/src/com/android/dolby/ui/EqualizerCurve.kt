/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.dolby.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.android.dolby.geq.data.formatHz
import com.android.dolby.geq.data.BandGain
import kotlin.math.roundToInt

private const val MAX_GAIN = 100 // backend units; the UI shows -10..+10 dB
private const val SNAP = 5 // 0.5 dB

/** Maps band index / gain to canvas coordinates and back. */
private class Plot(
    width: Float,
    height: Float,
    val left: Float,
    right: Float,
    val top: Float,
    bottom: Float,
    private val bands: Int,
) {
    val w = width - left - right
    val h = height - top - bottom

    fun x(i: Int) = left + w * i / (bands - 1).coerceAtLeast(1)

    fun y(gain: Int) = top + h * (1f - (gain + MAX_GAIN) / (2f * MAX_GAIN))

    fun nearestBand(x: Float) =
        (((x - left) / w) * (bands - 1)).roundToInt().coerceIn(0, bands - 1)

    fun gainAt(y: Float): Int {
        val raw = ((1f - (y - top) / h) * 2f * MAX_GAIN - MAX_GAIN).coerceIn(-MAX_GAIN.toFloat(), MAX_GAIN.toFloat())
        return (raw / SNAP).roundToInt() * SNAP
    }
}

private fun Density.plotFor(size: Size, bands: Int) =
    Plot(size.width, size.height, 36.dp.toPx(), 14.dp.toPx(), 20.dp.toPx(), 26.dp.toPx(), bands)


/**
 * The same bands as the vertical sliders (10, 15 or 20), drawn as one smooth curve. Touch or drag anywhere:
 * the nearest band follows your finger and is written when you let go (same cadence as the sliders).
 */
@Composable
internal fun EqualizerCurve(
    gains: List<BandGain>,
    onGainChangeFinished: (index: Int, gain: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Local copy so dragging is instant; re-seeded whenever the committed gains change
    // (preset switched, reset...).
    val live = remember(gains) { mutableStateListOf<Int>().apply { addAll(gains.map { it.gain }) } }
    var active by remember { mutableIntStateOf(-1) }
    val finished by rememberUpdatedState(onGainChangeFinished)
    val committed by rememberUpdatedState(gains)

    val cs = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.labelSmall.copy(color = cs.onSurfaceVariant)
    val valueStyle = MaterialTheme.typography.labelMedium.copy(color = cs.primary)

    fun commit(i: Int) {
        if (i in live.indices && live[i] != committed[i].gain) finished(i, live[i])
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(220.dp)
            .pointerInput(live) {
                detectTapGestures { o ->
                    val plot = plotFor(size.toSize(), live.size)
                    val i = plot.nearestBand(o.x)
                    live[i] = plot.gainAt(o.y)
                    commit(i)
                }
            }
            .pointerInput(live) {
                var band = -1
                detectDragGestures(
                    onDragStart = { o ->
                        val plot = plotFor(size.toSize(), live.size)
                        band = plot.nearestBand(o.x)
                        active = band
                        live[band] = plot.gainAt(o.y)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        if (band >= 0) live[band] = plotFor(size.toSize(), live.size).gainAt(change.position.y)
                    },
                    onDragEnd = {
                        commit(band)
                        band = -1
                        active = -1
                    },
                    onDragCancel = {
                        commit(band)
                        band = -1
                        active = -1
                    },
                )
            }
    ) {
        val n = live.size
        if (n < 2) return@Canvas
        val plot = plotFor(size, n)

        // Grid: +10 / +5 / 0 / -5 / -10 dB, with the 0 dB line emphasised.
        for (g in listOf(100, 50, 0, -50, -100)) {
            val y = plot.y(g)
            drawLine(
                color = if (g == 0) cs.outline else cs.outlineVariant,
                start = Offset(plot.left, y),
                end = Offset(plot.left + plot.w, y),
                strokeWidth = if (g == 0) 1.5.dp.toPx() else 1.dp.toPx(),
            )
            if (g % 100 == 0) {
                val label = measurer.measure(if (g > 0) "+${g / 10}" else "${g / 10}", axisStyle)
                drawText(label, topLeft = Offset(plot.left - 8.dp.toPx() - label.size.width, y - label.size.height / 2f))
            }
        }

        // Curve: each segment is an S-curve with horizontal tangents at the nodes, so the line is
        // smooth but can never overshoot the actual band gains.
        val curve = Path().apply {
            moveTo(plot.x(0), plot.y(live[0]))
            for (i in 1 until n) {
                val mid = (plot.x(i - 1) + plot.x(i)) / 2f
                cubicTo(mid, plot.y(live[i - 1]), mid, plot.y(live[i]), plot.x(i), plot.y(live[i]))
            }
        }
        val area = Path().apply {
            addPath(curve)
            lineTo(plot.x(n - 1), plot.y(0))
            lineTo(plot.x(0), plot.y(0))
            close()
        }
        drawPath(area, cs.primary.copy(alpha = 0.14f))
        drawPath(curve, cs.primary, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        for (i in 0 until n) {
            val c = Offset(plot.x(i), plot.y(live[i]))
            val isActive = i == active
            drawCircle(cs.primary, radius = (if (isActive) 9 else 6).dp.toPx(), center = c)
            drawCircle(cs.surfaceContainerHigh, radius = (if (isActive) 4f else 2.5f).dp.toPx(), center = c)

            // With 15/20 bands the labels would overlap, so only every second one is drawn.
            if (n <= 10 || i % 2 == 0) {
                val f = measurer.measure(formatHz(gains[i].band), axisStyle)
                drawText(f, topLeft = Offset(c.x - f.size.width / 2f, plot.top + plot.h + 6.dp.toPx()))
            }

            if (isActive) {
                val v = measurer.measure("%+.1f dB".format(live[i] / 10f), valueStyle)
                val above = c.y - v.size.height - 14.dp.toPx() >= 0f
                val ty = if (above) c.y - v.size.height - 12.dp.toPx() else c.y + 12.dp.toPx()
                drawText(v, topLeft = Offset((c.x - v.size.width / 2f).coerceIn(0f, size.width - v.size.width), ty))
            }
        }
    }
}
