package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.WebTab

@Composable
fun BottomBar(
    tab: WebTab?,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onGoHome: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onGoBack,
                enabled = tab?.canGoBack == true,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("nav_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "戻る",
                    tint = if (tab?.canGoBack == true) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    }
                )
            }

            IconButton(
                onClick = onGoForward,
                enabled = tab?.canGoForward == true,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("nav_forward_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "進む",
                    tint = if (tab?.canGoForward == true) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    }
                )
            }

            IconButton(
                onClick = onGoHome,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("nav_home_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "ホーム",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = onOpenBookmarks,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("nav_bookmarks_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmarks,
                    contentDescription = "ブックマークと履歴",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = onShare,
                enabled = tab?.isHomePage == false && tab?.url?.isNotBlank() == true,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("nav_share_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "共有",
                    tint = if (tab?.isHomePage == false && tab?.url?.isNotBlank() == true) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    }
                )
            }
        }
    }
}
