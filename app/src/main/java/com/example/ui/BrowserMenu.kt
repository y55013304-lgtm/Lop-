package com.example.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.WebTab

@Composable
fun BrowserMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    tab: WebTab?,
    isBookmarked: Boolean,
    adBlockEnabled: Boolean,
    blockedAdsCount: Int,
    javaScriptEnabled: Boolean,
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onToggleBookmark: () -> Unit,
    onFindInPage: () -> Unit,
    onToggleDesktop: () -> Unit,
    onToggleReader: () -> Unit,
    onToggleAdBlock: () -> Unit,
    onToggleJavaScript: () -> Unit,
    onOpenPasswordManager: () -> Unit,
    onShare: () -> Unit,
    onOpenSettings: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        DropdownMenuItem(
            text = { Text("新しいタブ") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            onClick = {
                onDismissRequest()
                onNewTab()
            }
        )

        DropdownMenuItem(
            text = { Text("新しいシークレットタブ") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            },
            onClick = {
                onDismissRequest()
                onNewIncognitoTab()
            }
        )

        HorizontalDivider()

        if (tab?.isHomePage == false) {
            DropdownMenuItem(
                text = { Text(if (isBookmarked) "ブックマークから削除" else "ブックマークに追加") },
                leadingIcon = {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    onDismissRequest()
                    onToggleBookmark()
                }
            )

            DropdownMenuItem(
                text = { Text("ページ内を検索") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    onDismissRequest()
                    onFindInPage()
                }
            )

            DropdownMenuItem(
                text = { Text("リーダーモード") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = if (tab.isReaderMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    Checkbox(
                        checked = tab.isReaderMode,
                        onCheckedChange = {
                            onDismissRequest()
                            onToggleReader()
                        }
                    )
                },
                onClick = {
                    onDismissRequest()
                    onToggleReader()
                }
            )

            DropdownMenuItem(
                text = { Text("PC版サイト") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.DesktopWindows,
                        contentDescription = null,
                        tint = if (tab.isDesktopMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    Checkbox(
                        checked = tab.isDesktopMode,
                        onCheckedChange = {
                            onDismissRequest()
                            onToggleDesktop()
                        }
                    )
                },
                onClick = {
                    onDismissRequest()
                    onToggleDesktop()
                }
            )

            DropdownMenuItem(
                text = { Text("このページを共有") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    onDismissRequest()
                    onShare()
                }
            )

            HorizontalDivider()
        }

        // AdBlock status with blocked count
        DropdownMenuItem(
            text = {
                Text(
                    if (blockedAdsCount > 0) "広告ブロック (${blockedAdsCount}件除去)"
                    else "広告ブロック"
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.VpnLock,
                    contentDescription = null,
                    tint = if (adBlockEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                Checkbox(
                    checked = adBlockEnabled,
                    onCheckedChange = {
                        onDismissRequest()
                        onToggleAdBlock()
                    }
                )
            },
            onClick = {
                onDismissRequest()
                onToggleAdBlock()
            }
        )

        // JavaScript toggle with reload indication
        DropdownMenuItem(
            text = { Text("JavaScript実行") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = if (javaScriptEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                Checkbox(
                    checked = javaScriptEnabled,
                    onCheckedChange = {
                        onDismissRequest()
                        onToggleJavaScript()
                    }
                )
            },
            onClick = {
                onDismissRequest()
                onToggleJavaScript()
            }
        )

        // Google Password Manager
        DropdownMenuItem(
            text = { Text("Google パスワードマネージャー") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Password,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            },
            onClick = {
                onDismissRequest()
                onOpenPasswordManager()
            }
        )

        HorizontalDivider()

        DropdownMenuItem(
            text = { Text("設定") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            onClick = {
                onDismissRequest()
                onOpenSettings()
            }
        )
    }
}
