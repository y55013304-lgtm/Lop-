package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.BrowserSettings
import com.example.model.WebTab
import com.example.util.AdBlocker
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.WebViewAction
import kotlinx.coroutines.flow.collectLatest

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

private const val READER_MODE_JS = """
(function() {
    var existing = document.getElementById('surf-reader-style');
    if (existing) {
        existing.remove();
        return;
    }
    var style = document.createElement('style');
    style.id = 'surf-reader-style';
    style.innerHTML = `
        body {
            max-width: 720px !important;
            margin: 0 auto !important;
            padding: 24px 16px !important;
            font-size: 19px !important;
            line-height: 1.8 !important;
            font-family: system-ui, -apple-system, sans-serif !important;
            background-color: #FAF8F5 !important;
            color: #1A1A1A !important;
        }
        nav, header, footer, aside, .ad, .ads, .advertisement, [role="banner"], [role="complementary"] {
            display: none !important;
        }
        img {
            max-width: 100% !important;
            height: auto !important;
            border-radius: 8px !important;
            margin: 16px 0 !important;
        }
    `;
    document.head.appendChild(style);
})();
"""

private const val DARK_WEB_JS = """
(function() {
    var existing = document.getElementById('lop-dark-style');
    if (existing) return;
    var style = document.createElement('style');
    style.id = 'lop-dark-style';
    style.innerHTML = `
        html {
            filter: invert(90%) hue-rotate(180deg) !important;
            background-color: #121212 !important;
        }
        img, video, canvas, svg, picture, [style*="background-image"] {
            filter: invert(100%) hue-rotate(180deg) !important;
        }
    `;
    document.head.appendChild(style);
})();
"""

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    tab: WebTab,
    settings: BrowserSettings,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var customVideoView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // Listen to actions targeted to this tab
    LaunchedEffect(tab.id) {
        viewModel.actions.collectLatest { action ->
            val webView = webViewInstance ?: return@collectLatest
            when (action) {
                is WebViewAction.LoadUrl -> {
                    if (action.tabId == tab.id) {
                        webView.loadUrl(action.url)
                    }
                }
                is WebViewAction.GoBack -> {
                    if (action.tabId == tab.id && webView.canGoBack()) {
                        webView.goBack()
                    }
                }
                is WebViewAction.GoForward -> {
                    if (action.tabId == tab.id && webView.canGoForward()) {
                        webView.goForward()
                    }
                }
                is WebViewAction.Reload -> {
                    if (action.tabId == tab.id) {
                        webView.reload()
                    }
                }
                is WebViewAction.Stop -> {
                    if (action.tabId == tab.id) {
                        webView.stopLoading()
                    }
                }
                is WebViewAction.FindQuery -> {
                    if (action.tabId == tab.id) {
                        if (action.query.isBlank()) {
                            webView.clearMatches()
                        } else {
                            webView.findAllAsync(action.query)
                        }
                    }
                }
                is WebViewAction.FindNext -> {
                    if (action.tabId == tab.id) {
                        webView.findNext(action.forward)
                    }
                }
                is WebViewAction.ClearFind -> {
                    if (action.tabId == tab.id) {
                        webView.clearMatches()
                    }
                }
                is WebViewAction.ToggleDesktop -> {
                    if (action.tabId == tab.id) {
                        webView.settings.userAgentString = if (action.enabled) DESKTOP_USER_AGENT else null
                        webView.reload()
                    }
                }
                is WebViewAction.ToggleReader -> {
                    if (action.tabId == tab.id) {
                        webView.evaluateJavascript(READER_MODE_JS, null)
                    }
                }
                is WebViewAction.EvaluateJavascript -> {
                    if (action.tabId == tab.id) {
                        webView.evaluateJavascript(action.script, null)
                    }
                }
                is WebViewAction.UpdateJavaScript -> {
                    if (action.tabId == tab.id) {
                        webView.settings.javaScriptEnabled = action.enabled
                    }
                }
                is WebViewAction.UpdateTextZoom -> {
                    if (action.tabId == tab.id) {
                        webView.settings.textZoom = action.textZoom
                    }
                }
                is WebViewAction.UpdateLoadImages -> {
                    if (action.tabId == tab.id) {
                        webView.settings.loadsImagesAutomatically = action.loadImages
                    }
                }
                is WebViewAction.ClearWebData -> {
                    if (action.clearCache) {
                        webView.clearCache(true)
                    }
                    if (action.clearCookies) {
                        CookieManager.getInstance().removeAllCookies(null)
                        CookieManager.getInstance().flush()
                    }
                }
                is WebViewAction.TriggerAutofill -> {
                    if (action.tabId == tab.id) {
                        com.example.util.GooglePasswordManagerHelper.requestAutofill(context, webView)
                    }
                }
            }
        }
    }

    // Fullscreen video layer or standard webview
    Box(modifier = modifier.fillMaxSize()) {
        if (customVideoView != null) {
            AndroidView(
                factory = { customVideoView!! },
                modifier = Modifier.fillMaxSize()
            )
        }

        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Configure WebSettings
                    with(this.settings) {
                        javaScriptEnabled = settings.javaScriptEnabled
                        domStorageEnabled = true
                        databaseEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        setSupportZoom(settings.supportZoom)
                        loadsImagesAutomatically = settings.loadImagesAutomatically
                        textZoom = settings.textZoomPercent
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        allowFileAccess = false
                        allowContentAccess = false
                        if (tab.isDesktopMode || settings.openDesktopByDefault) {
                            userAgentString = DESKTOP_USER_AGENT
                        }
                    }

                    // Setup Android Autofill & Google Password Manager
                    com.example.util.GooglePasswordManagerHelper.setupWebViewAutofill(this)

                    // Cookies
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, !settings.blockThirdPartyCookies)

                    // Setup Download Listener with VirusTotal threat inspection
                    setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                        viewModel.onDownloadRequested(
                            url = url,
                            userAgent = userAgent,
                            contentDisposition = contentDisposition,
                            mimeType = mimetype,
                            contentLength = contentLength
                        )
                    }

                    // Setup Find in page listener
                    setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
                        viewModel.updateFindResult(activeMatchOrdinal, numberOfMatches)
                    }

                    // Setup WebChromeClient
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            viewModel.onProgressChanged(tab.id, newProgress)

                            // Early cosmetic injection as page progresses
                            if (newProgress > 30 && settings.isAdBlockActive && settings.adBlockCosmeticFiltering) {
                                view?.evaluateJavascript(AdBlocker.getCosmeticHidingScript(), null)
                            }
                        }

                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title ?: "")
                            if (!title.isNullOrBlank()) {
                                viewModel.onTitleReceived(tab.id, title)
                            }
                        }

                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            customVideoView = view
                            customViewCallback = callback
                        }

                        override fun onHideCustomView() {
                            customViewCallback?.onCustomViewHidden()
                            customVideoView = null
                            customViewCallback = null
                        }
                    }

                    // Setup WebViewClient
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            url?.let {
                                if (viewModel.checkAndInterceptUrlThreat(tab.id, it)) {
                                    view?.stopLoading()
                                    return
                                }
                                viewModel.onPageStarted(tab.id, it)
                                viewModel.onCanGoBackForwardChanged(
                                    tab.id,
                                    canGoBack(),
                                    canGoForward()
                                )
                            }
                            if (settings.isAdBlockActive && settings.adBlockCosmeticFiltering) {
                                view?.evaluateJavascript(AdBlocker.getCosmeticHidingScript(), null)
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            url?.let {
                                viewModel.onPageFinished(tab.id, it)
                                viewModel.onCanGoBackForwardChanged(
                                    tab.id,
                                    canGoBack(),
                                    canGoForward()
                                )
                            }
                            // Inject Yahoo! JAPAN & cosmetic ad blocking CSS
                            if (settings.isAdBlockActive && settings.adBlockCosmeticFiltering) {
                                view?.evaluateJavascript(AdBlocker.getCosmeticHidingScript(), null)
                            }
                            // Force Dark web pages if enabled
                            if (settings.forceDarkWeb) {
                                view?.evaluateJavascript(DARK_WEB_JS, null)
                            }
                            // Optimize login forms for Google Password Manager & detect password fields
                            if (settings.enableGooglePasswordManagerAutofill) {
                                view?.evaluateJavascript(com.example.util.GooglePasswordManagerHelper.AUTOFILL_ENHANCER_JS, null)
                                view?.evaluateJavascript("document.querySelectorAll('input[type=\"password\"]').length > 0") { result ->
                                    val hasPwd = result?.trim() == "true"
                                    viewModel.onPasswordFieldDetected(tab.id, hasPwd)
                                }
                            }
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            if (settings.isAdBlockActive && request != null) {
                                val reqUrl = request.url.toString()
                                if (AdBlocker.isAd(reqUrl, settings.adBlockLevel)) {
                                    viewModel.recordAdBlocked(tab.id)
                                    return AdBlocker.createEmptyResponse()
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val uri = request?.url ?: return false
                            val scheme = uri.scheme?.lowercase() ?: return false
                            val urlStr = uri.toString()

                            // Check and intercept malicious URL
                            if (viewModel.checkAndInterceptUrlThreat(tab.id, urlStr)) {
                                view?.stopLoading()
                                return true
                            }

                            // Handle special schemes like tel:, mailto:, sms:, geo:
                            if (scheme != "http" && scheme != "https" && scheme != "about" && scheme != "data") {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                    return true
                                } catch (e: Exception) {
                                    return true
                                }
                            }
                            return false
                        }
                    }

                    // Load initial URL if present
                    if (tab.url.isNotBlank() && !tab.isHomePage) {
                        loadUrl(tab.url)
                    }

                    webViewInstance = this
                }
            },
            update = { webView ->
                // Keep settings in sync
                webView.settings.javaScriptEnabled = settings.javaScriptEnabled
                webView.settings.loadsImagesAutomatically = settings.loadImagesAutomatically
                webView.settings.textZoom = settings.textZoomPercent
                webView.settings.setSupportZoom(settings.supportZoom)
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    DisposableEffect(tab.id) {
        onDispose {
            webViewInstance?.stopLoading()
            webViewInstance?.destroy()
            webViewInstance = null
        }
    }
}
