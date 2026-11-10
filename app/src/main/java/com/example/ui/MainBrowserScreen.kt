package com.example.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.WebTab
import com.example.viewmodel.BrowserViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MainBrowserScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val isTabSwitcherOpen by viewModel.isTabSwitcherOpen.collectAsStateWithLifecycle()
    val isBookmarksHistoryOpen by viewModel.isBookmarksHistoryOpen.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()
    val isFindInPageOpen by viewModel.isFindInPageOpen.collectAsStateWithLifecycle()
    val findQuery by viewModel.findQuery.collectAsStateWithLifecycle()
    val findMatchCount by viewModel.findMatchCount.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val pendingDownload by viewModel.pendingDownload.collectAsStateWithLifecycle()
    val currentTabBlockedCount by viewModel.currentTabBlockedCount.collectAsStateWithLifecycle()
    val totalBlockedAds by viewModel.totalBlockedAds.collectAsStateWithLifecycle()

    var isMenuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect snackbar toasts
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Smart Back Handler
    BackHandler(enabled = true) {
        when {
            isFindInPageOpen -> viewModel.closeFindInPage()
            isTabSwitcherOpen -> viewModel.setTabSwitcherOpen(false)
            isBookmarksHistoryOpen -> viewModel.setBookmarksHistoryOpen(false)
            isSettingsOpen -> viewModel.setSettingsOpen(false)
            activeTab?.threatAlert != null -> viewModel.goBack()
            activeTab?.canGoBack == true -> viewModel.goBack()
            activeTab?.isHomePage == false -> viewModel.goHome()
            tabs.size > 1 -> viewModel.closeTab(activeTabId)
            else -> {
                (context as? android.app.Activity)?.finish()
            }
        }
    }

    // Share Handler
    val shareCurrentPage = {
        val currentUrl = activeTab?.url
        if (!currentUrl.isNullOrBlank()) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, activeTab?.title ?: "Web Page")
                putExtra(Intent.EXTRA_TEXT, currentUrl)
            }
            context.startActivity(Intent.createChooser(shareIntent, "リンクを共有"))
        }
    }

    // Tab Switcher Screen Overlay
    if (isTabSwitcherOpen) {
        TabSwitcher(
            tabs = tabs,
            activeTabId = activeTabId,
            onSelectTab = { viewModel.selectTab(it) },
            onCloseTab = { viewModel.closeTab(it) },
            onNewTab = { isIncognito -> viewModel.openNewTab(isIncognito = isIncognito) },
            onCloseAllTabs = { viewModel.closeAllTabs() },
            onDismiss = { viewModel.setTabSwitcherOpen(false) }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    Omnibox(
                        tab = activeTab,
                        tabCount = tabs.size,
                        onNavigate = { viewModel.navigateTo(it) },
                        onReload = { viewModel.reload() },
                        onStop = { viewModel.stopLoading() },
                        onOpenTabSwitcher = { viewModel.setTabSwitcherOpen(true) },
                        onOpenMenu = { isMenuExpanded = true }
                    )

                    if (isFindInPageOpen) {
                        FindInPageBar(
                            query = findQuery,
                            matchCount = findMatchCount,
                            onQueryChange = { viewModel.setFindQuery(it) },
                            onFindNext = { forward -> viewModel.findNext(forward) },
                            onClose = { viewModel.closeFindInPage() }
                        )
                    }

                    // Google Password Manager Autofill Quick Bar
                    val showQuickBar = activeTab?.hasPasswordField == true &&
                            settings.showPasswordManagerQuickBar &&
                            activeTab?.isHomePage == false
                    AnimatedVisibility(
                        visible = showQuickBar,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "パスワード入力欄を検出",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { viewModel.triggerAutofill() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Googleで自動入力", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { viewModel.openGooglePasswordManager(context) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Password,
                                        contentDescription = "Google パスワードマネージャー",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Dropdown Menu
                    BrowserMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        tab = activeTab,
                        isBookmarked = bookmarks.any { it.url == activeTab?.url },
                        adBlockEnabled = settings.isAdBlockActive,
                        blockedAdsCount = currentTabBlockedCount,
                        javaScriptEnabled = settings.javaScriptEnabled,
                        onNewTab = { viewModel.openNewTab() },
                        onNewIncognitoTab = { viewModel.openNewTab(isIncognito = true) },
                        onToggleBookmark = { viewModel.toggleBookmarkCurrentPage() },
                        onFindInPage = { viewModel.openFindInPage() },
                        onToggleDesktop = { viewModel.toggleDesktopMode() },
                        onToggleReader = { viewModel.toggleReaderMode() },
                        onToggleAdBlock = {
                            val newLevel = if (settings.isAdBlockActive) com.example.model.AdBlockLevel.OFF else com.example.model.AdBlockLevel.AGGRESSIVE
                            viewModel.updateAdBlockLevel(newLevel)
                        },
                        onToggleJavaScript = { viewModel.toggleJavaScript() },
                        onOpenPasswordManager = { viewModel.openGooglePasswordManager(context) },
                        onShare = shareCurrentPage,
                        onOpenSettings = { viewModel.setSettingsOpen(true) }
                    )
                }
            },
            bottomBar = {
                BottomBar(
                    tab = activeTab,
                    onGoBack = { viewModel.goBack() },
                    onGoForward = { viewModel.goForward() },
                    onGoHome = { viewModel.goHome() },
                    onOpenBookmarks = { viewModel.setBookmarksHistoryOpen(true) },
                    onShare = shareCurrentPage
                )
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val currentTab = activeTab
                if (currentTab?.threatAlert != null) {
                    // Show Intercepted Dangerous Site Warning Screen
                    ThreatWarningScreen(
                        threatAlert = currentTab.threatAlert,
                        onGoBack = { viewModel.goBack() },
                        onProceedAnyway = { viewModel.bypassThreatAndContinue(currentTab.id) },
                        onScanWithVirusTotal = { viewModel.scanWithVirusTotal(it) }
                    )
                } else if (currentTab == null || currentTab.isHomePage) {
                    NewTabPage(
                        isIncognito = currentTab?.isIncognito == true,
                        searchEngine = settings.searchEngine,
                        bookmarks = bookmarks,
                        history = history,
                        onNavigate = { viewModel.navigateTo(it) }
                    )
                } else {
                    WebViewContainer(
                        tab = currentTab,
                        settings = settings,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // Bookmarks and History Dialog
    if (isBookmarksHistoryOpen) {
        BookmarksHistoryDialog(
            bookmarks = bookmarks,
            history = history,
            onSelectUrl = { viewModel.navigateTo(it) },
            onDeleteBookmark = { viewModel.removeBookmark(it) },
            onDeleteHistoryItem = { viewModel.deleteHistoryItem(it) },
            onClearAllHistory = { viewModel.clearAllHistory() },
            onDismiss = { viewModel.setBookmarksHistoryOpen(false) }
        )
    }

    // Download Protection Interception Modal
    pendingDownload?.let { download ->
        DownloadProtectionDialog(
            pendingDownload = download,
            onConfirmDownload = { viewModel.confirmDownload(context, download) },
            onCancelDownload = { viewModel.cancelDownload() },
            onScanWithVirusTotal = { viewModel.scanWithVirusTotal(it) }
        )
    }

    // Settings Dialog
    if (isSettingsOpen) {
        SettingsDialog(
            settings = settings,
            currentTabBlockedCount = currentTabBlockedCount,
            totalBlockedAds = totalBlockedAds,
            onUpdateSearchEngine = { viewModel.updateSearchEngine(it) },
            onUpdateAdBlockLevel = { viewModel.updateAdBlockLevel(it) },
            onToggleAdBlockCosmetic = { viewModel.toggleAdBlockCosmeticFiltering() },
            onResetAdBlockStats = { viewModel.resetAdBlockStats() },
            onToggleJavaScript = { viewModel.toggleJavaScript() },
            onToggleBlockPopups = { viewModel.toggleBlockPopups() },
            onToggleDoNotTrack = { viewModel.toggleDoNotTrack() },
            onToggleBlockThirdPartyCookies = { viewModel.toggleBlockThirdPartyCookies() },
            onToggleWarnInsecure = { viewModel.toggleWarnInsecure() },
            onToggleClearOnExit = { viewModel.toggleClearOnExit() },
            onToggleSafeBrowsingGuard = { viewModel.toggleSafeBrowsingGuard() },
            onToggleDownloadProtection = { viewModel.toggleDownloadProtection() },
            onOpenGooglePasswordManager = { viewModel.openGooglePasswordManager(context) },
            onOpenPasswordCheckup = { viewModel.openPasswordCheckup(context) },
            onOpenAndroidAutofillSettings = { viewModel.openAutofillSettings(context) },
            onToggleGooglePasswordManagerAutofill = { viewModel.toggleGooglePasswordManagerAutofill() },
            onToggleShowPasswordManagerQuickBar = { viewModel.toggleShowPasswordManagerQuickBar() },
            onToggleLoadImages = { viewModel.toggleLoadImages() },
            onToggleSupportZoom = { viewModel.toggleSupportZoom() },
            onUpdateThemeMode = { viewModel.updateThemeMode(it) },
            onUpdateTextZoom = { viewModel.updateTextZoom(it) },
            onToggleForceDarkWeb = { viewModel.toggleForceDarkWeb() },
            onToggleDesktopByDefault = { viewModel.toggleDesktopByDefault() },
            onClearBrowsingData = { h, c, k -> viewModel.clearBrowsingData(h, c, k) },
            onResetSettings = { viewModel.resetSettingsToDefault() },
            onDismiss = { viewModel.setSettingsOpen(false) }
        )
    }
}
