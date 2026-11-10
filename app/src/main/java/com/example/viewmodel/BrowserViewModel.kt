package com.example.viewmodel

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BookmarkEntity
import com.example.data.BrowserRepository
import com.example.data.HistoryEntity
import com.example.model.AdBlockLevel
import com.example.model.AppThemeMode
import com.example.model.BrowserSettings
import com.example.model.SearchEngine
import com.example.model.WebTab
import com.example.ui.PendingDownload
import com.example.util.SafeBrowsingGuard
import com.example.util.ThreatAlert
import com.example.util.UrlUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class WebViewAction {
    data class LoadUrl(val tabId: String, val url: String) : WebViewAction()
    data class GoBack(val tabId: String) : WebViewAction()
    data class GoForward(val tabId: String) : WebViewAction()
    data class Reload(val tabId: String) : WebViewAction()
    data class Stop(val tabId: String) : WebViewAction()
    data class FindQuery(val tabId: String, val query: String) : WebViewAction()
    data class FindNext(val tabId: String, val forward: Boolean) : WebViewAction()
    data class ClearFind(val tabId: String) : WebViewAction()
    data class ToggleDesktop(val tabId: String, val enabled: Boolean) : WebViewAction()
    data class ToggleReader(val tabId: String, val enabled: Boolean) : WebViewAction()
    data class EvaluateJavascript(val tabId: String, val script: String) : WebViewAction()
    data class UpdateJavaScript(val tabId: String, val enabled: Boolean) : WebViewAction()
    data class UpdateTextZoom(val tabId: String, val textZoom: Int) : WebViewAction()
    data class UpdateLoadImages(val tabId: String, val loadImages: Boolean) : WebViewAction()
    data class ClearWebData(val clearCache: Boolean, val clearCookies: Boolean) : WebViewAction()
    data class TriggerAutofill(val tabId: String) : WebViewAction()
}

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = BrowserRepository(
        db.bookmarkDao(),
        db.historyDao(),
        db.settingsDao(),
        db.tabDao()
    )

    // Tabs state
    private val initialTab = WebTab(url = "", title = "新しいタブ")
    private val _tabs = MutableStateFlow<List<WebTab>>(listOf(initialTab))
    val tabs: StateFlow<List<WebTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(initialTab.id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    val activeTab: StateFlow<WebTab?> = combine(_tabs, _activeTabId) { tabList, activeId ->
        tabList.find { it.id == activeId } ?: tabList.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialTab)

    // UI Dialogs & Sheets
    private val _isTabSwitcherOpen = MutableStateFlow(false)
    val isTabSwitcherOpen: StateFlow<Boolean> = _isTabSwitcherOpen.asStateFlow()

    private val _isBookmarksHistoryOpen = MutableStateFlow(false)
    val isBookmarksHistoryOpen: StateFlow<Boolean> = _isBookmarksHistoryOpen.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    // Download Protection Interception
    private val _pendingDownload = MutableStateFlow<PendingDownload?>(null)
    val pendingDownload: StateFlow<PendingDownload?> = _pendingDownload.asStateFlow()

    // Find in Page
    private val _isFindInPageOpen = MutableStateFlow(false)
    val isFindInPageOpen: StateFlow<Boolean> = _isFindInPageOpen.asStateFlow()

    private val _findQuery = MutableStateFlow("")
    val findQuery: StateFlow<String> = _findQuery.asStateFlow()

    private val _findMatchCount = MutableStateFlow(Pair(0, 0)) // current, total
    val findMatchCount: StateFlow<Pair<Int, Int>> = _findMatchCount.asStateFlow()

    // Persistent Settings
    private val _settings = MutableStateFlow(BrowserSettings())
    val settings: StateFlow<BrowserSettings> = _settings.asStateFlow()

    // AdBlock statistics
    private val _pageBlockedCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val currentTabBlockedCount: StateFlow<Int> = combine(_activeTabId, _pageBlockedCounts) { id, map ->
        map[id] ?: 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _totalBlockedAds = MutableStateFlow(0L)
    val totalBlockedAds: StateFlow<Long> = _totalBlockedAds.asStateFlow()

    // Bookmarks and History from Room
    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.bookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // WebView Action Bus
    private val _actions = MutableSharedFlow<WebViewAction>()
    val actions: SharedFlow<WebViewAction> = _actions.asSharedFlow()

    // Feedback message (snackbar / toast)
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.populateDefaultsIfEmpty()
        }

        // Load saved tabs from Room database across app lifecycles
        viewModelScope.launch {
            val (savedTabs, savedActiveId) = repository.loadSavedTabs()
            if (savedTabs.isNotEmpty()) {
                _tabs.value = savedTabs
                if (savedActiveId != null && savedTabs.any { it.id == savedActiveId }) {
                    _activeTabId.value = savedActiveId
                } else {
                    _activeTabId.value = savedTabs.first().id
                }
            }
        }

        // Load persistent settings from Room database
        viewModelScope.launch {
            repository.savedSettings.collectLatest { saved ->
                if (saved != null) {
                    _settings.value = saved
                }
            }
        }
    }

    private fun persistCurrentSettings() {
        viewModelScope.launch {
            repository.persistSettings(_settings.value)
        }
    }

    private fun persistCurrentTabs() {
        viewModelScope.launch {
            repository.saveTabs(_tabs.value, _activeTabId.value)
        }
    }

    // Tab Operations
    fun openNewTab(url: String = "", isIncognito: Boolean = false) {
        val newTab = WebTab(
            url = url,
            title = if (url.isBlank()) "新しいタブ" else "読み込み中...",
            isIncognito = isIncognito,
            isDesktopMode = _settings.value.openDesktopByDefault
        )
        _tabs.value = _tabs.value + newTab
        _activeTabId.value = newTab.id
        _isTabSwitcherOpen.value = false
        persistCurrentTabs()
        if (url.isNotBlank()) {
            checkAndInterceptUrlThreat(newTab.id, url)
            viewModelScope.launch {
                _actions.emit(WebViewAction.LoadUrl(newTab.id, url))
            }
        }
    }

    fun selectTab(tabId: String) {
        if (_tabs.value.any { it.id == tabId }) {
            _activeTabId.value = tabId
            _isTabSwitcherOpen.value = false
            persistCurrentTabs()
        }
    }

    fun closeTab(tabId: String) {
        val currentList = _tabs.value
        if (currentList.size <= 1) {
            val fresh = WebTab(
                url = "",
                title = "新しいタブ",
                isDesktopMode = _settings.value.openDesktopByDefault
            )
            _tabs.value = listOf(fresh)
            _activeTabId.value = fresh.id
            persistCurrentTabs()
            return
        }

        val closingIndex = currentList.indexOfFirst { it.id == tabId }
        val newList = currentList.filterNot { it.id == tabId }
        _tabs.value = newList

        if (_activeTabId.value == tabId) {
            val newIndex = (closingIndex - 1).coerceAtLeast(0).coerceAtMost(newList.size - 1)
            _activeTabId.value = newList[newIndex].id
        }
        persistCurrentTabs()
    }

    fun closeAllTabs() {
        val fresh = WebTab(
            url = "",
            title = "新しいタブ",
            isDesktopMode = _settings.value.openDesktopByDefault
        )
        _tabs.value = listOf(fresh)
        _activeTabId.value = fresh.id
        _isTabSwitcherOpen.value = false
        persistCurrentTabs()
    }

    // Navigation & Malicious Site Detection
    fun navigateTo(input: String) {
        val currentTab = activeTab.value ?: return
        val targetUrl = UrlUtils.parseInput(input, _settings.value.searchEngine)
        if (targetUrl.isBlank()) return

        // Intercept dangerous site
        val isBlocked = checkAndInterceptUrlThreat(currentTab.id, targetUrl)
        if (isBlocked) {
            return
        }

        updateTab(currentTab.id) {
            it.copy(url = targetUrl, isLoading = true, progress = 10, threatAlert = null)
        }
        viewModelScope.launch {
            _actions.emit(WebViewAction.LoadUrl(currentTab.id, targetUrl))
        }
    }

    /**
     * Evaluates URL against VirusTotal / threat database.
     * Returns true if navigation was interrupted.
     */
    fun checkAndInterceptUrlThreat(tabId: String, url: String): Boolean {
        if (!_settings.value.enableSafeBrowsingGuard) return false
        val threat = SafeBrowsingGuard.evaluateUrl(url)
        if (threat != null) {
            updateTab(tabId) {
                it.copy(
                    url = url,
                    title = "⚠️ 危険サイト警告",
                    threatAlert = threat,
                    isLoading = false,
                    progress = 0
                )
            }
            viewModelScope.launch {
                _actions.emit(WebViewAction.Stop(tabId))
                _toastMessage.emit("⚠️ 危険なサイトへのアクセスを中断しました（VirusTotal脅威検知）")
            }
            return true
        }
        return false
    }

    fun bypassThreatAndContinue(tabId: String) {
        val tab = _tabs.value.find { it.id == tabId } ?: return
        val threat = tab.threatAlert ?: return
        SafeBrowsingGuard.allowHostBypass(threat.displayHost)
        updateTab(tabId) { it.copy(threatAlert = null) }
        viewModelScope.launch {
            _actions.emit(WebViewAction.LoadUrl(tabId, threat.url))
            _toastMessage.emit("警告を解除して続行します")
        }
    }

    fun scanWithVirusTotal(url: String) {
        val vtUrl = SafeBrowsingGuard.getVirusTotalSearchUrl(url)
        openNewTab(vtUrl)
    }

    // Download Protection & Interception
    fun onDownloadRequested(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long
    ) {
        val fileName = SafeBrowsingGuard.extractFileName(url, contentDisposition)
        val threat = if (_settings.value.enableDownloadProtection) {
            SafeBrowsingGuard.evaluateDownload(url, contentDisposition, mimeType)
        } else null

        val pending = PendingDownload(
            url = url,
            userAgent = userAgent,
            contentDisposition = contentDisposition,
            mimeType = mimeType,
            contentLength = contentLength,
            fileName = fileName,
            threatAlert = threat
        )

        _pendingDownload.value = pending

        if (threat != null) {
            viewModelScope.launch {
                _toastMessage.emit("🚨 危険なダウンロードを中断しました (${fileName})")
            }
        }
    }

    fun confirmDownload(context: Context, download: PendingDownload) {
        _pendingDownload.value = null
        try {
            val request = DownloadManager.Request(Uri.parse(download.url)).apply {
                setTitle(download.fileName)
                setDescription("Lopブラウザでダウンロード中...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, download.fileName)
                setMimeType(download.mimeType)
                addRequestHeader("User-Agent", download.userAgent)
            }
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            downloadManager?.enqueue(request)
            viewModelScope.launch {
                _toastMessage.emit("ダウンロードを開始しました: ${download.fileName}")
            }
        } catch (e: Exception) {
            viewModelScope.launch {
                _toastMessage.emit("ダウンロード開始エラー: ${e.localizedMessage}")
            }
        }
    }

    fun cancelDownload() {
        val current = _pendingDownload.value
        _pendingDownload.value = null
        if (current?.threatAlert != null) {
            viewModelScope.launch {
                _toastMessage.emit("危険なダウンロードを安全に破棄しました")
            }
        }
    }

    fun goBack() {
        val tab = activeTab.value ?: return
        if (tab.threatAlert != null) {
            updateTab(tab.id) { it.copy(threatAlert = null) }
            goHome()
            return
        }
        viewModelScope.launch {
            _actions.emit(WebViewAction.GoBack(tab.id))
        }
    }

    fun goForward() {
        val tabId = _activeTabId.value
        viewModelScope.launch {
            _actions.emit(WebViewAction.GoForward(tabId))
        }
    }

    fun reload() {
        val currentTab = activeTab.value ?: return
        if (currentTab.isHomePage) return
        if (currentTab.threatAlert != null) return
        viewModelScope.launch {
            _actions.emit(WebViewAction.Reload(currentTab.id))
        }
    }

    fun stopLoading() {
        val tabId = _activeTabId.value
        viewModelScope.launch {
            _actions.emit(WebViewAction.Stop(tabId))
        }
    }

    fun goHome() {
        val currentTab = activeTab.value ?: return
        updateTab(currentTab.id) {
            it.copy(
                url = "",
                title = "新しいタブ",
                progress = 0,
                isLoading = false,
                canGoBack = false,
                canGoForward = false,
                threatAlert = null
            )
        }
    }

    // Modes & Toggles
    fun toggleDesktopMode() {
        val currentTab = activeTab.value ?: return
        val newMode = !currentTab.isDesktopMode
        updateTab(currentTab.id) { it.copy(isDesktopMode = newMode) }
        viewModelScope.launch {
            _actions.emit(WebViewAction.ToggleDesktop(currentTab.id, newMode))
            _toastMessage.emit(if (newMode) "PC版サイトを表示" else "モバイル版サイトを表示")
        }
    }

    fun toggleReaderMode() {
        val currentTab = activeTab.value ?: return
        val newMode = !currentTab.isReaderMode
        updateTab(currentTab.id) { it.copy(isReaderMode = newMode) }
        viewModelScope.launch {
            _actions.emit(WebViewAction.ToggleReader(currentTab.id, newMode))
            _toastMessage.emit(if (newMode) "リーダーモード ON" else "リーダーモード OFF")
        }
    }

    // Find in Page
    fun openFindInPage() {
        _isFindInPageOpen.value = true
        _findQuery.value = ""
        _findMatchCount.value = Pair(0, 0)
    }

    fun closeFindInPage() {
        _isFindInPageOpen.value = false
        _findQuery.value = ""
        _findMatchCount.value = Pair(0, 0)
        val tabId = _activeTabId.value
        viewModelScope.launch {
            _actions.emit(WebViewAction.ClearFind(tabId))
        }
    }

    fun setFindQuery(query: String) {
        _findQuery.value = query
        val tabId = _activeTabId.value
        viewModelScope.launch {
            _actions.emit(WebViewAction.FindQuery(tabId, query))
        }
    }

    fun findNext(forward: Boolean) {
        val tabId = _activeTabId.value
        viewModelScope.launch {
            _actions.emit(WebViewAction.FindNext(tabId, forward))
        }
    }

    fun updateFindResult(activeMatchOrdinal: Int, numberOfMatches: Int) {
        _findMatchCount.value = Pair(
            if (numberOfMatches > 0) activeMatchOrdinal + 1 else 0,
            numberOfMatches
        )
    }

    // Bookmarks & History
    fun toggleBookmarkCurrentPage() {
        val currentTab = activeTab.value ?: return
        if (currentTab.url.isBlank()) return

        viewModelScope.launch {
            val isBookmarked = repository.isBookmarked(currentTab.url)
            if (isBookmarked) {
                repository.removeBookmarkByUrl(currentTab.url)
                _toastMessage.emit("ブックマークを解除しました")
            } else {
                repository.addBookmark(currentTab.title, currentTab.url)
                _toastMessage.emit("ブックマークに追加しました")
            }
        }
    }

    fun removeBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            repository.removeBookmark(bookmark)
            _toastMessage.emit("ブックマークを削除しました")
        }
    }

    fun deleteHistoryItem(item: HistoryEntity) {
        viewModelScope.launch {
            repository.removeHistory(item)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            _toastMessage.emit("すべての履歴を削除しました")
        }
    }

    // AdBlock tracking
    fun recordAdBlocked(tabId: String) {
        val currentMap = _pageBlockedCounts.value.toMutableMap()
        currentMap[tabId] = (currentMap[tabId] ?: 0) + 1
        _pageBlockedCounts.value = currentMap
        _totalBlockedAds.value = _totalBlockedAds.value + 1
    }

    fun resetAdBlockStats() {
        val currentMap = _pageBlockedCounts.value.toMutableMap()
        currentMap.clear()
        _pageBlockedCounts.value = currentMap
        _totalBlockedAds.value = 0L
    }

    // UI Dialog controls
    fun setTabSwitcherOpen(open: Boolean) {
        _isTabSwitcherOpen.value = open
    }

    fun setBookmarksHistoryOpen(open: Boolean) {
        _isBookmarksHistoryOpen.value = open
    }

    fun setSettingsOpen(open: Boolean) {
        _isSettingsOpen.value = open
    }

    // Settings actions with persistent SQLite saving
    fun updateSearchEngine(engine: SearchEngine) {
        _settings.value = _settings.value.copy(searchEngine = engine)
        persistCurrentSettings()
        viewModelScope.launch {
            _toastMessage.emit("検索エンジンを「${engine.displayName}」に変更し保存しました")
        }
    }

    fun updateAdBlockLevel(level: AdBlockLevel) {
        _settings.value = _settings.value.copy(adBlockLevel = level)
        persistCurrentSettings()
        viewModelScope.launch {
            _toastMessage.emit("広告ブロック: ${level.label}（保存完了）")
        }
    }

    fun toggleAdBlockCosmeticFiltering() {
        val newVal = !_settings.value.adBlockCosmeticFiltering
        _settings.value = _settings.value.copy(adBlockCosmeticFiltering = newVal)
        persistCurrentSettings()
        viewModelScope.launch {
            _toastMessage.emit(if (newVal) "広告枠の非表示化: ON" else "広告枠の非表示化: OFF")
        }
    }

    fun toggleJavaScript() {
        val newVal = !_settings.value.javaScriptEnabled
        _settings.value = _settings.value.copy(javaScriptEnabled = newVal)
        persistCurrentSettings()
        val tabId = _activeTabId.value
        viewModelScope.launch {
            _actions.emit(WebViewAction.UpdateJavaScript(tabId, newVal))
            _actions.emit(WebViewAction.Reload(tabId))
            _toastMessage.emit(
                if (newVal) "JavaScriptを有効化（ページを再読み込み）"
                else "JavaScriptを無効化（ページを再読み込み）"
            )
        }
    }

    fun toggleSafeBrowsingGuard() {
        val newVal = !_settings.value.enableSafeBrowsingGuard
        _settings.value = _settings.value.copy(enableSafeBrowsingGuard = newVal)
        persistCurrentSettings()
        viewModelScope.launch {
            _toastMessage.emit(if (newVal) "VirusTotal/セーフガード保護: 有効" else "VirusTotal/セーフガード保護: 無効")
        }
    }

    fun toggleDownloadProtection() {
        val newVal = !_settings.value.enableDownloadProtection
        _settings.value = _settings.value.copy(enableDownloadProtection = newVal)
        persistCurrentSettings()
        viewModelScope.launch {
            _toastMessage.emit(if (newVal) "危険なダウンロード中断保護: 有効" else "危険なダウンロード保護: 無効")
        }
    }

    fun toggleBlockPopups() {
        val newVal = !_settings.value.blockPopups
        _settings.value = _settings.value.copy(blockPopups = newVal)
        persistCurrentSettings()
    }

    fun toggleDoNotTrack() {
        val newVal = !_settings.value.sendDoNotTrack
        _settings.value = _settings.value.copy(sendDoNotTrack = newVal)
        persistCurrentSettings()
        viewModelScope.launch {
            _toastMessage.emit(if (newVal) "Do Not Track（追跡拒否）: 有効" else "Do Not Track: 無効")
        }
    }

    fun toggleBlockThirdPartyCookies() {
        val newVal = !_settings.value.blockThirdPartyCookies
        _settings.value = _settings.value.copy(blockThirdPartyCookies = newVal)
        persistCurrentSettings()
        viewModelScope.launch {
            _toastMessage.emit(if (newVal) "サードパーティCookie拒否: 有効" else "サードパーティCookie拒否: 無効")
        }
    }

    fun toggleWarnInsecure() {
        val newVal = !_settings.value.warnInsecureConnections
        _settings.value = _settings.value.copy(warnInsecureConnections = newVal)
        persistCurrentSettings()
    }

    fun toggleClearOnExit() {
        val newVal = !_settings.value.clearHistoryOnExit
        _settings.value = _settings.value.copy(clearHistoryOnExit = newVal)
        persistCurrentSettings()
    }

    fun toggleLoadImages() {
        val newVal = !_settings.value.loadImagesAutomatically
        _settings.value = _settings.value.copy(loadImagesAutomatically = newVal)
        persistCurrentSettings()
        val tabId = _activeTabId.value
        viewModelScope.launch {
            _actions.emit(WebViewAction.UpdateLoadImages(tabId, newVal))
            _actions.emit(WebViewAction.Reload(tabId))
            _toastMessage.emit(if (newVal) "画像の自動読み込み: ON" else "画像の自動読み込み: OFF (省データ)")
        }
    }

    fun toggleSupportZoom() {
        val newVal = !_settings.value.supportZoom
        _settings.value = _settings.value.copy(supportZoom = newVal)
        persistCurrentSettings()
        viewModelScope.launch {
            _toastMessage.emit(if (newVal) "ズーム操作: 許可" else "ズーム操作: 無効")
        }
    }

    fun updateTextZoom(percent: Int) {
        _settings.value = _settings.value.copy(textZoomPercent = percent)
        persistCurrentSettings()
        val tabId = _activeTabId.value
        viewModelScope.launch {
            _actions.emit(WebViewAction.UpdateTextZoom(tabId, percent))
        }
    }

    fun updateThemeMode(mode: AppThemeMode) {
        _settings.value = _settings.value.copy(themeMode = mode)
        persistCurrentSettings()
        viewModelScope.launch {
            _toastMessage.emit("テーマ設定: ${mode.label}（保存完了）")
        }
    }

    fun toggleForceDarkWeb() {
        val newVal = !_settings.value.forceDarkWeb
        _settings.value = _settings.value.copy(forceDarkWeb = newVal)
        persistCurrentSettings()
        val tabId = _activeTabId.value
        viewModelScope.launch {
            _actions.emit(WebViewAction.Reload(tabId))
            _toastMessage.emit(if (newVal) "夜間ダークWeb表示: ON" else "夜間ダークWeb表示: OFF")
        }
    }

    fun toggleDesktopByDefault() {
        val newVal = !_settings.value.openDesktopByDefault
        _settings.value = _settings.value.copy(openDesktopByDefault = newVal)
        persistCurrentSettings()
        viewModelScope.launch {
            _toastMessage.emit(if (newVal) "常にPC版で開く: ON" else "常にPC版で開く: OFF")
        }
    }

    fun clearBrowsingData(clearHistory: Boolean, clearCache: Boolean, clearCookies: Boolean) {
        viewModelScope.launch {
            if (clearHistory) {
                repository.clearAllHistory()
            }
            if (clearCache || clearCookies) {
                _actions.emit(WebViewAction.ClearWebData(clearCache, clearCookies))
            }
            _toastMessage.emit("指定した閲覧データを消去しました")
        }
    }

    fun resetSettingsToDefault() {
        _settings.value = BrowserSettings()
        persistCurrentSettings()
        viewModelScope.launch {
            _toastMessage.emit("設定を初期状態にリセットしました")
        }
    }

    // WebView Callbacks
    fun onPageStarted(tabId: String, url: String) {
        val currentMap = _pageBlockedCounts.value.toMutableMap()
        currentMap[tabId] = 0
        _pageBlockedCounts.value = currentMap

        // Intercept threat on redirect or URL change
        val isBlocked = checkAndInterceptUrlThreat(tabId, url)
        if (isBlocked) return

        updateTab(tabId) {
            it.copy(url = url, isLoading = true, progress = 10, threatAlert = null)
        }
    }

    fun onPageFinished(tabId: String, url: String) {
        val tab = _tabs.value.find { it.id == tabId }
        updateTab(tabId) {
            it.copy(url = url, isLoading = false, progress = 100)
        }
        if (tab?.isIncognito == false && url.isNotBlank() && !url.startsWith("about:")) {
            viewModelScope.launch {
                repository.addHistory(tab.title, url)
            }
        }
    }

    fun onTitleReceived(tabId: String, title: String) {
        if (title.isNotBlank()) {
            updateTab(tabId) { it.copy(title = title) }
        }
    }

    fun onProgressChanged(tabId: String, progress: Int) {
        updateTab(tabId) {
            it.copy(progress = progress, isLoading = progress < 100)
        }
    }

    fun onCanGoBackForwardChanged(tabId: String, canBack: Boolean, canForward: Boolean) {
        updateTab(tabId) {
            it.copy(canGoBack = canBack, canGoForward = canForward)
        }
    }

    // Google Password Manager & Autofill
    fun onPasswordFieldDetected(tabId: String, hasPassword: Boolean) {
        updateTab(tabId) { it.copy(hasPasswordField = hasPassword) }
    }

    fun triggerAutofill() {
        val tabId = _activeTabId.value
        viewModelScope.launch {
            _actions.emit(WebViewAction.TriggerAutofill(tabId))
            _toastMessage.emit("Google パスワードマネージャー自動入力を要求しました")
        }
    }

    fun openGooglePasswordManager(context: Context) {
        com.example.util.GooglePasswordManagerHelper.openGooglePasswordManager(context) { url ->
            openNewTab(url)
        }
    }

    fun openPasswordCheckup(context: Context) {
        com.example.util.GooglePasswordManagerHelper.openPasswordCheckup(context) { url ->
            openNewTab(url)
        }
    }

    fun openAutofillSettings(context: Context) {
        com.example.util.GooglePasswordManagerHelper.openAndroidAutofillSettings(context)
    }

    fun toggleGooglePasswordManagerAutofill() {
        val newVal = !_settings.value.enableGooglePasswordManagerAutofill
        _settings.value = _settings.value.copy(enableGooglePasswordManagerAutofill = newVal)
        persistCurrentSettings()
        viewModelScope.launch {
            _toastMessage.emit(if (newVal) "Google パスワードマネージャー連携: 有効" else "Google パスワードマネージャー連携: 無効")
        }
    }

    fun toggleShowPasswordManagerQuickBar() {
        val newVal = !_settings.value.showPasswordManagerQuickBar
        _settings.value = _settings.value.copy(showPasswordManagerQuickBar = newVal)
        persistCurrentSettings()
    }

    private fun updateTab(tabId: String, update: (WebTab) -> WebTab) {
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == tabId) update(tab) else tab
        }
        persistCurrentTabs()
    }
}
