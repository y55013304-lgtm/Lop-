package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val searchEngineName: String = "GOOGLE",
    val themeMode: String = "SYSTEM",
    val adBlockLevel: String = "AGGRESSIVE",
    val adBlockCosmetic: Boolean = true,
    val javaScriptEnabled: Boolean = true,
    val blockPopups: Boolean = true,
    val sendDoNotTrack: Boolean = true,
    val blockThirdPartyCookies: Boolean = true,
    val warnInsecure: Boolean = true,
    val clearOnExit: Boolean = false,
    val loadImages: Boolean = true,
    val supportZoom: Boolean = true,
    val textZoom: Int = 100,
    val forceDarkWeb: Boolean = false,
    val openDesktopByDefault: Boolean = false,
    val enableSafeBrowsingGuard: Boolean = true,
    val enableDownloadProtection: Boolean = true,
    val enableGooglePasswordManagerAutofill: Boolean = true,
    val showPasswordManagerQuickBar: Boolean = true
)
