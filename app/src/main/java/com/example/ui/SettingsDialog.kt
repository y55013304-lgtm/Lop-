package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AdBlockLevel
import com.example.model.AppThemeMode
import com.example.model.BrowserSettings
import com.example.model.SearchEngine

@Composable
fun SettingsDialog(
    settings: BrowserSettings,
    currentTabBlockedCount: Int,
    totalBlockedAds: Long,
    onUpdateSearchEngine: (SearchEngine) -> Unit,
    onUpdateAdBlockLevel: (AdBlockLevel) -> Unit,
    onToggleAdBlockCosmetic: () -> Unit,
    onResetAdBlockStats: () -> Unit,
    onToggleJavaScript: () -> Unit,
    onToggleBlockPopups: () -> Unit,
    onToggleDoNotTrack: () -> Unit,
    onToggleBlockThirdPartyCookies: () -> Unit,
    onToggleWarnInsecure: () -> Unit,
    onToggleClearOnExit: () -> Unit,
    onToggleSafeBrowsingGuard: () -> Unit,
    onToggleDownloadProtection: () -> Unit,
    onOpenGooglePasswordManager: () -> Unit,
    onOpenPasswordCheckup: () -> Unit,
    onOpenAndroidAutofillSettings: () -> Unit,
    onToggleGooglePasswordManagerAutofill: () -> Unit,
    onToggleShowPasswordManagerQuickBar: () -> Unit,
    onToggleLoadImages: () -> Unit,
    onToggleSupportZoom: () -> Unit,
    onUpdateThemeMode: (AppThemeMode) -> Unit,
    onUpdateTextZoom: (Int) -> Unit,
    onToggleForceDarkWeb: () -> Unit,
    onToggleDesktopByDefault: () -> Unit,
    onClearBrowsingData: (history: Boolean, cache: Boolean, cookies: Boolean) -> Unit,
    onResetSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lopブラウザ 詳細設定",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ==================== 1. 検索エンジン ====================
                SectionHeader(title = "検索エンジン", icon = Icons.Default.Search)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        for (engine in SearchEngine.entries) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUpdateSearchEngine(engine) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.searchEngine == engine,
                                    onClick = { onUpdateSearchEngine(engine) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = engine.displayName,
                                        fontSize = 14.sp,
                                        fontWeight = if (settings.searchEngine == engine) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    Text(
                                        text = engine.host,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // ==================== 2. 広告＆トラッカーブロック（強化版） ====================
                SectionHeader(title = "広告・トラッカーブロック (Yahoo! JAPAN・国内対応)", icon = Icons.Default.VpnLock)

                // AdBlock Level Selection
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        for (level in AdBlockLevel.entries) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUpdateAdBlockLevel(level) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.adBlockLevel == level,
                                    onClick = { onUpdateAdBlockLevel(level) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = level.label,
                                        fontSize = 13.sp,
                                        fontWeight = if (settings.adBlockLevel == level) FontWeight.Bold else FontWeight.Normal,
                                        color = if (level == AdBlockLevel.AGGRESSIVE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = level.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Cosmetic Element Hiding Toggle
                SettingsSwitchRow(
                    icon = Icons.Default.VisibilityOff,
                    title = "広告・スポンサー枠の完全消去 (CSS)",
                    description = "Yahoo! JAPANやポータルサイトの空の広告枠・スポンサー枠を非表示化",
                    checked = settings.adBlockCosmeticFiltering,
                    onCheckedChange = { onToggleAdBlockCosmetic() }
                )

                // AdBlock Stats Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "現在のページ: $currentTabBlockedCount 件ブロック",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "累計ブロック数: $totalBlockedAds 件",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        OutlinedButton(
                            onClick = onResetAdBlockStats,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("リセット", fontSize = 11.sp)
                        }
                    }
                }

                HorizontalDivider()

                // ==================== 3. Webスクリプト＆コンテンツ制御 ====================
                SectionHeader(title = "Webスクリプトとコンテンツ制御", icon = Icons.Default.Code)

                // JavaScript Toggle (Explicit Reload Behavior)
                SettingsSwitchRow(
                    icon = Icons.Default.Code,
                    title = "JavaScriptの実行を許可",
                    description = "動的スクリプトを有効化。無効化すると安全性が高まります（切り替え時にページを自動再読み込みします）",
                    checked = settings.javaScriptEnabled,
                    onCheckedChange = { onToggleJavaScript() }
                )

                // Pop-ups
                SettingsSwitchRow(
                    icon = Icons.Default.Block,
                    title = "ポップアップウィンドウのブロック",
                    description = "勝手に開く新しいウィンドウや迷惑なタブを阻止",
                    checked = settings.blockPopups,
                    onCheckedChange = { onToggleBlockPopups() }
                )

                // Load images automatically
                SettingsSwitchRow(
                    icon = Icons.Default.Image,
                    title = "画像の自動読み込み（データ節約）",
                    description = "オフにすると画像を読み込まず通信量を大幅節約（再読み込み適用）",
                    checked = settings.loadImagesAutomatically,
                    onCheckedChange = { onToggleLoadImages() }
                )

                // Pinch zoom
                SettingsSwitchRow(
                    icon = Icons.Default.ZoomIn,
                    title = "ピンチズーム（拡大縮小）の許可",
                    description = "ズーム禁止指定のページでも自由に拡大可能にします",
                    checked = settings.supportZoom,
                    onCheckedChange = { onToggleSupportZoom() }
                )

                HorizontalDivider()

                // ==================== 4. プライバシーとセキュリティ ====================
                SectionHeader(title = "プライバシーとセキュリティ", icon = Icons.Default.Lock)

                SettingsSwitchRow(
                    icon = Icons.Default.Shield,
                    title = "Do Not Track（追跡拒否）ヘッダー",
                    description = "Webサイトに行動追跡の停止を要求する信号を送信",
                    checked = settings.sendDoNotTrack,
                    onCheckedChange = { onToggleDoNotTrack() }
                )

                SettingsSwitchRow(
                    icon = Icons.Default.Cookie,
                    title = "サードパーティCookieの拒否",
                    description = "別サイトからのトラッキングCookieをブロック",
                    checked = settings.blockThirdPartyCookies,
                    onCheckedChange = { onToggleBlockThirdPartyCookies() }
                )

                SettingsSwitchRow(
                    icon = Icons.Default.Shield,
                    title = "VirusTotal ＆ 危険サイト自動遮断",
                    description = "マルウェア配布やフィッシング詐欺の疑いがある危険サイトへのアクセスを自動中断",
                    checked = settings.enableSafeBrowsingGuard,
                    onCheckedChange = { onToggleSafeBrowsingGuard() }
                )

                SettingsSwitchRow(
                    icon = Icons.Default.Block,
                    title = "危険なファイルのダウンロード中断",
                    description = "野良APKや実行可能プログラムなど、悪意ある可能性のあるファイルのダウンロードを遮断",
                    checked = settings.enableDownloadProtection,
                    onCheckedChange = { onToggleDownloadProtection() }
                )

                SettingsSwitchRow(
                    icon = Icons.Default.Lock,
                    title = "非HTTPS接続（保護なし）の警告",
                    description = "通信が暗号化されていないサイトを開く際にアラートを表示",
                    checked = settings.warnInsecureConnections,
                    onCheckedChange = { onToggleWarnInsecure() }
                )

                SettingsSwitchRow(
                    icon = Icons.Default.DeleteSweep,
                    title = "アプリ終了時に履歴を自動消去",
                    description = "ブラウザを閉じたときに訪問履歴を自動削除",
                    checked = settings.clearHistoryOnExit,
                    onCheckedChange = { onToggleClearOnExit() }
                )

                // Granular Data Clearance Button
                Card(
                    onClick = { showClearDataDialog = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("閲覧データを個別に消去...", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text("履歴、キャッシュ、Cookieを選択して削除", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                HorizontalDivider()

                // ==================== 5. パスワードと自動入力（Google パスワードマネージャー） ====================
                SectionHeader(title = "パスワードと自動入力", icon = androidx.compose.material.icons.Icons.Default.Password)

                // Google Password Manager Action Card
                Card(
                    onClick = onOpenGooglePasswordManager,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Google パスワードマネージャーを開く",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "保存済みパスワード、パスキー、自動入力データの確認・編集",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Password Checkup Action Card
                Card(
                    onClick = onOpenPasswordCheckup,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Google パスワード安全診断 (Checkup)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "漏洩した可能性のある危険なパスワードや使い回しを監査",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Android System Autofill Settings Card
                Card(
                    onClick = onOpenAndroidAutofillSettings,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Android システム自動入力設定",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "端末の自動入力プロバイダ（Google）の設定画面を開く",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                SettingsSwitchRow(
                    icon = Icons.Default.Password,
                    title = "ログインフォーム自動入力の最適化",
                    description = "Google パスワードマネージャーが正しく認識できるようHTMLフォームを調整",
                    checked = settings.enableGooglePasswordManagerAutofill,
                    onCheckedChange = { onToggleGooglePasswordManagerAutofill() }
                )

                SettingsSwitchRow(
                    icon = Icons.Default.Key,
                    title = "ログイン画面でのクイックアクセス表示",
                    description = "パスワード入力欄が検出された際に画面上部にワンタップ入力バーを表示",
                    checked = settings.showPasswordManagerQuickBar,
                    onCheckedChange = { onToggleShowPasswordManagerQuickBar() }
                )

                HorizontalDivider()

                // ==================== 6. 外観と表示設定 ====================
                SectionHeader(title = "外観と表示設定", icon = Icons.Default.Palette)

                // Theme Mode
                Text("アプリのテーマ", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        for (mode in AppThemeMode.entries) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUpdateThemeMode(mode) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.themeMode == mode,
                                    onClick = { onUpdateThemeMode(mode) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = mode.label, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Text Zoom Scaling
                Text("テキスト倍率（文字サイズ）", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val zooms = listOf(80, 100, 120, 150)
                    for (zoom in zooms) {
                        OutlinedButton(
                            onClick = { onUpdateTextZoom(zoom) },
                            colors = if (settings.textZoomPercent == zoom) {
                                androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            } else {
                                androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("${zoom}%", fontSize = 12.sp)
                        }
                    }
                }

                // Force Dark Mode on Websites
                SettingsSwitchRow(
                    icon = Icons.Default.Brightness4,
                    title = "夜間ダークWeb表示 (強制ダーク)",
                    description = "白色背景のWebサイトを反転し、暗所での読書負担を軽減",
                    checked = settings.forceDarkWeb,
                    onCheckedChange = { onToggleForceDarkWeb() }
                )

                // Desktop site as default
                SettingsSwitchRow(
                    icon = Icons.Default.DesktopWindows,
                    title = "常にPC版（デスクトップ）で開く",
                    description = "新しいタブを常にPC向けレイアウトで要求",
                    checked = settings.openDesktopByDefault,
                    onCheckedChange = { onToggleDesktopByDefault() }
                )

                HorizontalDivider()

                // ==================== 6. 初期化とアプリ情報 ====================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showResetConfirmDialog = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("すべての設定を初期化", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                        Text("ブラウザ設定を初期値に戻します", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lopブラウザ v1.1 • 高度な広告・Yahoo! JAPANフィルタ搭載",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完了", fontWeight = FontWeight.Bold)
            }
        }
    )

    // Clear Browsing Data Dialog
    if (showClearDataDialog) {
        var clearHistory by remember { mutableStateOf(true) }
        var clearCache by remember { mutableStateOf(true) }
        var clearCookies by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("閲覧データの消去") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("削除する項目を選択してください:", fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = clearHistory, onCheckedChange = { clearHistory = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("閲覧履歴", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = clearCache, onCheckedChange = { clearCache = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("キャッシュされたファイルと画像", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = clearCookies, onCheckedChange = { clearCookies = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cookieおよび保存されたサイトデータ", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearBrowsingData(clearHistory, clearCache, clearCookies)
                        showClearDataDialog = false
                    }
                ) {
                    Text("消去する", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // Reset Confirm Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("設定の初期化") },
            text = { Text("ブラウザのすべての設定を初期値に戻しますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetSettings()
                        showResetConfirmDialog = false
                    }
                ) {
                    Text("初期化", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(text = description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange() }
        )
    }
}
