/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aosp.dolby.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aosp.dolby.R
import kotlinx.coroutines.launch

/**
 * A reusable component for enhancer sections (Bass, Mid, Treble) with optional curve selector.
 *
 * @param title The title of the enhancer section
 * @param subtitle The subtitle/summary of the enhancer section
 * @param icon The icon to display with the title
 * @param isEnabled Whether the enhancer is currently enabled
 * @param onEnabledChange Callback for when the enabled state changes
 * @param initialValueWhenEnabled The value to set when enabling the enhancer (if currently 0)
 * @param onEnabledZeroValue Value to set when disabling (usually 0)
 * @param sliderTitle Title for the level slider
 * @param sliderValue Current slider value (0-100)
 * @param onSliderChange Callback for slider value changes
 * @param showSelector Whether to show the curve selector (true for Bass only)
 * @param selectorTitle Title for the curve selector
 * @param selectorValue Current selector value
 * @param selectorEntries Resource ID for selector entries array
 * @param selectorValues Resource ID for selector values array
 * @param onSelectorChange Callback for selector value changes
 * @param selectorIcon Icon for the selector
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModernEnhancerSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    initialValueWhenEnabled: Int,
    onEnabledZeroValue: Int,
    sliderTitle: String,
    sliderValue: Int,
    onSliderChange: (Int) -> Unit,
    showSelector: Boolean = false,
    selectorTitle: String = "",
    selectorValue: Int = 0,
    selectorEntries: Int = 0,
    selectorValues: Int = 0,
    onSelectorChange: ((Int) -> Unit)? = null,
    selectorIcon: ImageVector? = null
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()

    Column {
        ModernSettingSwitch(
            title = title,
            subtitle = subtitle,
            checked = isEnabled,
            onCheckedChange = { enabled ->
                scope.launch {
                    haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
                }
                onEnabledChange(enabled)

                // Set initial value when enabling from zero, or zero when disabling
                if (enabled && sliderValue == onEnabledZeroValue) {
                    // TODO: This would need to call the appropriate ViewModel function
                    // For now, the caller handles the ViewModel updates via the onSliderChange/onSelectorChange callbacks
                } else if (!enabled) {
                    // TODO: This would need to call the appropriate ViewModel function to set zero
                    // For now, the caller handles the ViewModel updates via the onSliderChange/onSelectorChange callbacks
                }
            },
            icon = icon
        )

        AnimatedVisibility(visible = isEnabled) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))

                if (showSelector) {
                    ModernSettingSelector(
                        title = selectorTitle,
                        currentValue = selectorValue,
                        entries = selectorEntries,
                        values = selectorValues,
                        onValueChange = { onSelectorChange?.invoke(it) },
                        icon = selectorIcon
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                ModernSettingSlider(
                    title = sliderTitle,
                    value = sliderValue,
                    onValueChange = { onSliderChange(it) },
                    valueRange = 0f..100f,
                    steps = 19,
                    valueLabel = { "$it%" }
                )
            }
        }
    }
}