package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WebTab
import com.example.ui.theme.InsecureRed
import com.example.ui.theme.SecureLockGreen

@Composable
fun Omnibox(
    tab: WebTab?,
    tabCount: Int,
    onNavigate: (String) -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onOpenTabSwitcher: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Keep input text synced with tab url when not actively typing
    LaunchedEffect(tab?.url, isEditing) {
        if (!isEditing) {
            inputText = if (tab?.isHomePage == true) "" else (tab?.url ?: "")
        }
    }

    val isIncognito = tab?.isIncognito == true
    val containerBg = if (isIncognito) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    val inputBg = if (isIncognito) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant

    Surface(
        color = containerBg,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                // Incognito indicator if active
                if (isIncognito) {
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "シークレット",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Omnibox URL / Search input box
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(inputBg)
                        .padding(horizontal = 12.dp)
                ) {
                    // Security or Search Icon
                    if (!isEditing && tab?.isHomePage == false) {
                        if (tab.isSecure) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "保護された通信",
                                tint = SecureLockGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = "保護されていない通信",
                                tint = InsecureRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "検索",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Text Field
                    Box(modifier = Modifier.weight(1f)) {
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    if (inputText.isNotBlank()) {
                                        onNavigate(inputText)
                                        focusManager.clearFocus()
                                    }
                                }
                            ),
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { focusState ->
                                    isEditing = focusState.isFocused
                                    if (focusState.isFocused && tab?.isHomePage == false) {
                                        inputText = tab.url
                                    }
                                }
                                .testTag("omnibox_input")
                        )

                        if (inputText.isEmpty() && !isEditing) {
                            Text(
                                text = "URLを入力 または 検索",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Clear button or Refresh / Stop button
                    if (isEditing && inputText.isNotEmpty()) {
                        IconButton(
                            onClick = { inputText = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "クリア",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else if (tab?.isHomePage == false) {
                        IconButton(
                            onClick = {
                                if (tab.isLoading) onStop() else onReload()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (tab.isLoading) Icons.Default.Close else Icons.Default.Refresh,
                                contentDescription = if (tab.isLoading) "中止" else "再読み込み",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Tab Switcher Button
                IconButton(
                    onClick = onOpenTabSwitcher,
                    modifier = Modifier.testTag("tab_switcher_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabCount.toString(),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Menu button
                IconButton(
                    onClick = onOpenMenu,
                    modifier = Modifier.testTag("browser_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "メニュー",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Loading Progress Bar
            AnimatedVisibility(
                visible = tab?.isLoading == true,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    progress = { (tab?.progress ?: 0) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            }
        }
    }
}
