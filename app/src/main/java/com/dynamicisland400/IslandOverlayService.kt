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
    private var island: LinearLayout? = null
    private var titleView: TextView? = null
    private var detailView: TextView? = null
    private var collapse: Runnable? = null

    private fun dp(v: Float) = (v * resources.displayMetrics.density).roundToInt()

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(400, notification())
        instance=this
        if (Settings.canDrawOverlays(this)) showIsland() else stopSelf()
    }

    private fun showIsland() {
        if (island != null) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val context = createDisplayContext(windowManager!!.defaultDisplay)
            .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
        val prefs = getSharedPreferences("island_settings", MODE_PRIVATE)
        val width = prefs.getInt("width", 162)
        val height = prefs.getInt("height", 38)
        val x = prefs.getInt("x", 0)
        val y = prefs.getInt("y", 8)

        island = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(14f), dp(4f), dp(14f), dp(4f))
            background = GradientDrawable().apply {
                setColor(Color.BLACK)
                cornerRadius = dp(28f).toFloat()
            }
            elevation = dp(10f).toFloat()
            isClickable = true
            setOnClickListener { toggleExpanded() }
            setOnLongClickListener { collapseNow(); true }
        }
        titleView = TextView(context).apply {
            text = "●"
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            maxLines = 1
        }
        detailView = TextView(context).apply {
            textSize = 10f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            maxLines = 1
            visibility = View.GONE
        }
        island!!.addView(titleView, LinearLayout.LayoutParams(-1, dp(22f)))
        island!!.addView(detailView, LinearLayout.LayoutParams(-1, dp(18f)))

        val params = WindowManager.LayoutParams(
            dp(width.toFloat()), dp(height.toFloat()),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            this.x = dp(x.toFloat()); this.y = dp(y.toFloat())
        }
        try {
            windowManager!!.addView(island, params)
            island!!.scaleX=.94f; island!!.scaleY=.94f
            island!!.animate().scaleX(1f).scaleY(1f).setDuration(220).start()
        } catch (_: Exception) { island=null; stopSelf() }
    }

    private fun toggleExpanded() {
        val v=island ?: return
        if (detailView?.visibility == View.VISIBLE) collapseNow() else {
            detailView?.visibility=View.VISIBLE
            titleView?.text=if (titleView?.text=="●") "Dynamic Island" else titleView?.text
            resize(dp(300f), dp(76f))
            scheduleCollapse()
        }
    }

    private fun resize(w:Int,h:Int) {
        val lp=island?.layoutParams as? WindowManager.LayoutParams ?: return
        lp.width=w; lp.height=h
        try { windowManager?.updateViewLayout(island,lp) } catch (_:Exception) {}
    }

    private fun collapseNow() {
        collapse?.let { Handler(Looper.getMainLooper()).removeCallbacks(it) }
        detailView?.visibility=View.GONE
        titleView?.text="●"
        val prefs=getSharedPreferences("island_settings",MODE_PRIVATE)
        resize(dp(prefs.getInt("width",162).toFloat()),dp(prefs.getInt("height",38).toFloat()))
    }

    private fun scheduleCollapse() {
        collapse?.let { Handler(Looper.getMainLooper()).removeCallbacks(it) }
        val r=Runnable{collapseNow()}; collapse=r
        Handler(Looper.getMainLooper()).postDelayed(r,4500)
    }

    fun showEvent(title:String, detail:String) {
        Handler(Looper.getMainLooper()).post {
            titleView?.text=title.take(28)
            detailView?.text=detail.take(55)
            detailView?.visibility=View.VISIBLE
            resize(dp(300f),dp(76f))
            scheduleCollapse()
        }
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("island","Dynamic Island",NotificationManager.IMPORTANCE_LOW)
        )
    }
    private fun notification(): Notification =
        NotificationCompat.Builder(this,"island")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Dynamic Island is active")
            .setContentText("The overlay is running.")
            .setOngoing(true).setCategory(Notification.CATEGORY_SERVICE).build()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {\n        if (intent?.action=="SHOW_EVENT") showEvent(intent.getStringExtra("title") ?: "Notification", intent.getStringExtra("detail") ?: "")\n        return START_STICKY\n    }\n\n    override fun onDestroy() {
        collapse?.let { Handler(Looper.getMainLooper()).removeCallbacks(it) }
        island?.let { try{windowManager?.removeView(it)}catch(_:Exception){} }
        island=null; instance=null; super.onDestroy()
    }
    override fun onBind(intent: Intent?)=null

    companion object {
        var instance:IslandOverlayService?=null
    }
}