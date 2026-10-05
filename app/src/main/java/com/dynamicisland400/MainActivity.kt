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
import android.widget.ScrollView
import android.widget.TextView
import android.graphics.Color
import android.view.Gravity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : androidx.appcompat.app.AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var positionText: TextView
    private lateinit var sizeText: TextView
    private val prefs by lazy { getSharedPreferences("island_settings", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); buildUi() }
    override fun onResume() { super.onResume(); updateStatus(); updatePositionText(); updateSizeText() }

    private fun buildUi() {
        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.BLACK)
        }
        root.addView(TextView(this).apply { text="Dynamic Island 400"; textSize=28f; setTextColor(Color.WHITE); setPadding(0,20,0,12) })
        root.addView(TextView(this).apply { text="iPhone-style interactive overlay for HONOR 400."; textSize=16f; setTextColor(Color.LTGRAY); setPadding(0,0,0,16) })
        status=TextView(this).apply { textSize=15f; setTextColor(Color.GRAY); setPadding(0,0,0,12) }; root.addView(status)
        root.addView(Button(this).apply { text="1. Open App Info / Allow Restricted Settings"; setOnClickListener{openAppInfo()} })
        root.addView(Button(this).apply { text="2. Allow Display Over Other Apps"; setOnClickListener{openOverlaySettings()} })
        root.addView(Button(this).apply { text="3. Allow Notification Access (for live events)"; setOnClickListener{openNotificationAccess()} })
        root.addView(Button(this).apply { text="4. Start Dynamic Island"; setOnClickListener{startIsland()} })

        root.addView(TextView(this).apply { text="SIZE"; textSize=13f; setTextColor(Color.LTGRAY); setPadding(0,24,0,8) })
        sizeText=TextView(this).apply { textSize=14f; setTextColor(Color.WHITE); gravity=Gravity.CENTER; setPadding(0,0,0,8) }; root.addView(sizeText)
        root.addView(Button(this).apply { text="WIDTH  +"; setOnClickListener{changeSize(8,0)} })
        root.addView(Button(this).apply { text="WIDTH  −"; setOnClickListener{changeSize(-8,0)} })
        root.addView(Button(this).apply { text="HEIGHT  +"; setOnClickListener{changeSize(0,4)} })
        root.addView(Button(this).apply { text="HEIGHT  −"; setOnClickListener{changeSize(0,-4)} })
        root.addView(Button(this).apply { text="Reset Size"; setOnClickListener{prefs.edit().putInt("width",162).putInt("height",38).apply(); updateSizeText(); restartIslandIfRunning()} })

        root.addView(TextView(this).apply { text="POSITION"; textSize=13f; setTextColor(Color.LTGRAY); setPadding(0,24,0,8) })
        positionText=TextView(this).apply { textSize=14f; setTextColor(Color.WHITE); gravity=Gravity.CENTER; setPadding(0,0,0,8) }; root.addView(positionText)
        root.addView(Button(this).apply { text="▲  UP"; setOnClickListener{move(0,-4)} })
        root.addView(Button(this).apply { text="▼  DOWN"; setOnClickListener{move(0,4)} })
        root.addView(Button(this).apply { text="◀  LEFT"; setOnClickListener{move(-4,0)} })
        root.addView(Button(this).apply { text="RIGHT  ▶"; setOnClickListener{move(4,0)} })
        root.addView(Button(this).apply { text="Reset Position"; setOnClickListener{prefs.edit().putInt("x",0).putInt("y",8).apply(); updatePositionText(); restartIslandIfRunning()} })
        root.addView(Button(this).apply { text="Stop Island"; setOnClickListener{stopService(Intent(this@MainActivity,IslandOverlayService::class.java))} })
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.BLACK)
            addView(root, ScrollView.LayoutParams(-1, -1))
        }
        setContentView(scroll); updateStatus(); updatePositionText(); updateSizeText()
    }

    private fun changeSize(dw:Int,dh:Int) {
        val w=(prefs.getInt("width",162)+dw).coerceIn(80,360)
        val h=(prefs.getInt("height",38)+dh).coerceIn(24,120)
        prefs.edit().putInt("width",w).putInt("height",h).apply()
        updateSizeText(); restartIslandIfRunning()
    }
    private fun move(dx:Int,dy:Int) {
        prefs.edit().putInt("x",prefs.getInt("x",0)+dx).putInt("y",prefs.getInt("y",8)+dy).apply()
        updatePositionText(); restartIslandIfRunning()
    }
    private fun restartIslandIfRunning() {
        stopService(Intent(this,IslandOverlayService::class.java))
        if(Settings.canDrawOverlays(this)) ContextCompat.startForegroundService(this,Intent(this,IslandOverlayService::class.java))
    }
    private fun openAppInfo(){ startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$packageName"))) }
    private fun openOverlaySettings(){ try{startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))}catch(_:Exception){startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))} }
    private fun openNotificationAccess(){ startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) }
    private fun startIsland(){
        if(!Settings.canDrawOverlays(this)){updateStatus();openOverlaySettings();return}
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.POST_NOTIFICATIONS),400);return}
        ContextCompat.startForegroundService(this,Intent(this,IslandOverlayService::class.java))
    }
    private fun updateStatus(){
        if(!::status.isInitialized)return
        val overlay=Settings.canDrawOverlays(this)
        val notifications=Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED
        status.text=when{overlay&&notifications->"Ready: overlay + notifications allowed.";overlay->"Overlay allowed. Notifications are optional but recommended.";else->"Overlay not allowed yet. Open App Info → ⋮ → Allow restricted settings, then allow Display Over Other Apps."}
    }
    private fun updatePositionText(){if(!::positionText.isInitialized)return;positionText.text="Horizontal: "+prefs.getInt("x",0)+"   Vertical: "+prefs.getInt("y",8)}
    private fun updateSizeText(){if(!::sizeText.isInitialized)return;sizeText.text="Width: "+prefs.getInt("width",162)+"   Height: "+prefs.getInt("height",38)}
}