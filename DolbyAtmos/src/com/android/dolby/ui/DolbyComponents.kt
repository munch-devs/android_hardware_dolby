/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.dolby.ui

import android.R as AndroidR
import androidx.annotation.ArrayRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Parsed `*_entries` / `*_values` array pair. Built once per configuration, not per frame. */
@Immutable
internal class Choices(val labels: List<String>, val values: List<Int>) {
    val size: Int
        get() = minOf(labels.size, values.size)

    /** Index of [value], or 0 when the value isn't one of the known steps. */
    fun indexOf(value: Int): Int = values.indexOf(value).coerceAtLeast(0)
}

@Composable
internal fun rememberChoices(@ArrayRes labels: Int, @ArrayRes values: Int): Choices {
    val res = LocalContext.current.resources
    val config = LocalConfiguration.current // re-parse on locale change
    return remember(labels, values, config) {
        Choices(res.getStringArray(labels).toList(), res.getStringArray(values).map { it.toInt() })
    }
}

/** Rounded surface card that groups related rows, with an optional icon + title header. */
@Composable
internal fun SettingsCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            if (title != null) {
                Row(
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (icon != null) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            content()
        }
    }
}

@Composable
internal fun SwitchRow(
    @StringRes title: Int,
    checked: Boolean,
    enabled: Boolean,
    supporting: String?,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = supporting?.let { { Text(it) } },
        leadingContent = icon?.let { { Icon(it, contentDescription = null) } },
        // The whole row toggles; the Switch itself is display-only (better for TalkBack).
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier =
            Modifier.toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
    )
}

/**
 * Slider that snaps to the entries of [choices]. The value is committed when the thumb is
 * released (or on tap), so dragging across 4 steps writes to the effect once, not 4 times.
 */
@Composable
internal fun StepSliderRow(
    title: String,
    choices: Choices,
    selected: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    supporting: String? = null,
    icon: ImageVector? = null,
) {
    val selectedIndex = choices.indexOf(selected)
    // Re-seeded whenever the committed value changes (e.g. changed from the QS tile).
    var position by remember(selectedIndex) { mutableFloatStateOf(selectedIndex.toFloat()) }
    val current = choices.labels[position.roundToInt()]

    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Same 24dp icon + 16dp gap as SwitchRow, so every row of a card lines up.
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 16.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (supporting != null) {
                    Text(
                        supporting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                current,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = position,
            onValueChange = { position = it },
            onValueChangeFinished = {
                val value = choices.values[position.roundToInt()]
                if (value != selected) onSelect(value)
            },
            valueRange = 0f..(choices.size - 1).toFloat(),
            steps = (choices.size - 2).coerceAtLeast(0),
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().semantics { stateDescription = current },
        )
        // Tick captions; the horizontal inset matches the slider thumb's own padding.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            choices.labels.take(choices.size).forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Square-ish selectable tile with an icon above a label. Used for the 2x2 IEQ grid. */
@Composable
internal fun OptionTile(
    label: String,
    @DrawableRes icon: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 40.dp,
) {
    val colors = MaterialTheme.colorScheme
    val container by
        animateColorAsState(
            if (selected) colors.primaryContainer else colors.surfaceContainerHighest,
            label = "tileContainer",
        )
    val content = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant
    Column(
        modifier
            .clip(MaterialTheme.shapes.large)
            .background(container)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(iconSize))
        Text(label, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
    }
}

/** One destination of [FloatingTabBar]. */
@Immutable
internal data class TabItem(@StringRes val label: Int, val icon: ImageVector)

/** Floating pill navigation: the selected tab expands to icon + label, the others are icons. */
@Composable
internal fun FloatingTabBar(
    tabs: List<TabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        modifier = modifier,
    ) {
        Row(
            Modifier.padding(6.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { i, tab ->
                val selected = i == selectedIndex
                val bg by
                    animateColorAsState(
                        if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        label = "tabBg",
                    )
                val fg =
                    if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                val label = stringResource(tab.label)
                Row(
                    Modifier.animateContentSize()
                        .clip(CircleShape)
                        .background(bg)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(i) })
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = if (selected) null else label,
                        tint = fg,
                        modifier = Modifier.size(24.dp),
                    )
                    AnimatedVisibility(selected) {
                        Text(
                            label,
                            color = fg,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ConfirmDialog(
    title: String,
    icon: ImageVector,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(icon, contentDescription = null) },
        title = { Text(title) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(AndroidR.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(AndroidR.string.cancel)) }
        },
    )
}
