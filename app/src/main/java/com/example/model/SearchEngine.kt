package com.example.model

import java.net.URLEncoder

enum class SearchEngine(
    val displayName: String,
    val searchUrlTemplate: String,
    val host: String
) {
    GOOGLE("Google", "https://www.google.com/search?q=%s", "google.com"),
    YAHOO("Yahoo! JAPAN", "https://search.yahoo.co.jp/search?p=%s", "yahoo.co.jp"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=%s", "duckduckgo.com"),
    BING("Bing", "https://www.bing.com/search?q=%s", "bing.com"),
    ECOSIA("Ecosia", "https://www.ecosia.org/search?q=%s", "ecosia.org"),
    BAIDU("Baidu (百度)", "https://www.baidu.com/s?wd=%s", "baidu.com");

    fun buildSearchUrl(query: String): String {
        val encoded = try {
            URLEncoder.encode(query.trim(), "UTF-8")
        } catch (e: Exception) {
            query.trim()
        }
        return String.format(searchUrlTemplate, encoded)
    }
}

enum class AdBlockLevel(val label: String, val description: String) {
    AGGRESSIVE("強力ブロック（推奨）", "Yahoo! JAPAN、国内主要アド網、追跡タグ、広告枠を徹底除去"),
    STANDARD("標準ブロック", "一般的なポップアップや追跡スクリプトをブロック"),
    OFF("オフ", "すべての広告とスクリプトを許可")
}

enum class AppThemeMode(val label: String) {
    SYSTEM("システム設定に従う"),
    DARK("常にダークモード"),
    LIGHT("常にライトモード")
}

data class BrowserSettings(
    // 検索・一般
    val searchEngine: SearchEngine = SearchEngine.GOOGLE,
    val customHomePageUrl: String = "",
    val openDesktopByDefault: Boolean = false,

    // 広告ブロック
    val adBlockLevel: AdBlockLevel = AdBlockLevel.AGGRESSIVE,
    val adBlockCosmeticFiltering: Boolean = true,
    val blockedAdsCountTotal: Long = 0,

    // プライバシー・セキュリティ・認証
    val sendDoNotTrack: Boolean = true,
    val blockThirdPartyCookies: Boolean = true,
    val warnInsecureConnections: Boolean = true,
    val clearHistoryOnExit: Boolean = false,
    val enableSafeBrowsingGuard: Boolean = true,
    val enableDownloadProtection: Boolean = true,
    val enableGooglePasswordManagerAutofill: Boolean = true,
    val showPasswordManagerQuickBar: Boolean = true,

    // Webコンテンツ制御
    val javaScriptEnabled: Boolean = true,
    val blockPopups: Boolean = true,
    val loadImagesAutomatically: Boolean = true,
    val supportZoom: Boolean = true,

    // 外観・表示
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val textZoomPercent: Int = 100,
    val forceDarkWeb: Boolean = false
) {
    val isAdBlockActive: Boolean
        get() = adBlockLevel != AdBlockLevel.OFF
}
