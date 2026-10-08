/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.dolby

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalContext
import com.android.dolby.ui.DolbyRoute

class DolbyActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Compose draws its own top app bar; hide the platform one that comes with the
        // Android (DeviceDefault) theme.
        actionBar?.apply {
            setShowHideAnimationEnabled(false)
            hide()
        }
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val colors =
                if (isSystemInDarkTheme()) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            MaterialTheme(colorScheme = colors) { DolbyRoute() }
        }
    }
}
