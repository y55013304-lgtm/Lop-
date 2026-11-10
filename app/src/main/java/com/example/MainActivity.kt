package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppThemeMode
import com.example.ui.MainBrowserScreen
import com.example.ui.theme.LopBrowserTheme
import com.example.viewmodel.BrowserViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntentUrl(intent)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val isSystemDark = isSystemInDarkTheme()
            val isDarkTheme = when (settings.themeMode) {
                AppThemeMode.SYSTEM -> isSystemDark
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
            }

            LopBrowserTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainBrowserScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntentUrl(intent)
    }

    private fun handleIntentUrl(intent: Intent?) {
        val uri = intent?.data ?: return
        val url = uri.toString()
        if (url.isNotBlank()) {
            viewModel.openNewTab(url)
        }
    }
}
