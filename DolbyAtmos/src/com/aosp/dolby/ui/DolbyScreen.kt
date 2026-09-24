package com.aosp.dolby.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.aosp.dolby.R
import com.aosp.dolby.geq.EqualizerActivity
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private data class Option(val value: Int, val label: String)

@Composable
private fun options(entriesRes: Int, valuesRes: Int): List<Option> {
    val labels = stringArrayResource(entriesRes)
    val values = stringArrayResource(valuesRes)
    return labels.indices.map { Option(values[it].toInt(), labels[it]) }
}

private const val DISABLED_ALPHA = 0.38f

@Composable
fun DolbyScreen(
    viewModel: DolbyViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val profiles = options(R.array.dolby_profile_entries, R.array.dolby_profile_values)
    val ieqOptions = options(R.array.dolby_ieq_entries, R.array.dolby_ieq_values)
    val dialogueOptions = options(R.array.dolby_dialogue_entries, R.array.dolby_dialogue_values)
    val stereoOptions = options(R.array.dolby_stereo_entries, R.array.dolby_stereo_values)

    val currentProfileName = profiles.firstOrNull { it.value == state.profile }?.label
    val connectHeadphones = stringResource(R.string.dolby_connect_headphones)
    val resetToast = stringResource(
        R.string.dolby_reset_profile_toast,
        currentProfileName ?: stringResource(R.string.dolby_unknown)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Hero: main on/off ────────────────────────────────────────────────
        HeroCard(
            checked = state.dsOn,
            subtitle = when {
                !state.dsOn -> stringResource(R.string.dolby_off)
                currentProfileName != null ->
                    stringResource(R.string.dolby_on_with_profile, currentProfileName)
                else -> stringResource(R.string.dolby_on)
            },
            onCheckedChange = viewModel::setDsOn
        )

        // ── Profile (Slide Card Carousel) ────────────────────────────────────
        SectionTitle(stringResource(R.string.dolby_profile_title))
        SectionCard {
            ProfileSelectorCarousel(
                profiles = profiles,
                currentProfile = state.profile,
                enabled = state.dsOn,
                onProfileSelect = viewModel::setProfile
            )
        }

        // ── Equalizers ───────────────────────────────────────────────────────
        SectionTitle(stringResource(R.string.dolby_category_settings))
        SectionCard {
            NavigationRow(
                title = stringResource(R.string.dolby_preset),
                summary = state.presetName.takeIf { it.isNotEmpty() },
                enabled = state.controlsEnabled,
                onClick = {
                    context.startActivity(Intent(context, EqualizerActivity::class.java))
                }
            )
            CardDivider()
            IeqSelector(
                options = ieqOptions,
                current = state.ieq,
                enabled = state.controlsEnabled,
                onSelect = viewModel::setIeq
            )
        }

        // ── Levels ───────────────────────────────────────────────────────────
        SectionCard {
            LevelSetting(
                title = stringResource(R.string.dolby_dialogue_enhancer),
                options = dialogueOptions,
                current = state.dialogue,
                enabled = state.controlsEnabled,
                onSelect = viewModel::setDialogue
            )
            CardDivider()
            LevelSetting(
                title = stringResource(R.string.dolby_stereo_widening),
                summary = if (state.isOnSpeaker) connectHeadphones else null,
                options = stereoOptions,
                current = state.stereo,
                enabled = state.stereoEnabled,
                onSelect = viewModel::setStereo
            )
        }

        // ── Switches ─────────────────────────────────────────────────────────
        SectionCard {
            SwitchRow(
                title = stringResource(R.string.dolby_spk_virtualizer),
                checked = state.speakerVirt,
                enabled = state.controlsEnabled,
                onCheckedChange = viewModel::setSpeakerVirt
            )
            CardDivider()
            SwitchRow(
                title = stringResource(R.string.dolby_hp_virtualizer),
                summary = if (state.isOnSpeaker) connectHeadphones else null,
                checked = state.headphoneVirt,
                enabled = state.headphoneControlsEnabled,
                onCheckedChange = viewModel::setHeadphoneVirt
            )
            CardDivider()
            SwitchRow(
                title = stringResource(R.string.dolby_bass_enhancer),
                summary = if (state.isOnSpeaker) connectHeadphones else null,
                checked = state.bass,
                enabled = state.headphoneControlsEnabled,
                onCheckedChange = viewModel::setBass
            )
            CardDivider()
            SwitchRow(
                title = stringResource(R.string.dolby_volume_leveler),
                checked = state.volume,
                enabled = state.controlsEnabled,
                onCheckedChange = viewModel::setVolume
            )
        }

        // ── Reset ────────────────────────────────────────────────────────────
        FilledTonalButton(
            onClick = {
                viewModel.resetProfile()
                Toast.makeText(context, resetToast, Toast.LENGTH_SHORT).show()
            },
            enabled = state.controlsEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.reset_settings_24px),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.dolby_reset_profile))
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Building blocks
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileSelectorCarousel(
    profiles: List<Option>,
    currentProfile: Int,
    enabled: Boolean,
    onProfileSelect: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    // Tìm vị trí ban đầu
    val initialPage = remember(profiles, currentProfile) {
        profiles.indexOfFirst { it.value == currentProfile }.coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { profiles.size }
    )

    // Đồng bộ vuốt (swipe) sang ViewModel
    LaunchedEffect(pagerState.currentPage) {
        val selectedProfile = profiles[pagerState.currentPage].value
        if (enabled && selectedProfile != currentProfile) {
            onProfileSelect(selectedProfile)
        }
    }

    // Đồng bộ từ ViewModel sang Pager (khi đổi profile từ bên ngoài)
    LaunchedEffect(currentProfile) {
        val index = profiles.indexOfFirst { it.value == currentProfile }
        if (index >= 0 && index != pagerState.currentPage) {
            pagerState.animateScrollToPage(index)
        }
    }

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 80.dp),
        userScrollEnabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
    ) { page ->
        val option = profiles[page]
        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
        
        // Hiệu ứng scale và alpha
        val scale = lerp(start = 0.85f, stop = 1f, fraction = 1f - pageOffset.coerceIn(0f, 1f))
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
                .height(90.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                // Cho phép chạm vào thẻ hai bên để cuộn tới thẻ đó
                .clickable(
                    enabled = enabled && page != pagerState.currentPage,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(page)
                        }
                    }
                )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (pageOffset < 0.5f) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HeroCard(
    checked: Boolean,
    subtitle: String,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (checked) colors.primaryContainer else colors.surfaceContainerHighest,
        label = "heroContainer"
    )
    val content by animateColorAsState(
        if (checked) colors.onPrimaryContainer else colors.onSurfaceVariant,
        label = "heroContent"
    )
    val badge by animateColorAsState(
        if (checked) colors.primary else colors.outline,
        label = "heroBadge"
    )
    val shape = RoundedCornerShape(32.dp)

    Surface(
        shape = shape,
        color = container,
        contentColor = content,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(badge),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_dolby),
                    contentDescription = null,
                    tint = colors.surface,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.dolby_enable),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, top = 8.dp)
    )
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth(),
        content = content
    )
}

@Composable
private fun CardDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun NavigationRow(
    title: String,
    summary: String?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text(
            text = "›",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    summary: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun LevelSetting(
    title: String,
    options: List<Option>,
    current: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    summary: String? = null
) {
    val currentIndex = options.indexOfFirst { it.value == current }
    var position by remember(currentIndex) {
        mutableFloatStateOf(currentIndex.coerceAtLeast(0).toFloat())
    }
    var touched by remember(currentIndex) { mutableStateOf(false) }
    val shownIndex = position.roundToInt().coerceIn(options.indices)
    val label = if (currentIndex == -1 && !touched) {
        stringResource(R.string.dolby_unknown)
    } else {
        options[shownIndex].label
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                if (summary != null) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = position,
            onValueChange = {
                touched = true
                position = it
            },
            onValueChangeFinished = {
                val index = position.roundToInt().coerceIn(options.indices)
                position = index.toFloat()
                val newValue = options[index].value
                if (newValue != current) onSelect(newValue)
            },
            valueRange = 0f..(options.size - 1).toFloat(),
            steps = (options.size - 2).coerceAtLeast(0),
            enabled = enabled
        )
    }
}

@Composable
private fun IeqSelector(
    options: List<Option>,
    current: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit
) {
    val icons = mapOf(
        0 to R.drawable.ic_ieq_off,
        1 to R.drawable.ic_ieq_balanced,
        2 to R.drawable.ic_ieq_warm,
        3 to R.drawable.ic_ieq_detailed,
    )
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.dolby_ieq),
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val selected = current == option.value
                val background by animateColorAsState(
                    if (selected) colors.primaryContainer else colors.surfaceContainerHighest,
                    label = "ieqTile"
                )
                val tint = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(background)
                        .selectable(
                            selected = selected,
                            enabled = enabled,
                            role = Role.RadioButton,
                            onClick = { onSelect(option.value) }
                        )
                        .padding(vertical = 14.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(icons[option.value] ?: R.drawable.ic_ieq_off),
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = tint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}