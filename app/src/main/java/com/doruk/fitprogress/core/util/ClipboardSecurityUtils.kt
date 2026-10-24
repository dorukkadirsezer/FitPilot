package com.doruk.fitprogress.core.util

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle

/**
 * M5 Clipboard Security Utility.
 * Marks copied workout/telemetry payloads with `EXTRA_IS_SENSITIVE = true`
 * to prevent Android 13+ clipboard preview popups and third-party IME history logging.
 */
object ClipboardSecurityUtils {

    private const val EXTRA_IS_SENSITIVE_COMPAT = "android.content.extra.IS_SENSITIVE"

    fun copySensitiveText(
        context: Context,
        label: String,
        text: String
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = ClipData.newPlainText(label, text)
        val extras = PersistableBundle().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
            putBoolean(EXTRA_IS_SENSITIVE_COMPAT, true)
        }
        clip.description.extras = extras
        clipboard.setPrimaryClip(clip)
    }
}
