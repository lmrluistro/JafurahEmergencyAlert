package com.lmr.jafurahemergency

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.os.Bundle
import java.util.Locale

class WhatsAppNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != "com.whatsapp" && sbn.packageName != "com.whatsapp.w4b") return
        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = collectText(extras)
        if (text.isBlank()) return

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val filter = prefs.getString("groupFilter", "").orEmpty().trim()
        // If configured, require group text to appear in the notification title or combined visible text.
        if (filter.isNotEmpty() && !(title + " " + text).contains(filter, ignoreCase = true)) return

        val normalized = text.lowercase(Locale.ROOT).replace('–', '-').replace('—', '-')
            .replace(Regex("\\s+"), " ").trim()
        val active = phrases(prefs.getString("activate", "shelter-in-place is activated,shelter-in-place activated,shelter in place is activated,shelter in place activated").orEmpty())
        val inactive = phrases(prefs.getString("deactivate", "shelter-in-place is deactivated,shelter-in-place deactivated,shelter in place is deactivated,shelter in place deactivated").orEmpty())
        // Check deactivation first to avoid matching the word "activated" inside "deactivated".
        when {
            inactive.any { normalized.contains(it) } -> AlertActivity.launch(this, false, text, title)
            active.any { normalized.contains(it) } -> AlertActivity.launch(this, true, text, title)
        }
    }

    private fun collectText(extras: Bundle): String {
        val parts = mutableListOf<String>()
        listOf(Notification.EXTRA_TEXT, Notification.EXTRA_BIG_TEXT, Notification.EXTRA_SUB_TEXT, Notification.EXTRA_SUMMARY_TEXT).forEach { key ->
            extras.getCharSequence(key)?.toString()?.let { if (it.isNotBlank()) parts.add(it) }
        }
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach { it?.toString()?.let { s -> if (s.isNotBlank()) parts.add(s) } }
        return parts.distinct().joinToString(" ")
    }

    private fun phrases(raw: String): List<String> = raw.split(',').map { it.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ") }.filter { it.isNotBlank() }
}
