package com.dynamicisland400

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.view.Gravity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : androidx.appcompat.app.AppCompatActivity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun buildUi() {
        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.BLACK)
        }

        root.addView(TextView(this).apply {
            text = "Dynamic Island 400"
            textSize = 28f
            setTextColor(Color.WHITE)
            setPadding(0, 20, 0, 12)
        })

        root.addView(TextView(this).apply {
            text = "HONOR 400 Dynamic Island overlay.\n\nBecause this APK is installed outside Google Play, Android may require one extra security step before allowing the overlay."
            textSize = 16f
            setTextColor(Color.LTGRAY)
            setPadding(0, 0, 0, 20)
        })

        status = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.GRAY)
            setPadding(0, 0, 0, 20)
        }
        root.addView(status)

        root.addView(Button(this).apply {
            text = "1. Open App Info / Allow Restricted Settings"
            setOnClickListener { openAppInfo() }
        }, LinearLayout.LayoutParams(-1, -2))

        root.addView(Button(this).apply {
            text = "2. Allow Display Over Other Apps"
            setOnClickListener { openOverlaySettings() }
        }, LinearLayout.LayoutParams(-1, -2))

        root.addView(Button(this).apply {
            text = "3. Start Dynamic Island"
            setOnClickListener { startIsland() }
        }, LinearLayout.LayoutParams(-1, -2))

        root.addView(Button(this).apply {
            text = "Stop Island"
            setOnClickListener { stopService(Intent(this@MainActivity, IslandOverlayService::class.java)) }
        }, LinearLayout.LayoutParams(-1, -2))

        setContentView(root)
        updateStatus()
    }

    private fun openAppInfo() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }

    private fun openOverlaySettings() {
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
        }
    }

    private fun startIsland() {
        if (!Settings.canDrawOverlays(this)) {
            updateStatus()
            openOverlaySettings()
            return
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                400
            )
            return
        }

        ContextCompat.startForegroundService(
            this,
            Intent(this, IslandOverlayService::class.java)
        )
    }

    private fun updateStatus() {
        if (!::status.isInitialized) return
        val overlay = Settings.canDrawOverlays(this)
        val notifications = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        status.text = when {
            overlay && notifications -> "Ready: overlay + notifications allowed."
            overlay -> "Overlay allowed. Notifications are optional but recommended."
            else -> "Overlay not allowed yet. If Android says “App was denied access”, open App Info → ⋮ → Allow restricted settings, then return here."
        }
    }
}