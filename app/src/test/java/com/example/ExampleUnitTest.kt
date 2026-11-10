package com.example

import com.example.model.SearchEngine
import com.example.util.AdBlocker
import com.example.util.SafeBrowsingGuard
import com.example.util.ThreatType
import com.example.util.UrlUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testAdBlocker() {
        assertTrue(AdBlocker.isAd("https://googleads.g.doubleclick.net/pagead/ads?client=ca-pub-123"))
        assertTrue(AdBlocker.isAd("https://pagead2.googlesyndication.com/activeview"))
        assertTrue(AdBlocker.isAd("https://yads.yahoo.co.jp/tag/yads.js"))
        assertTrue(AdBlocker.isAd("https://b92.yahoo.co.jp/js/s_code.js"))
        assertTrue(AdBlocker.isAd("https://yjtag.yahoo.co.jp/tag.js"))
        assertTrue(AdBlocker.isAd("https://ai.yimg.jp/bdv/yahoo/ad/ad.js"))
        assertTrue(AdBlocker.isAd("https://spdeliver.i-mobile.co.jp/script/ads.js"))
        assertFalse(AdBlocker.isAd("https://www.google.com/search?q=android"))
        assertFalse(AdBlocker.isAd("https://github.com/torvalds/linux"))
        assertFalse(AdBlocker.isAd("https://www.yahoo.co.jp/"))
    }

    @Test
    fun testUrlUtils() {
        val googleSearch = UrlUtils.parseInput("android compose", SearchEngine.GOOGLE)
        assertTrue(googleSearch.contains("google.com/search?q=android+compose") || googleSearch.contains("google.com/search?q=android%20compose"))

        val urlWithScheme = UrlUtils.parseInput("https://example.com/test", SearchEngine.GOOGLE)
        assertEquals("https://example.com/test", urlWithScheme)
    }

    @Test
    fun testSafeBrowsingThreatDetection() {
        // Known malware test host
        val malwareAlert = SafeBrowsingGuard.evaluateUrl("https://testsafebrowsing.appspot.com/s/malware.html")
        assertNotNull(malwareAlert)
        assertEquals(ThreatType.MALICIOUS_SITE, malwareAlert?.threatType)

        // Phishing pattern on suspicious domain
        val phishingAlert = SafeBrowsingGuard.evaluateUrl("https://paypal-verify-account-security.top/login")
        assertNotNull(phishingAlert)
        assertEquals(ThreatType.PHISHING_SCAM, phishingAlert?.threatType)

        // Deceptive fake system warning
        val deceptiveAlert = SafeBrowsingGuard.evaluateUrl("https://random-domain.com/device-infected/warning")
        assertNotNull(deceptiveAlert)
        assertEquals(ThreatType.DECEPTIVE_SITE, deceptiveAlert?.threatType)

        // Safe official site
        val safeSite = SafeBrowsingGuard.evaluateUrl("https://www.google.com")
        assertNull(safeSite)

        val safeYahoo = SafeBrowsingGuard.evaluateUrl("https://www.yahoo.co.jp")
        assertNull(safeYahoo)
    }

    @Test
    fun testDangerousDownloadDetection() {
        // APK package download
        val apkThreat = SafeBrowsingGuard.evaluateDownload(
            url = "https://untrusted-site.com/downloads/trojan.apk",
            contentDisposition = "attachment; filename=\"trojan.apk\"",
            mimeType = "application/vnd.android.package-archive"
        )
        assertNotNull(apkThreat)
        assertEquals(ThreatType.DANGEROUS_DOWNLOAD, apkThreat?.threatType)
        assertEquals("trojan.apk", apkThreat?.fileName)

        // Windows executable script
        val exeThreat = SafeBrowsingGuard.evaluateDownload(
            url = "https://suspicious-files.net/payload.exe",
            contentDisposition = null,
            mimeType = "application/x-msdownload"
        )
        assertNotNull(exeThreat)
        assertEquals("payload.exe", exeThreat?.fileName)

        // Safe PDF file
        val safePdf = SafeBrowsingGuard.evaluateDownload(
            url = "https://example.com/documents/report.pdf",
            contentDisposition = "attachment; filename=\"report.pdf\"",
            mimeType = "application/pdf"
        )
        assertNull(safePdf)

        // VirusTotal query URL generator
        val vtUrl = SafeBrowsingGuard.getVirusTotalSearchUrl("https://testsafebrowsing.appspot.com")
        assertTrue(vtUrl.startsWith("https://www.virustotal.com/gui/search/"))
    }

    @Test
    fun testTabEntityMapping() {
        val tab = com.example.model.WebTab(
            url = "https://www.yahoo.co.jp",
            title = "Yahoo! JAPAN",
            isDesktopMode = true
        )
        val entity = com.example.data.TabEntity(
            id = tab.id,
            url = tab.url,
            title = tab.title,
            isDesktopMode = tab.isDesktopMode,
            positionIndex = 0,
            isActive = true
        )
        assertEquals(tab.id, entity.id)
        assertEquals("https://www.yahoo.co.jp", entity.url)
        assertEquals("Yahoo! JAPAN", entity.title)
        assertTrue(entity.isDesktopMode)
        assertTrue(entity.isActive)
    }
}
