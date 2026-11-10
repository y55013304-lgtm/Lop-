package com.example.util

import android.webkit.WebResourceResponse
import com.example.model.AdBlockLevel
import java.io.ByteArrayInputStream
import java.net.URI

object AdBlocker {

    // Comprehensive list of ad, tracking, and sponsor hosts
    // Special extensive coverage for Yahoo! JAPAN and Japanese domestic ad networks
    private val BLOCKED_HOSTS = hashSetOf(
        // === Yahoo! JAPAN Ad & Tracking Network ===
        "yads.yahoo.co.jp",
        "yjtag.yahoo.co.jp",
        "b92.yahoo.co.jp",
        "b97.yahoo.co.jp",
        "b98.yahoo.co.jp",
        "ad.yahoo.co.jp",
        "ads.yahoo.com",
        "yads-v2.c.yimg.jp",
        "ai.yimg.jp",
        "ard.yahoo.co.jp",
        "listing.yahoo.co.jp",
        "promotionalads.yahoo.co.jp",
        "clover.yahoo.co.jp",
        "yads.c.yimg.jp",
        "s.yimg.jp/images/listing",

        // === Japanese Domestic Ad Networks ===
        "microad.jp",
        "microad.net",
        "compass-fit.jp",
        "cir.io",
        "send.microad.jp",
        "i-mobile.co.jp",
        "spdeliver.i-mobile.co.jp",
        "im-apps.net",
        "ad.i-mobile.co.jp",
        "geniee.jp",
        "gssprt.jp",
        "gsspat.jp",
        "genieesspv.jp",
        "fluct.jp",
        "adingo.jp",
        "cc.adingo.jp",
        "ad-stir.com",
        "adstir.com",
        "amoad.com",
        "ad.amoad.com",
        "zucks.net",
        "sh.zucks.net",
        "j.zucks.net",
        "fout.jp",
        "freakout.net",
        "js.fout.jp",
        "ad.line.me",
        "fivecdm.com",
        "d.line-apps.com",
        "popin.cc",
        "api.popin.cc",
        "logly.co.jp",
        "uzou.jp",
        "socdm.com",
        "impact-ad.jp",
        "scaleout.jp",
        "ad-generation.jp",
        "speee-ad.jp",
        "ad.speee-ad.jp",
        "ad.rakuten.co.jp",
        "ca-adv.jp",
        "medibaad.com",
        "mediba.jp",
        "bypass.jp",
        "ladsp.com",

        // === Global Major Ad & Tracker Networks ===
        "doubleclick.net",
        "googleads.g.doubleclick.net",
        "pagead2.googlesyndication.com",
        "adservice.google.com",
        "googletagservices.com",
        "googleadservices.com",
        "googlesyndication.com",
        "tpc.googlesyndication.com",
        "google-analytics.com",
        "analytics.google.com",
        "amazon-adsystem.com",
        "aax.amazon-adsystem.com",
        "c.amazon-adsystem.com",
        "fls-fe.amazon-adsystem.com",
        "criteo.com",
        "criteo.net",
        "static.criteo.net",
        "bidder.criteo.com",
        "taboola.com",
        "taboolasyndication.com",
        "trc.taboola.com",
        "outbrain.com",
        "outbrainimg.com",
        "widgets.outbrain.com",
        "popads.net",
        "adcolony.com",
        "rubiconproject.com",
        "magnite.com",
        "moatads.com",
        "moat.com",
        "pubmatic.com",
        "ads.pubmatic.com",
        "casalemedia.com",
        "indexexchange.com",
        "adnxs.com",
        "ib.adnxs.com",
        "adtechus.com",
        "smartadserver.com",
        "serving-sys.com",
        "advertising.com",
        "adroll.com",
        "quantserve.com",
        "quantcast.com",
        "ad-delivery.net",
        "inmobi.com",
        "unityads.unity3d.com",
        "applovin.com",
        "admob.com",
        "openx.net",
        "openx.com",
        "scorecardresearch.com",
        "rtbhouse.com",
        "rtbhouse.net",
        "teads.tv",
        "media.net",
        "yieldmo.com",
        "sovrn.com"
    )

    // Keywords in URLs that indicate ads or tracking pixels
    private val AD_URL_PATTERNS = listOf(
        "yads.js",
        "/yads/",
        "yjtag.js",
        "yads-v2",
        "/pagead/",
        "googleads",
        "/adsense/",
        "adservice",
        "/doubleclick/",
        "/ad_delivery",
        "/banner_ad",
        "/sponsor_ad",
        "sp_ad.js",
        "ad_tag",
        "popup_ad"
    )

    /**
     * Checks if a request URL matches an ad or tracking network
     */
    fun isAd(url: String, level: AdBlockLevel = AdBlockLevel.AGGRESSIVE): Boolean {
        if (level == AdBlockLevel.OFF) return false

        val lowerUrl = url.lowercase()

        // Fast keyword check for Yahoo! JAPAN and ad scripts
        for (pattern in AD_URL_PATTERNS) {
            if (lowerUrl.contains(pattern)) return true
        }

        // Host check
        return try {
            val uri = URI(url)
            val host = uri.host?.lowercase() ?: return false
            BLOCKED_HOSTS.any { blocked ->
                host == blocked || host.endsWith(".$blocked") || lowerUrl.contains("/$blocked")
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Returns an empty WebResourceResponse to cancel loading the ad resource
     */
    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "utf-8",
            ByteArrayInputStream("".toByteArray())
        )
    }

    /**
     * Generates JavaScript code to cosmetically hide ad containers, sponsor banners,
     * and empty ad frames, particularly on Yahoo! JAPAN and news portals.
     */
    fun getCosmeticHidingScript(): String {
        return """
        (function() {
            var styleId = 'lop-cosmetic-adblock';
            if (document.getElementById(styleId)) return;
            var style = document.createElement('style');
            style.id = styleId;
            style.innerHTML = `
                /* Yahoo! JAPAN Ads, Sponsors and In-feed promos */
                #yads, [id^="yads"], [class*="yads"], [class*="yjSponsor"], [class*="ySponsor"],
                [id*="yjSponsor"], [id*="yads-"], [data-yads], .yads-ad-frame,
                .yj-sponsor, .ySponsor-box, .advertisement-area, .ad-column,
                aside[role="complementary"] iframe[src*="yahoo"],
                div[id*="ad_header"], div[id*="ad_footer"], div[id*="ad_sidebar"],
                .yads_ad, .yads-unit,
                
                /* Google Ads, AdSense, DFP */
                .adsbygoogle, [id^="google_ads_"], [id^="div-gpt-ad"],
                [data-google-query-id], [data-ad-slot], [data-ad-client],
                
                /* General Banner & Sponsored Units */
                .ad-container, .ad_container, .ad-wrapper, .ad_wrapper,
                .ad-banner, .ad_banner, .ad-box, .ad_box, .banner-ad,
                .ad_unit, .ad-unit, .commercial-unit, .sponsored-post,
                .taboola, .outbrain, .criteo-ad, [data-ad],
                
                /* Ad Iframes */
                iframe[src*="yads"], iframe[src*="doubleclick"], iframe[src*="googlesyndication"],
                iframe[src*="amazon-adsystem"], iframe[src*="ad-stir"], iframe[src*="ad.line.me"],
                iframe[src*="i-mobile"], iframe[src*="microad"], iframe[src*="geniee"],
                iframe[id*="google_ads"], iframe[name*="google_ads"] {
                    display: none !important;
                    visibility: hidden !important;
                    height: 0 !important;
                    min-height: 0 !important;
                    max-height: 0 !important;
                    opacity: 0 !important;
                    pointer-events: none !important;
                }
            `;
            (document.head || document.documentElement).appendChild(style);
        })();
        """.trimIndent()
    }
}
