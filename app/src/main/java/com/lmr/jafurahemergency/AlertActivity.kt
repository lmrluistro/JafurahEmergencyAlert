package com.lmr.jafurahemergency

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlertActivity : Activity() {
    private var ringtone: Ringtone? = null
    private var tts: TextToSpeech? = null
    private var vibrator: Vibrator? = null
    private var isActivation = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true); setTurnScreenOn(true)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        isActivation = intent.getBooleanExtra(EXTRA_ACTIVE, true)
        val message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty()
        val sender = intent.getStringExtra(EXTRA_SENDER).orEmpty()
        val color = if (isActivation) Color.rgb(150, 0, 0) else Color.rgb(0, 100, 48)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(24, 30, 24, 30); setBackgroundColor(color)
        }
        root.addView(TextView(this).apply {
            text = if (isActivation) "🚨 EMERGENCY" else "🟢 ALL CLEAR"
            textSize = 32f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = if (isActivation) "SHELTER-IN-PLACE\nACTIVATED" else "SHELTER-IN-PLACE\nDEACTIVATED"
            textSize = 26f; setTextColor(Color.WHITE); gravity = Gravity.CENTER; setPadding(0, 22, 0, 22)
        })
        root.addView(TextView(this).apply {
            text = "Source: $sender\n\n$message\n\n${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}"
            textSize = 15f; setTextColor(Color.WHITE); gravity = Gravity.CENTER; setPadding(0, 10, 0, 24)
        })
        root.addView(Button(this).apply {
            text = "ACKNOWLEDGE / STOP SOUND"; textSize = 17f
            setOnClickListener { stopAlert(); finish() }
        })
        setContentView(root)
        saveHistory(isActivation, message, sender)
        startAlert()
    }

    private fun startAlert() {
        val uri = if (isActivation) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        try {
            ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
                if (Build.VERSION.SDK_INT >= 28) isLooping = isActivation
                play()
            }
        } catch (_: Exception) { }
        vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
        if (isActivation) {
            val pattern = longArrayOf(0, 700, 250, 700, 250, 1200)
            try { if (Build.VERSION.SDK_INT >= 26) vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0)) else @Suppress("DEPRECATION") vibrator?.vibrate(pattern, 0) } catch (_: Exception) { }
        } else {
            try { if (Build.VERSION.SDK_INT >= 26) vibrator?.vibrate(VibrationEffect.createOneShot(350, VibrationEffect.DEFAULT_AMPLITUDE)) else @Suppress("DEPRECATION") vibrator?.vibrate(350) } catch (_: Exception) { }
        }
        tts = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.speak(if (isActivation) "Emergency. Shelter in place activated." else "All clear. Shelter in place deactivated.", TextToSpeech.QUEUE_FLUSH, null, "jafurah-alert")
            }
        }
    }

    private fun stopAlert() {
        try { ringtone?.stop() } catch (_: Exception) { }
        try { vibrator?.cancel() } catch (_: Exception) { }
        try { tts?.stop(); tts?.shutdown() } catch (_: Exception) { }
    }

    override fun onDestroy() { stopAlert(); super.onDestroy() }

    private fun saveHistory(active: Boolean, message: String, sender: String) {
        val prefs = getSharedPreferences("history", Context.MODE_PRIVATE)
        val old = prefs.getString("events", "").orEmpty().lines().filter { it.isNotBlank() }
        val row = "${System.currentTimeMillis()}|${if (active) "ACTIVATED" else "DEACTIVATED"}|${sender.replace('|', '/') }|${message.replace('|', '/').replace('\n', ' ')}"
        prefs.edit().putString("events", (listOf(row) + old).take(100).joinToString("\n")).apply()
    }

    companion object {
        private const val EXTRA_ACTIVE = "active"
        private const val EXTRA_MESSAGE = "message"
        private const val EXTRA_SENDER = "sender"
        private const val CHANNEL = "jafurah_emergency_alerts"

        fun launch(context: Context, active: Boolean, message: String, sender: String) {
            ensureChannel(context)
            val intent = Intent(context, AlertActivity::class.java).apply {
                putExtra(EXTRA_ACTIVE, active); putExtra(EXTRA_MESSAGE, message); putExtra(EXTRA_SENDER, sender)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val pending = PendingIntent.getActivity(context, if (active) 11 else 12, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val title = if (active) "🚨 SHELTER-IN-PLACE ACTIVATED" else "🟢 SHELTER-IN-PLACE DEACTIVATED"
            val notification = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(if (active) "Emergency alert received — open immediately" else "All-clear message received")
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .setOngoing(active)
                .setFullScreenIntent(pending, true)
                .setContentIntent(pending)
                .build()
            manager.notify(if (active) 1001 else 1002, notification)
            // Try opening the alert immediately; Android may block background activity launches on some versions.
            try { context.startActivity(intent) } catch (_: Exception) { }
        }

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= 26) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = NotificationChannel(CHANNEL, "Jafurah Emergency Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Urgent shelter-in-place activation and all-clear alerts"
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 700, 250, 700, 250, 1200)
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
