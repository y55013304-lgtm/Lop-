package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.autofill.AutofillManager
import android.webkit.WebView

object GooglePasswordManagerHelper {

    const val GOOGLE_PASSWORDS_WEB_URL = "https://passwords.google.com"
    const val GOOGLE_PASSWORD_CHECKUP_URL = "https://passwords.google.com/checkup"

    /**
     * JavaScript to optimize web forms for Android Autofill and Google Password Manager.
     * Ensures input fields have standard autocomplete tokens and detects password fields.
     */
    const val AUTOFILL_ENHANCER_JS = """
    (function() {
        try {
            var passwordInputs = document.querySelectorAll('input[type="password"]');
            if (passwordInputs.length > 0) {
                // Signal to native Lop browser that a login form is present
                passwordInputs.forEach(function(input) {
                    if (!input.getAttribute('autocomplete')) {
                        input.setAttribute('autocomplete', 'current-password');
                    }
                    var form = input.form;
                    if (form) {
                        var textInputs = form.querySelectorAll('input[type="text"], input[type="email"], input[type="tel"]');
                        textInputs.forEach(function(ti) {
                            if (!ti.getAttribute('autocomplete')) {
                                var name = (ti.name || ti.id || '').toLowerCase();
                                if (name.includes('user') || name.includes('mail') || name.includes('login') || name.includes('id')) {
                                    ti.setAttribute('autocomplete', 'username');
                                }
                            }
                        });
                    }
                });
            }
        } catch(e) {}
    })();
    """

    /**
     * Opens the native Google Password Manager if available on the device,
     * otherwise opens passwords.google.com.
     */
    fun openGooglePasswordManager(
        context: Context,
        onFallbackWebTab: (String) -> Unit
    ) {
        val intents = listOf(
            // 1. Google Play Services Password Manager direct action
            Intent("com.google.android.gms.autofill.service.PASSWORD_MANAGER").apply {
                setPackage("com.google.android.gms")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            // 2. Google Play Services Password Manager Activity component
            Intent().apply {
                setClassName(
                    "com.google.android.gms",
                    "com.google.android.gms.autofill.ui.PasswordManagerActivity"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            // 3. Google Play Services Credential Provider / Password Manager settings
            Intent("android.settings.REQUEST_SET_AUTOFILL_SERVICE").apply {
                data = Uri.parse("package:com.google.android.gms")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )

        for (intent in intents) {
            try {
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return
                }
            } catch (e: Exception) {
                // Continue to next intent fallback
            }
        }

        // Web fallback: open Google Password Manager online
        onFallbackWebTab(GOOGLE_PASSWORDS_WEB_URL)
    }

    /**
     * Opens Google Password Checkup to check for compromised, reused, or weak passwords.
     */
    fun openPasswordCheckup(
        context: Context,
        onFallbackWebTab: (String) -> Unit
    ) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_PASSWORD_CHECKUP_URL)).apply {
                setPackage("com.android.chrome")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return
            }
        } catch (e: Exception) {
            // Ignore
        }
        onFallbackWebTab(GOOGLE_PASSWORD_CHECKUP_URL)
    }

    /**
     * Opens the Android system Autofill service settings so the user can verify
     * Google is set as the primary autofill and password provider.
     */
    fun openAndroidAutofillSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                    data = Uri.parse("package:com.google.android.gms")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                // Fallback to general settings
            }

            try {
                val intent = Intent("android.settings.AUTOFILL_SETTINGS").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                // Fallback to general settings
            }
        }

        try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // No-op
        }
    }

    /**
     * Configures a WebView to participate in Android Autofill and Google Password Manager.
     */
    fun setupWebViewAutofill(webView: WebView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
        }
    }

    /**
     * Requests Android Autofill to trigger on the current focused field in WebView.
     */
    fun requestAutofill(context: Context, webView: WebView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val afm = context.getSystemService(AutofillManager::class.java)
                afm?.requestAutofill(webView)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
