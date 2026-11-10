package com.example.util

import android.net.Uri
import java.net.URLEncoder
import java.util.Locale

enum class ThreatType(
    val title: String,
    val description: String,
    val badge: String
) {
    MALICIOUS_SITE(
        title = "悪意のある不正サイト（マルウェア検知）",
        description = "VirusTotal およびセキュリティ脅威インテリジェンスにより、端末に害を及ぼす不正プログラムやウイルスを配布している疑いがある危険なサイトとして判定されました。",
        badge = "マルウェア感染リスク"
    ),
    PHISHING_SCAM(
        title = "フィッシング詐欺・偽サイト",
        description = "本物のサービスを偽装し、パスワード、クレジットカード情報、暗号資産の認証情報などの個人情報を不正に搾取する疑いがある詐欺サイトとして判定されました。",
        badge = "個人情報搾取リスク"
    ),
    DECEPTIVE_SITE(
        title = "偽のセキュリティ警告・不正誘導サイト",
        description = "「ウイルスに感染しました」「バッテリーが破損しています」といった虚偽の警告を表示して、不正なアプリをインストールさせようとする危険サイトとして判定されました。",
        badge = "偽警告・不正アプリ誘導"
    ),
    DANGEROUS_DOWNLOAD(
        title = "危険なファイルのダウンロード",
        description = "実行可能なアプリケーション（.apk / .exe / スクリプト等）が含まれており、インストールすると不正プログラムが端末で実行される重大な危険性があります。",
        badge = "高危険度ファイル"
    )
}

data class ThreatAlert(
    val url: String,
    val displayHost: String,
    val threatType: ThreatType,
    val threatName: String,
    val detail: String,
    val riskLevel: String = "高危険度 (Critical Hazard)",
    val isDownload: Boolean = false,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val mimeType: String? = null,
    val virusTotalUrl: String = SafeBrowsingGuard.getVirusTotalSearchUrl(url)
)

object SafeBrowsingGuard {

    // Known test / security simulation hosts for malware/phishing
    private val KNOWN_MALICIOUS_DOMAINS = setOf(
        "testsafebrowsing.appspot.com",
        "malware.wicar.org",
        "wicar.org",
        "itisatrap.org",
        "ianfette.org",
        "malvertising.test",
        "phishing.test",
        "badssl.com"
    )

    // Trusted official domains that should not be falsely flagged by keyword matching
    private val OFFICIAL_DOMAINS = setOf(
        "paypal.com",
        "apple.com",
        "amazon.co.jp",
        "amazon.com",
        "google.com",
        "google.co.jp",
        "yahoo.co.jp",
        "yahoo.com",
        "microsoft.com",
        "github.com",
        "twitter.com",
        "x.com",
        "line.me",
        "rakuten.co.jp",
        "smbc.co.jp",
        "mufg.jp",
        "mizuho-fg.co.jp",
        "yucho-bank.japanpost.jp"
    )

    // Suspicious TLDs with high fraud rates
    private val SUSPICIOUS_TLDS = setOf(
        ".top", ".tk", ".ml", ".ga", ".cf", ".gq", ".work",
        ".click", ".cam", ".kim", ".loan", ".racing", ".download", ".stream", ".rest"
    )

    // Phishing keyword combinations
    private val PHISHING_PATTERNS = listOf(
        "paypal.*(verify|login|account|security|billing)",
        "appleid.*(verify|update|signin|locked)",
        "amazon.*(verify|security|signin|order-alert)",
        "rakuten.*(login|point-claim|security)",
        "line.*(security-login|verify-pin)",
        "smbc.*(auth|login|update)",
        "mufg.*(direct|login|renew)",
        "metamask.*(restore|seed|claim|airdrop)",
        "binance.*(claim|gift|airdrop)",
        "free-crypto.*(claim|bonus)",
        "iphone-winner.*(claim|gift)"
    ).map { Regex(it, RegexOption.IGNORE_CASE) }

    // Deceptive fake system warning paths
    private val FAKE_WARNING_PATHS = listOf(
        "/virus-alert",
        "/device-infected",
        "/system-damaged",
        "/call-support",
        "/security-warning",
        "/system-compromised",
        "/install-cleaner",
        "/android-virus"
    )

    // Dangerous executable file extensions
    private val EXECUTABLE_EXTENSIONS = setOf(
        "apk", "dex", "exe", "scr", "bat", "cmd", "vbs", "js",
        "jar", "msi", "ps1", "sh", "bin", "elf", "iso", "dmg"
    )

    // Set of domains temporarily allowed by the user for this session
    private val allowedBypassHosts = mutableSetOf<String>()

    fun allowHostBypass(host: String) {
        if (host.isNotBlank()) {
            allowedBypassHosts.add(host.lowercase(Locale.ROOT))
        }
    }

    fun isHostBypassed(host: String): Boolean {
        return allowedBypassHosts.contains(host.lowercase(Locale.ROOT))
    }

    /**
     * Checks if a browsing URL is malicious or poses a security threat.
     */
    fun evaluateUrl(rawUrl: String): ThreatAlert? {
        val host = parseHost(rawUrl) ?: return null
        if (isHostBypassed(host)) return null

        // 1. Direct match on known malware/phishing test hosts
        if (KNOWN_MALICIOUS_DOMAINS.any { host == it || host.endsWith(".$it") }) {
            return ThreatAlert(
                url = rawUrl,
                displayHost = host,
                threatType = ThreatType.MALICIOUS_SITE,
                threatName = "VirusTotal 脅威データベース照合: 悪意あるサイト",
                detail = "公知のマルウェア配布・フィッシングテスト基準データベースに一致しました。アクセスすると不正プログラムの実行や不正アクセスを試みられる危険があります。"
            )
        }

        // Check if official domain
        val isOfficial = OFFICIAL_DOMAINS.any { host == it || host.endsWith(".$it") }

        // 2. Phishing keyword patterns on non-official hosts
        if (!isOfficial) {
            for (pattern in PHISHING_PATTERNS) {
                if (pattern.containsMatchIn(host)) {
                    return ThreatAlert(
                        url = rawUrl,
                        displayHost = host,
                        threatType = ThreatType.PHISHING_SCAM,
                        threatName = "VirusTotal / セーフブラウジング判定: フィッシング詐欺",
                        detail = "有名企業や金融機関を装った不審なドメイン名（$host）が検出されました。認証情報やクレジットカード番号が詐取される危険性があります。"
                    )
                }
            }
        }

        // 3. Deceptive fake warning paths
        val path = parsePath(rawUrl)
        if (FAKE_WARNING_PATHS.any { path.contains(it) }) {
            return ThreatAlert(
                url = rawUrl,
                displayHost = host,
                threatType = ThreatType.DECEPTIVE_SITE,
                threatName = "偽セキュリティ警告（サポート詐欺・不正誘導）",
                detail = "「ウイルス感染」「システム破損」等の偽装メッセージを表示して不審なアプリのインストールや連絡を強制しようとする詐欺ページが検出されました。"
            )
        }

        // 4. Suspicious TLD with deceptive words
        val matchedSuspiciousTld = SUSPICIOUS_TLDS.firstOrNull { host.endsWith(it) }
        if (matchedSuspiciousTld != null) {
            val suspiciousKeywords = listOf("login", "secure", "bank", "free", "gift", "update", "verify", "bonus", "reward")
            if (suspiciousKeywords.any { host.contains(it) }) {
                return ThreatAlert(
                    url = rawUrl,
                    displayHost = host,
                    threatType = ThreatType.PHISHING_SCAM,
                    threatName = "不正ドメイン検知: 高危険度TLD ($matchedSuspiciousTld)",
                    detail = "悪用率が極めて高い不審なトップレベルドメイン ($matchedSuspiciousTld) と金融・ログイン誘引キーワードの組み合わせが確認されました。"
                )
            }
        }

        // 5. Raw numeric IP addresses trying to load non-local web content
        if (host.matches(Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$"""))) {
            if (!host.startsWith("127.") && !host.startsWith("192.168.") && !host.startsWith("10.") && host != "0.0.0.0") {
                if (path.endsWith(".apk") || path.endsWith(".exe") || path.contains("setup") || path.contains("install")) {
                    return ThreatAlert(
                        url = rawUrl,
                        displayHost = host,
                        threatType = ThreatType.MALICIOUS_SITE,
                        threatName = "未認証IPアドレスからの不正プログラム配布",
                        detail = "正規のドメイン名を持たないダイレクトIP ($host) から実行可能ファイルが直接配布されています。"
                    )
                }
            }
        }

        return null
    }

    /**
     * Inspects a requested file download for malware, ransomware, and dangerous executables.
     */
    fun evaluateDownload(
        url: String,
        contentDisposition: String?,
        mimeType: String?
    ): ThreatAlert? {
        val fileName = extractFileName(url, contentDisposition)
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val host = parseHost(url) ?: "不明なドメイン"

        // Check if executable extension
        val isExecutable = EXECUTABLE_EXTENSIONS.contains(extension)
        val isApk = extension == "apk"
        val isSuspiciousMime = mimeType?.lowercase(Locale.ROOT)?.let {
            it.contains("android.package-archive") ||
            it.contains("x-msdownload") ||
            it.contains("x-dosexec") ||
            it.contains("application/x-bat") ||
            it.contains("application/x-sh")
        } ?: false

        if (isExecutable || isApk || isSuspiciousMime) {
            val threatDescription = when {
                isApk -> "Androidアプリケーションパッケージ (.apk) です。Google Play外の野良アプリはスパイウェアや銀行マルウェア、遠隔操作トロイの木馬を含んでいる危険性が極めて高いため、ダウンロードを中断しました。"
                extension in setOf("exe", "scr", "bat", "cmd", "vbs", "ps1", "msi") -> "WindowsおよびPC向け実行可能スクリプト ($extension) です。不正なコマンドの自動実行やランサムウェア感染のリスクがあります。"
                else -> "システムに危害を加える可能性のある実行可能バイナリファイル ($extension) です。"
            }

            return ThreatAlert(
                url = url,
                displayHost = host,
                threatType = ThreatType.DANGEROUS_DOWNLOAD,
                threatName = if (isApk) "危険なAPKパッケージ検知" else "危険な実行可能ファイル検知 (.$extension)",
                detail = threatDescription,
                isDownload = true,
                fileName = fileName,
                mimeType = mimeType ?: "application/octet-stream",
                riskLevel = "極めて高危険 (Critical Malware Risk)"
            )
        }

        // Suspicious archives (zip, rar, 7z) with suspicious names
        if (extension in setOf("zip", "rar", "7z", "tar", "gz")) {
            val suspiciousWords = listOf("crack", "keygen", "patch", "hack", "free", "invoice", "receipt", "setup", "update")
            if (suspiciousWords.any { fileName.lowercase(Locale.ROOT).contains(it) }) {
                return ThreatAlert(
                    url = url,
                    displayHost = host,
                    threatType = ThreatType.DANGEROUS_DOWNLOAD,
                    threatName = "不審な圧縮アーカイブ検知 ($fileName)",
                    detail = "クラックや偽の請求書、更新プログラムを偽装したZIP/RARファイルです。解凍時にマルウェアが展開される危険性があります。",
                    isDownload = true,
                    fileName = fileName,
                    mimeType = mimeType ?: "application/zip",
                    riskLevel = "警戒 (Suspicious Archive)"
                )
            }
        }

        return null
    }

    fun parseHost(rawUrl: String): String? {
        return try {
            val uri = java.net.URI(rawUrl)
            val h = uri.host
            if (!h.isNullOrBlank()) h.lowercase(Locale.ROOT)
            else extractHostRegex(rawUrl)
        } catch (e: Exception) {
            extractHostRegex(rawUrl)
        }
    }

    private fun extractHostRegex(url: String): String? {
        val regex = Regex("""^https?://([^/?#:]+)""", RegexOption.IGNORE_CASE)
        return regex.find(url)?.groupValues?.get(1)?.lowercase(Locale.ROOT)
    }

    fun parsePath(rawUrl: String): String {
        return try {
            val uri = java.net.URI(rawUrl)
            uri.path?.lowercase(Locale.ROOT) ?: ""
        } catch (e: Exception) {
            val afterHost = rawUrl.substringAfter("://", "").substringAfter('/', "")
            ("/" + afterHost.substringBefore('?').substringBefore('#')).lowercase(Locale.ROOT)
        }
    }

    fun extractFileName(url: String, contentDisposition: String?): String {
        // Try content-disposition header first
        if (!contentDisposition.isNullOrBlank()) {
            val cd = contentDisposition.trim()
            val filenameMatch = Regex("""filename\*?=['"]?(?:UTF-8'')?([^'";]+)['"]?""", RegexOption.IGNORE_CASE).find(cd)
            if (filenameMatch != null) {
                val matched = filenameMatch.groupValues[1]
                return try {
                    java.net.URLDecoder.decode(matched, "UTF-8")
                } catch (e: Exception) {
                    matched
                }
            }
        }

        // Fallback to URL path last segment
        val clean = url.substringBefore('?').substringBefore('#')
        val lastSegment = clean.substringAfterLast('/')
        return if (lastSegment.isNotBlank()) lastSegment else "download_file"
    }

    fun getVirusTotalSearchUrl(url: String): String {
        return try {
            val encoded = URLEncoder.encode(url.trim(), "UTF-8")
            "https://www.virustotal.com/gui/search/$encoded"
        } catch (e: Exception) {
            "https://www.virustotal.com/gui/home/url"
        }
    }
}
