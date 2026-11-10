package com.example.data

import com.example.model.AdBlockLevel
import com.example.model.AppThemeMode
import com.example.model.BrowserSettings
import com.example.model.SearchEngine
import com.example.model.WebTab
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BrowserRepository(
    private val bookmarkDao: BookmarkDao,
    private val historyDao: HistoryDao,
    private val settingsDao: SettingsDao,
    private val tabDao: TabDao
) {
    val bookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()
    val history: Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    fun recentHistory(limit: Int = 10): Flow<List<HistoryEntity>> = historyDao.getRecentHistory(limit)

    // Persistent Settings
    val savedSettings: Flow<BrowserSettings?> = settingsDao.getSettings().map { entity ->
        entity?.let {
            BrowserSettings(
                searchEngine = try { SearchEngine.valueOf(it.searchEngineName) } catch (e: Exception) { SearchEngine.GOOGLE },
                themeMode = try { AppThemeMode.valueOf(it.themeMode) } catch (e: Exception) { AppThemeMode.SYSTEM },
                adBlockLevel = try { AdBlockLevel.valueOf(it.adBlockLevel) } catch (e: Exception) { AdBlockLevel.AGGRESSIVE },
                adBlockCosmeticFiltering = it.adBlockCosmetic,
                javaScriptEnabled = it.javaScriptEnabled,
                blockPopups = it.blockPopups,
                sendDoNotTrack = it.sendDoNotTrack,
                blockThirdPartyCookies = it.blockThirdPartyCookies,
                warnInsecureConnections = it.warnInsecure,
                clearHistoryOnExit = it.clearOnExit,
                enableSafeBrowsingGuard = it.enableSafeBrowsingGuard,
                enableDownloadProtection = it.enableDownloadProtection,
                enableGooglePasswordManagerAutofill = it.enableGooglePasswordManagerAutofill,
                showPasswordManagerQuickBar = it.showPasswordManagerQuickBar,
                loadImagesAutomatically = it.loadImages,
                supportZoom = it.supportZoom,
                textZoomPercent = it.textZoom,
                forceDarkWeb = it.forceDarkWeb,
                openDesktopByDefault = it.openDesktopByDefault
            )
        }
    }

    suspend fun persistSettings(settings: BrowserSettings) {
        settingsDao.saveSettings(
            SettingsEntity(
                id = 1,
                searchEngineName = settings.searchEngine.name,
                themeMode = settings.themeMode.name,
                adBlockLevel = settings.adBlockLevel.name,
                adBlockCosmetic = settings.adBlockCosmeticFiltering,
                javaScriptEnabled = settings.javaScriptEnabled,
                blockPopups = settings.blockPopups,
                sendDoNotTrack = settings.sendDoNotTrack,
                blockThirdPartyCookies = settings.blockThirdPartyCookies,
                warnInsecure = settings.warnInsecureConnections,
                clearOnExit = settings.clearHistoryOnExit,
                enableSafeBrowsingGuard = settings.enableSafeBrowsingGuard,
                enableDownloadProtection = settings.enableDownloadProtection,
                enableGooglePasswordManagerAutofill = settings.enableGooglePasswordManagerAutofill,
                showPasswordManagerQuickBar = settings.showPasswordManagerQuickBar,
                loadImages = settings.loadImagesAutomatically,
                supportZoom = settings.supportZoom,
                textZoom = settings.textZoomPercent,
                forceDarkWeb = settings.forceDarkWeb,
                openDesktopByDefault = settings.openDesktopByDefault
            )
        )
    }

    suspend fun isBookmarked(url: String): Boolean {
        return bookmarkDao.getBookmarkByUrl(url) != null
    }

    suspend fun addBookmark(title: String, url: String, faviconUrl: String = "", isPinned: Boolean = false): Long {
        val cleanTitle = title.ifBlank { url }
        return bookmarkDao.insertBookmark(
            BookmarkEntity(
                title = cleanTitle,
                url = url,
                faviconUrl = faviconUrl,
                isPinned = isPinned
            )
        )
    }

    suspend fun removeBookmark(bookmark: BookmarkEntity) {
        bookmarkDao.deleteBookmark(bookmark)
    }

    suspend fun removeBookmarkByUrl(url: String) {
        bookmarkDao.deleteByUrl(url)
    }

    suspend fun addHistory(title: String, url: String): Long {
        if (url.isBlank() || url.startsWith("about:") || url.startsWith("data:")) return -1
        val cleanTitle = title.ifBlank { url }
        return historyDao.insertHistory(
            HistoryEntity(
                title = cleanTitle,
                url = url,
                visitedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeHistory(item: HistoryEntity) {
        historyDao.deleteHistory(item)
    }

    suspend fun clearAllHistory() {
        historyDao.clearAll()
    }

    suspend fun clearHistorySince(sinceTimestamp: Long) {
        historyDao.clearSince(sinceTimestamp)
    }

    // Tab Session Persistence across process lifecycles
    suspend fun saveTabs(tabs: List<WebTab>, activeTabId: String) {
        val nonIncognitoTabs = tabs.filterNot { it.isIncognito }
        if (nonIncognitoTabs.isEmpty()) {
            tabDao.clearAllTabs()
            return
        }
        val entities = nonIncognitoTabs.mapIndexed { index, tab ->
            TabEntity(
                id = tab.id,
                url = tab.url,
                title = tab.title,
                isDesktopMode = tab.isDesktopMode,
                isReaderMode = tab.isReaderMode,
                lastAccessed = tab.lastAccessed,
                positionIndex = index,
                isActive = (tab.id == activeTabId)
            )
        }
        tabDao.replaceAllTabs(entities)
    }

    suspend fun loadSavedTabs(): Pair<List<WebTab>, String?> {
        val saved = tabDao.getAllTabs()
        if (saved.isEmpty()) {
            return Pair(emptyList(), null)
        }
        val activeId = saved.firstOrNull { it.isActive }?.id ?: saved.firstOrNull()?.id
        val webTabs = saved.map { entity ->
            WebTab(
                id = entity.id,
                url = entity.url,
                title = entity.title,
                isDesktopMode = entity.isDesktopMode,
                isReaderMode = entity.isReaderMode,
                lastAccessed = entity.lastAccessed
            )
        }
        return Pair(webTabs, activeId)
    }

    suspend fun populateDefaultsIfEmpty() {
        if (bookmarkDao.count() == 0) {
            val defaults = listOf(
                BookmarkEntity(title = "Google", url = "https://www.google.com", isPinned = true),
                BookmarkEntity(title = "Yahoo! JAPAN", url = "https://www.yahoo.co.jp", isPinned = true),
                BookmarkEntity(title = "YouTube", url = "https://www.youtube.com", isPinned = true),
                BookmarkEntity(title = "Wikipedia", url = "https://ja.wikipedia.org", isPinned = true),
                BookmarkEntity(title = "GitHub", url = "https://github.com", isPinned = true),
                BookmarkEntity(title = "X (Twitter)", url = "https://x.com", isPinned = true),
                BookmarkEntity(title = "Reddit", url = "https://www.reddit.com", isPinned = false),
                BookmarkEntity(title = "NHK News", url = "https://www3.nhk.or.jp/news/", isPinned = false)
            )
            for (bm in defaults) {
                bookmarkDao.insertBookmark(bm)
            }
        }
    }
}
