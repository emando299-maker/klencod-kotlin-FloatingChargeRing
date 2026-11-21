package com.floatingchargering

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: SharedPreferences
    private lateinit var reader: BatteryReader
    private lateinit var status: TextView
    private lateinit var toggle: Button
    private lateinit var diagnostics: TextView

    private val refresh = object : Runnable {
        override fun run() {
            reader.read()
            diagnostics.text = reader.diagnosticText()
            updateStatus()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(FloatingRingService.PREFS, MODE_PRIVATE)
        reader = BatteryReader(this)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(32, 48, 32, 32)
            setBackgroundColor(Color.rgb(5, 5, 14))
        }

        val title = TextView(this).apply {
            text = "FLOATING CHARGE RING"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 24)
        }
        val permButton = Button(this).apply {
            text = "منح إذن العرض فوق التطبيقات"
            setOnClickListener { openOverlaySettings() }
        }
        toggle = Button(this).apply {
            setOnClickListener { toggleService() }
        }

        val sizeLabel = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(0, 32, 0, 8)
        }
        val seek = SeekBar(this).apply {
            max = FloatingRingService.MAX_SIZE
            progress = prefs.getInt(FloatingRingService.KEY_SIZE, FloatingRingService.DEFAULT_SIZE)
                .coerceIn(0, FloatingRingService.MAX_SIZE)
        }
        fun showSize(v: Int) {
            sizeLabel.text = if (v == 0) "الحجم: 0 (مخفية)" else "الحجم: $v بكسل"
        }
        showSize(seek.progress)
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                showSize(value)
                if (fromUser) prefs.edit().putInt(FloatingRingService.KEY_SIZE, value).apply()
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })

        val hint = TextView(this).apply {
            text = "اسحب الدائرة بإصبعك لتحريكها على الشاشة. القيمة 0 تخفيها، والحد الأقصى 1000 بكسل."
            textSize = 13f
            setTextColor(Color.rgb(170, 170, 190))
            setPadding(0, 16, 0, 0)
        }
        diagnostics = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 12f
            setTextColor(Color.rgb(150, 220, 170))
            textDirection = View.TEXT_DIRECTION_LTR
            gravity = Gravity.START
            setPadding(0, 36, 0, 0)
        }

        layout.addView(title)
        layout.addView(status)
        layout.addView(permButton)
        layout.addView(toggle)
        layout.addView(sizeLabel)
        layout.addView(seek)
        layout.addView(hint)
        layout.addView(diagnostics)

        setContentView(ScrollView(this).apply {
            isFillViewport = true
            addView(layout)
        })
    }

    override fun onResume() {
        super.onResume()
        handler.post(refresh)
    }

    override fun onPause() {
        handler.removeCallbacks(refresh)
        super.onPause()
    }

    private fun updateStatus() {
        val granted = Settings.canDrawOverlays(this)
        val running = FloatingRingService.running
        status.text = (if (granted) "إذن العرض فوق التطبيقات: ممنوح" else "إذن العرض فوق التطبيقات: غير ممنوح") +
            "\n" + (if (running) "النافذة العائمة: تعمل" else "النافذة العائمة: متوقفة")
        toggle.text = if (running) "إيقاف النافذة العائمة" else "تشغيل النافذة العائمة"
    }

    private fun openOverlaySettings() {
        startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        )
    }

    private fun toggleService() {
        val intent = Intent(this, FloatingRingService::class.java)
        if (FloatingRingService.running) {
            stopService(intent)
            handler.postDelayed({ updateStatus() }, 300L)
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "امنح إذن العرض فوق التطبيقات أولًا", Toast.LENGTH_LONG).show()
            openOverlaySettings()
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        handler.postDelayed({ updateStatus() }, 500L)
    }
}