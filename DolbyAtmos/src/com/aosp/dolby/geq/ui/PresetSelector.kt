package com.aosp.dolby.geq.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.aosp.dolby.R
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetSelector(viewModel: EqualizerViewModel) {
    val presets by viewModel.presets.collectAsState()
    val currentPreset by viewModel.preset.collectAsState()

    // Tìm vị trí của preset hiện tại để khởi tạo Pager
    val initialPage = remember(presets, currentPreset) {
        presets.indexOf(currentPreset).coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { presets.size }
    )

    // Đồng bộ từ Pager (khi người dùng vuốt) sang ViewModel
    LaunchedEffect(pagerState.currentPage) {
        if (presets.isNotEmpty() && presets[pagerState.currentPage] != currentPreset) {
            viewModel.setPreset(presets[pagerState.currentPage])
        }
    }

    // Đồng bộ từ ViewModel (khi reset/xóa/thêm) sang Pager
    LaunchedEffect(currentPreset) {
        val index = presets.indexOf(currentPreset)
        if (index >= 0 && index != pagerState.currentPage) {
            pagerState.animateScrollToPage(index)
        }
    }

    var showNewPresetDialog by remember { mutableStateOf(false) }
    var showRenamePresetDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- KHU VỰC SLIDE CARD ---
        HorizontalPager(
            state = pagerState,
            // Để lộ một phần của card trước và sau
            contentPadding = PaddingValues(horizontal = 80.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) { page ->
            val preset = presets[page]
            
            // Tính toán khoảng cách từ thẻ hiện tại đến trung tâm màn hình
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            
            // Hiệu ứng scale (thẻ ở giữa to nhất, thẻ hai bên nhỏ lại)
            val scale = lerp(start = 0.85f, stop = 1f, fraction = 1f - pageOffset.coerceIn(0f, 1f))
            // Hiệu ứng mờ (thẻ hai bên mờ hơn)
            val alpha = lerp(start = 0.5f, stop = 1f, fraction = 1f - pageOffset.coerceIn(0f, 1f))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (pageOffset < 0.5f) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (pageOffset < 0.5f) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (pageOffset < 0.5f) 6.dp else 0.dp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp) // Chiều cao của thẻ
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                ) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (pageOffset < 0.5f) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // --- CÁC NÚT THAO TÁC NẰM DƯỚI CAROUSEL ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TooltipIconButton(
                icon = ImageVector.vectorResource(id = R.drawable.save_as_24px),
                text = stringResource(id = R.string.dolby_geq_new_preset),
                onClick = { showNewPresetDialog = true }
            )

            if (currentPreset.isUserDefined) {
                TooltipIconButton(
                    icon = Icons.Default.Edit,
                    text = stringResource(id = R.string.dolby_geq_rename_preset),
                    onClick = { showRenamePresetDialog = true }
                )
                TooltipIconButton(
                    icon = Icons.Default.Delete,
                    text = stringResource(id = R.string.dolby_geq_delete_preset),
                    onClick = { showDeleteConfirmDialog = true }
                )
            }

            TooltipIconButton(
                icon = ImageVector.vectorResource(id = R.drawable.reset_settings_24px),
                text = stringResource(id = R.string.dolby_geq_reset_gains),
                onClick = {
                    if (currentPreset.isUserDefined) {
                        showResetConfirmDialog = true
                    } else {
                        viewModel.reset()
                    }
                }
            )
        }
    }

    // Các phần Dialog giữ nguyên như cũ
    if (showNewPresetDialog) {
        PresetNameDialog(
            title = stringResource(id = R.string.dolby_geq_new_preset),
            onPresetNameSet = { viewModel.createNewPreset(name = it) },
            onDismissDialog = { showNewPresetDialog = false }
        )
    }

    if (showRenamePresetDialog) {
        PresetNameDialog(
            title = stringResource(id = R.string.dolby_geq_rename_preset),
            presetName = currentPreset.name,
            onPresetNameSet = { viewModel.renamePreset(preset = currentPreset, name = it) },
            onDismissDialog = { showRenamePresetDialog = false }
        )
    }

    if (showDeleteConfirmDialog) {
        ConfirmationDialog(
            text package com.aosp.dolby.geq.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.aosp.dolby.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetSelector(viewModel: EqualizerViewModel) {
    val presets by viewModel.presets.collectAsState()
    val currentPreset by viewModel.preset.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var showNewPresetDialog by remember { mutableStateOf(false) }
    var showRenamePresetDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier
                .padding(end = 8.dp)
                .weight(1f)
        ) {
            TextField(
                value = currentPreset.name,
                onValueChange = { },
                readOnly = true,
                label = {
                    Text(
                        stringResource(id = R.string.dolby_geq_preset)
                    )
                },
                singleLine = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded
                    )
                },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier.menuAnchor()
                    // prevent keyboard from popping up
                    .focusProperties { canFocus = false }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                presets.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(text = preset.name) },
                        onClick = {
                            viewModel.setPreset(preset)
                            expanded = false
                        }
                    )
                }
            }
        }

        TooltipIconButton(
            icon = ImageVector.vectorResource(
                id = R.drawable.save_as_24px
            ),
            text = stringResource(id = R.string.dolby_geq_new_preset),
            onClick = { showNewPresetDialog = true }
        )

        if (currentPreset.isUserDefined) {
            TooltipIconButton(
                icon = Icons.Default.Edit,
                text = stringResource(id = R.string.dolby_geq_rename_preset),
                onClick = { showRenamePresetDialog = true }
            )
            TooltipIconButton(
                icon = Icons.Default.Delete,
                text = stringResource(id = R.string.dolby_geq_delete_preset),
                onClick = { showDeleteConfirmDialog = true }
            )
        }

        TooltipIconButton(
            icon = ImageVector.vectorResource(
                id = R.drawable.reset_settings_24px
            ),
            text = stringResource(id = R.string.dolby_geq_reset_gains),
            onClick = {
                if (currentPreset.isUserDefined) {
                    showResetConfirmDialog = true
                } else {
                    viewModel.reset()
                }
            }
        )
    }

    // Dialogs

    if (showNewPresetDialog) {
        PresetNameDialog(
            title = stringResource(id = R.string.dolby_geq_new_preset),
            onPresetNameSet = {
                return@PresetNameDialog viewModel.createNewPreset(name = it)
            },
            onDismissDialog = { showNewPresetDialog = false }
        )
    }

    if (showRenamePresetDialog) {
        PresetNameDialog(
            title = stringResource(id = R.string.dolby_geq_rename_preset),
            presetName = currentPreset.name,
            onPresetNameSet = {
                return@PresetNameDialog viewModel.renamePreset(
                    preset = currentPreset,
                    name = it
                )
            },
            onDismissDialog = { showRenamePresetDialog = false }
        )
    }

    if (showDeleteConfirmDialog) {
        ConfirmationDialog(
            text = stringResource(id = R.string.dolby_geq_delete_preset_prompt),
            onConfirm = { viewModel.deletePreset(currentPreset) },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }

    if (showResetConfirmDialog) {
        ConfirmationDialog(
            text = stringResource(id = R.string.dolby_geq_reset_gains_prompt),
            onConfirm = { viewModel.reset() },
            onDismiss = { showResetConfirmDialog = false }
        )
    }
}
= stringResource(id = R.string.dolby_geq_delete_preset_prompt),
            onConfirm = { viewModel.deletePreset(currentPreset) },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }

    if (showResetConfirmDialog) {
        ConfirmationDialog(
            text = stringResource(id = R.string.dolby_geq_reset_gains_prompt),
            onConfirm = { viewModel.reset() },
            onDismiss = { showResetConfirmDialog = false }
        )
    }
}