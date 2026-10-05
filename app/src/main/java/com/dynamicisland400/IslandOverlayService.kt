package com.dynamicisland400

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import kotlin.math.roundToInt

class IslandOverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var island: FrameLayout? = null

    private fun dp(v: Float) = (v * resources.displayMetrics.density).roundToInt()

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(400, notification())
        if (Settings.canDrawOverlays(this)) showIsland() else stopSelf()
    }

    private fun showIsland() {
        if (island != null) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val context = createDisplayContext(windowManager!!.defaultDisplay)
            .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)

        island = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                setColor(Color.BLACK)
                cornerRadius = dp(24f).toFloat()
            }
            elevation = dp(10f).toFloat()
        }

        val prefs = getSharedPreferences("island_settings", MODE_PRIVATE)
        val x = prefs.getInt("x", 0)
        val y = prefs.getInt("y", 8)

        val params = WindowManager.LayoutParams(
            dp(162f),
            dp(38f),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            this.x = dp(x.toFloat())
            this.y = dp(y.toFloat())
        }

        try {
            windowManager!!.addView(island, params)
            island!!.scaleX = .94f
            island!!.scaleY = .94f
            island!!.animate().scaleX(1f).scaleY(1f).setDuration(220).start()
        } catch (_: SecurityException) {
            island = null
            stopSelf()
        } catch (_: WindowManager.BadTokenException) {
            island = null
            stopSelf()
        }
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel("island", "Dynamic Island", NotificationManager.IMPORTANCE_LOW)
            )
    }

    private fun notification(): Notification =
        NotificationCompat.Builder(this, "island")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Dynamic Island is active")
            .setContentText("The overlay is running.")
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()

    override fun onDestroy() {
        island?.let { try { windowManager?.removeView(it) } catch (_: Exception) {} }
        island = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}
