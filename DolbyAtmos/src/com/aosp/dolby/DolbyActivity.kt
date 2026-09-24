package com.aosp.dolby

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.rememberNavController
import com.aosp.dolby.ui.DolbyScreen
import com.aosp.dolby.ui.DolbyViewModel
import com.android.settingslib.spa.framework.compose.localNavController
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.android.settingslib.spa.widget.scaffold.SettingsScaffold

class DolbyActivity : ComponentActivity() {

    private val viewModel: DolbyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingsTheme {
                MainContent()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Profile / state may have changed from the QS tile or elsewhere.
        viewModel.refresh()
    }

    @Composable
    private fun MainContent() {
        val navController = rememberNavController()
        CompositionLocalProvider(navController.localNavController()) {
            SettingsScaffold(
                title = stringResource(id = R.string.dolby_title)
            ) { paddingValues ->
                DolbyScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}
