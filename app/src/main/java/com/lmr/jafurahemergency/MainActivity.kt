package com.lmr.jafurahemergency

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    private lateinit var groupFilter: EditText
    private lateinit var activationWords: EditText
    private lateinit var deactivationWords: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(0xFF07111F.toInt())
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)

        content.addView(TextView(this).apply {
            text = "JAFURAH\nEMERGENCY ALERT"
            textSize = 28f; setTextColor(0xFFFFC928.toInt()); gravity = Gravity.CENTER
        }, match(WRAP))
        content.addView(TextView(this).apply {
            text = "WhatsApp emergency notification monitor"
            textSize = 15f; setTextColor(0xFFE7EDF5.toInt()); gravity = Gravity.CENTER
            setPadding(0, 8, 0, 20)
        }, match(WRAP))

        status = TextView(this).apply {
            textSize = 16f; setTextColor(0xFF8FF0A4.toInt()); setPadding(14, 14, 14, 14)
            setBackgroundColor(0xFF142536.toInt())
        }
        content.addView(status, match(WRAP))
        refreshStatus()

        content.addView(label("Emergency group filter (text seen in notification title)"))
        groupFilter = field(prefs.getString("groupFilter", "SUBCON Management Emergency Group") ?: "SUBCON Management Emergency Group", "e.g. Jafurah Emergency")
        content.addView(groupFilter, match(WRAP))
        content.addView(label("Activation phrases (separate with commas)"))
        activationWords = field(prefs.getString("activate", "shelter-in-place is activated,shelter-in-place activated,shelter in place is activated,shelter in place activated") ?: "", "Activation phrases")
        content.addView(activationWords, match(WRAP))
        content.addView(label("Deactivation phrases (separate with commas)"))
        deactivationWords = field(prefs.getString("deactivate", "shelter-in-place is deactivated,shelter-in-place deactivated,shelter in place is deactivated,shelter in place deactivated") ?: "", "Deactivation phrases")
        content.addView(deactivationWords, match(WRAP))

        content.addView(button("SAVE SETTINGS") {
            prefs.edit().putString("groupFilter", groupFilter.text.toString().trim())
                .putString("activate", activationWords.text.toString())
                .putString("deactivate", deactivationWords.text.toString()).apply()
            Toast.makeText(this, "Settings saved on this phone", Toast.LENGTH_SHORT).show()
            refreshStatus()
        }, match(WRAP))
        content.addView(button("ENABLE WHATSAPP NOTIFICATION ACCESS") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }, match(WRAP))
        content.addView(button("APP NOTIFICATION SETTINGS") {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        }, match(WRAP))
        content.addView(button("TEST ACTIVATION ALARM") { AlertActivity.launch(this, true, "TEST ACTIVATION — NOT A REAL EMERGENCY", "Test mode") }, match(WRAP))
        content.addView(button("TEST ALL-CLEAR ALERT") { AlertActivity.launch(this, false, "TEST DEACTIVATION — NOT A REAL EMERGENCY", "Test mode") }, match(WRAP))
        content.addView(TextView(this).apply {
            text = "IMPORTANT: This is an early test build. It only detects text Android exposes in WhatsApp notifications. It cannot read WhatsApp directly, cannot guarantee alerts if notifications are hidden or suppressed, and must not replace official site alarms or emergency procedures."
            textSize = 14f; setTextColor(0xFFFFD4A8.toInt()); setPadding(0, 18, 0, 8)
        }, match(WRAP))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    override fun onResume() { super.onResume(); if (::status.isInitialized) refreshStatus() }

    private fun refreshStatus() {
        val enabled = NotificationManagerCompatHelper.isListenerEnabled(this)
        status.text = if (enabled) "● MONITOR PERMISSION ENABLED\nConfirm with a test message before relying on it." else "● MONITOR NOT ENABLED\nTap ENABLE WHATSAPP NOTIFICATION ACCESS, then enable this app."
        status.setTextColor(if (enabled) 0xFF8FF0A4.toInt() else 0xFFFF7373.toInt())
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 300)
        }
    }

    private fun label(text: String) = TextView(this).apply { this.text = text; textSize = 14f; setTextColor(0xFFE7EDF5.toInt()); setPadding(2, 18, 2, 6) }
    private fun field(value: String, hintText: String) = EditText(this).apply {
        setText(value); hint = hintText; textSize = 15f; setTextColor(0xFFFFFFFF.toInt()); setHintTextColor(0xFF8B9CAF.toInt())
        setPadding(14, 12, 14, 12); setBackgroundColor(0xFF17283A.toInt())
    }
    private fun button(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text; isAllCaps = false; setTextColor(0xFF07111F.toInt()); setBackgroundColor(0xFFFFC928.toInt())
        setOnClickListener { action() }; setPadding(8, 8, 8, 8)
    }.also { it.layoutParams = LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 12 } }
    private fun match(height: Int) = LinearLayout.LayoutParams(-1, height)
    companion object { private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT }
}

object NotificationManagerCompatHelper {
    fun isListenerEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
        return enabled.contains(context.packageName)
    }
}
