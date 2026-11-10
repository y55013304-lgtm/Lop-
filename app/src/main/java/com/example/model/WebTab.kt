package com.example.model

import java.util.UUID

data class WebTab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "",
    val title: String = "新しいタブ",
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isDesktopMode: Boolean = false,
    val isReaderMode: Boolean = false,
    val isIncognito: Boolean = false,
    val lastAccessed: Long = System.currentTimeMillis(),
    val threatAlert: com.example.util.ThreatAlert? = null,
    val hasPasswordField: Boolean = false
) {
    val isHomePage: Boolean
        get() = url.isBlank() || url == "about:blank"

    val isSecure: Boolean
        get() = url.startsWith("https://", ignoreCase = true)

    val displayHost: String
        get() {
            if (isHomePage) return "ホーム"
            return try {
                val uri = android.net.Uri.parse(url)
                uri.host ?: url
            } catch (e: Exception) {
                url
            }
        }
}
