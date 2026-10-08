/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.dolby.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
import com.android.dolby.R
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/** Everything one flashcard needs; built once, so pager pages don't re-resolve resources. */
@Immutable
private class ProfileCard(
    val value: Int,
    val name: String,
    val description: String,
    @DrawableRes val image: Int,
)

/** Profile ids (see dolby_profile_values, mirrors dax-default.xml) -> artwork. */
@DrawableRes
private fun imageFor(value: Int): Int =
    when (value) {
        0 -> R.drawable.img_profile_dynamic
        1 -> R.drawable.img_profile_video
        2 -> R.drawable.img_profile_music
        4 -> R.drawable.img_profile_mobility_default
        5 -> R.drawable.img_profile_mobility_on_the_go
        6 -> R.drawable.img_profile_mobility_commute
        7 -> R.drawable.img_profile_mobility_travel
        else -> R.drawable.img_profile_custom
    }

/**
 * Horizontally swipeable profile flashcards. The profile is applied as soon as the pager *settles*
 * on a card (not while it is still flicking past), so passing over "Movie" on the way to "Music"
 * doesn't briefly switch the audio chain. Tapping a peeking neighbour scrolls to it, which applies it.
 */
@Composable
internal fun ProfileCarousel(
    profile: Int,
    enabled: Boolean,
    onProfileChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val descriptions = stringArrayResource(R.array.dolby_profile_descriptions).toList()
    val values = rememberChoices(R.array.dolby_profile_entries, R.array.dolby_profile_values)
    val cards =
        remember(values, descriptions) {
            List(values.size) { i ->
                ProfileCard(
                    value = values.values[i],
                    name = values.labels[i],
                    description = descriptions.getOrElse(i) { "" },
                    image = imageFor(values.values[i]),
                )
            }
        }

    val selectedIndex = cards.indexOfFirst { it.value == profile }
    val pagerState = rememberPagerState(initialPage = selectedIndex.coerceAtLeast(0)) { cards.size }

    val scope = rememberCoroutineScope()
    val currentEnabled by rememberUpdatedState(enabled)
    val currentCards by rememberUpdatedState(cards)
    val currentOnProfileChange by rememberUpdatedState(onProfileChange)

    // Swipe -> apply. settledPage only changes once the pager has come to rest, so a fast flick
    // across several cards results in a single write. drop(1) skips the initial page, which
    // would otherwise overwrite an unknown profile (selectedIndex == -1) with page 0.
    // The ViewModel ignores a profile that is already active, so echoes are harmless.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .drop(1)
            .distinctUntilChanged()
            .collect { page ->
                if (currentEnabled) currentOnProfileChange(currentCards[page].value)
            }
    }

    // Apply -> follow. Changes made elsewhere (QS tile, reset) move the pager, unless the user
    // is dragging it right now.
    LaunchedEffect(selectedIndex) {
        if (
            selectedIndex >= 0 &&
                selectedIndex != pagerState.settledPage &&
                !pagerState.isScrollInProgress
        ) {
            pagerState.animateScrollToPage(selectedIndex)
        }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 40.dp),
            pageSpacing = 12.dp,
            userScrollEnabled = enabled,
            key = { cards[it].value },
        ) { page ->
            val card = cards[page]
            ProfileFlashcard(
                card = card,
                selected = card.value == profile,
                enabled = enabled,
                onSelect = { scope.launch { pagerState.animateScrollToPage(page) } },
                // Neighbours shrink/fade slightly for a "deck" feel.
                modifier =
                    Modifier.graphicsLayer {
                        val offset =
                            abs((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                                .coerceIn(0f, 1f)
                        val s = 1f - 0.08f * offset
                        scaleX = s
                        scaleY = s
                        alpha = 1f - 0.4f * offset
                    },
            )
        }
        PageDots(pagerState, Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun ProfileFlashcard(
    card: ProfileCard,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val container by
        animateColorAsState(
            if (selected) colors.primaryContainer else colors.surfaceContainerHighest,
            label = "cardContainer",
        )
    val content = if (selected) colors.onPrimaryContainer else colors.onSurface

    Card(
        onClick = onSelect,
        enabled = enabled,
        shape = MaterialTheme.shapes.extraLarge,
        colors =
            CardDefaults.cardColors(
                containerColor = container,
                contentColor = content,
                // Dimming is done once, on the whole page.
                disabledContainerColor = container,
                disabledContentColor = content,
            ),
        // Colour alone is not enough for screen readers.
        modifier = modifier.fillMaxWidth().semantics { this.selected = selected },
    ) {
        Column {
            Image(
                painter = painterResource(card.image),
                contentDescription = null, // decorative; the name below says it all
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(2.2f),
            )
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(card.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    card.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        if (selected) colors.onPrimaryContainer.copy(alpha = 0.8f)
                        else colors.onSurfaceVariant,
                    minLines = 3,
                    maxLines = 3,
                )
            }
        }
    }
}

@Composable
private fun PageDots(state: PagerState, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(state.pageCount) { i ->
            val active = i == state.currentPage
            val color by
                animateColorAsState(
                    if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                    label = "dot",
                )
            Box(Modifier.size(if (active) 10.dp else 8.dp).clip(CircleShape).background(color))
        }
    }
}
