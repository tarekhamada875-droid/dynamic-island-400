package com.dynamicisland400

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.view.Gravity

class MainActivity : androidx.appcompat.app.AppCompatActivity() {
    private lateinit var status: TextView
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); buildUi() }
    override fun onResume() { super.onResume(); updateStatus() }
    private fun buildUi() {
        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(pad,pad,pad,pad); gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.BLACK)
        }
        root.addView(TextView(this).apply { text="Dynamic Island 400"; textSize=28f; setTextColor(Color.WHITE); setPadding(0,20,0,12) })
        root.addView(TextView(this).apply {
            text="iPhone-style Dynamic Island overlay for HONOR 400.\n\nGrant overlay permission, then start the island."
            textSize=16f; setTextColor(Color.LTGRAY); setPadding(0,0,0,24)
        })
        status = TextView(this).apply { textSize=15f; setTextColor(Color.GRAY); setPadding(0,0,0,20) }
        root.addView(status)
        root.addView(Button(this).apply {
            text="1. Allow Display Over Other Apps"
            setOnClickListener { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }
        }, LinearLayout.LayoutParams(-1,-2))
        root.addView(Button(this).apply {
            text="2. Start Dynamic Island"
            setOnClickListener { if (Settings.canDrawOverlays(this@MainActivity)) startForegroundService(Intent(this@MainActivity, IslandOverlayService::class.java)) }
        }, LinearLayout.LayoutParams(-1,-2))
        root.addView(Button(this).apply {
            text="Stop Island"
            setOnClickListener { stopService(Intent(this@MainActivity, IslandOverlayService::class.java)) }
        }, LinearLayout.LayoutParams(-1,-2))
        setContentView(root); updateStatus()
    }
    private fun updateStatus() {
        if (::status.isInitialized) status.text = if (Settings.canDrawOverlays(this)) "Overlay permission: GRANTED" else "Overlay permission: NOT GRANTED"
    }
}
