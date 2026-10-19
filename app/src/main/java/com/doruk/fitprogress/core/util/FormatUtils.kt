package com.doruk.fitprogress.core.util

import android.content.Context
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object FormatUtils {
    
    fun getAppLocale(context: Context): Locale {
        return context.resources.configuration.locales[0]
    }

    fun formatDouble(value: Double?, locale: Locale, maxDecimals: Int = 1): String {
        if (value == null) return "-"
        val format = NumberFormat.getInstance(locale).apply {
            maximumFractionDigits = maxDecimals
            minimumFractionDigits = 0
        }
        return format.format(value)
    }

    fun formatDouble(value: Float?, locale: Locale, maxDecimals: Int = 1): String {
        if (value == null) return "-"
        return formatDouble(value.toDouble(), locale, maxDecimals)
    }

    fun parseFloatSafe(text: String): Float? {
        val sanitized = text.replace(',', '.').trim()
        return sanitized.toFloatOrNull()
    }
    
    fun formatDoubleEditable(value: Float?, locale: Locale): String {
        if (value == null) return ""
        val format = NumberFormat.getInstance(locale).apply {
            maximumFractionDigits = 1
            minimumFractionDigits = 0
            isGroupingUsed = false
        }
        return format.format(value)
    }

    fun parseIntSafe(text: String): Int? {
        return text.trim().toIntOrNull()
    }

    fun formatDate(millis: Long, locale: Locale): String {
        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    }

    fun formatDateIso(isoString: String, locale: Locale): String {
        return try {
            val date = LocalDate.parse(isoString, DateTimeFormatter.ISO_LOCAL_DATE)
            date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
        } catch (e: Exception) {
            isoString
        }
    }

    fun formatDuration(seconds: Long?, locale: Locale): String {
        if (seconds == null) return "-"
        if (seconds == 0L) return "0 sn"
        
        val isTurkish = locale.language == "tr"
        
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        
        return buildString {
            if (h > 0) {
                append(h).append(" ").append(if (isTurkish) "sa" else "hr").append(" ")
                append(m).append(" ").append(if (isTurkish) "dk" else "min")
            } else if (m > 0) {
                append(m).append(" ").append(if (isTurkish) "dk" else "min")
            } else {
                append(s).append(" ").append(if (isTurkish) "sn" else "sec")
            }
        }.trim()
    }
}
