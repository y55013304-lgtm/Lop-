package com.example.util

import android.util.Patterns
import com.example.model.SearchEngine

object UrlUtils {
    fun parseInput(input: String, searchEngine: SearchEngine): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""

        if (trimmed.equals("about:blank", ignoreCase = true)) {
            return trimmed
        }

        // Check if starts with a scheme
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("file://", ignoreCase = true)
        ) {
            return trimmed
        }

        // Check if user entered domain like "google.com" or "localhost:8080"
        val hasWhitespace = trimmed.any { it.isWhitespace() }
        val looksLikeDomain = !hasWhitespace && (
            Patterns.WEB_URL.matcher("https://$trimmed").matches() ||
            trimmed.contains(".") ||
            trimmed.startsWith("localhost", ignoreCase = true)
        )

        return if (looksLikeDomain) {
            "https://$trimmed"
        } else {
            searchEngine.buildSearchUrl(trimmed)
        }
    }

    fun getDomain(url: String): String {
        return try {
            val uri = android.net.Uri.parse(url)
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    }
}
