package com.dynamicisland400

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import kotlin.math.roundToInt

class IslandOverlayService : Service() {
    private var wm: WindowManager? = null
    private var island: LinearLayout? = null
    private var title: TextView? = null
    private var detail: TextView? = null
    private var actions: LinearLayout? = null
    private var collapse: Runnable? = null
    private var telephony: TelephonyManager? = null
    private var phoneListener: PhoneStateListener? = null
    private var expanded = false

    private fun dp(v: Float)= (v*resources.displayMetrics.density).roundToInt()

    override fun onCreate(){
        super.onCreate(); createChannel(); startForeground(400,notification()); instance=this
        registerPhoneStateListener()
        if(Settings.canDrawOverlays(this)) showIsland() else stopSelf()
    }

    private fun registerPhoneStateListener(){
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE)==0){
            telephony=getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            phoneListener=object: PhoneStateListener(){
                override fun onCallStateChanged(state:Int, phoneNumber:String?){
                    when(state){
                        TelephonyManager.CALL_STATE_RINGING->showEvent("📞 Incoming call", phoneNumber?.takeIf{it.isNotBlank()} ?: "Phone is ringing", "call")
                        TelephonyManager.CALL_STATE_OFFHOOK->showEvent("📞 Call active","Tap to return to the call","call")
                        TelephonyManager.CALL_STATE_IDLE->collapseNow()
                    }
                }
            }
            try{telephony?.listen(phoneListener,PhoneStateListener.LISTEN_CALL_STATE)}catch(_:Exception){}
        }
    }

    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
        if(intent?.action=="SHOW_EVENT") showEvent(intent.getStringExtra("title")?:"Notification",intent.getStringExtra("detail")?:"","notification")
        return START_STICKY
    }

    private fun showIsland(){
        if(island!=null)return
        wm=getSystemService(WINDOW_SERVICE) as WindowManager
        val ctx=createDisplayContext(wm!!.defaultDisplay).createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,null)
        val p=getSharedPreferences("island_settings",MODE_PRIVATE)
        val w=p.getInt("width",162); val h=p.getInt("height",38); val x=p.getInt("x",0); val y=p.getInt("y",8)
        island=LinearLayout(ctx).apply{
            orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; setPadding(dp(14f),dp(5f),dp(14f),dp(5f))
            background=GradientDrawable().apply{setColor(Color.BLACK);cornerRadius=dp(30f).toFloat()}
            elevation=dp(10f).toFloat();isClickable=true
            setOnClickListener{toggleExpanded()};setOnLongClickListener{collapseNow();true}
        }
        title=TextView(ctx).apply{text="";textSize=14f;setTextColor(Color.WHITE);gravity=Gravity.CENTER;maxLines=1}
        detail=TextView(ctx).apply{textSize=11f;setTextColor(Color.LTGRAY);gravity=Gravity.CENTER;maxLines=1;visibility=View.GONE}
        actions=LinearLayout(ctx).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER;visibility=View.GONE}
        island!!.addView(title,LinearLayout.LayoutParams(-1,dp(24f)))
        island!!.addView(detail,LinearLayout.LayoutParams(-1,dp(20f)))
        island!!.addView(actions,LinearLayout.LayoutParams(-1,dp(38f)))
        val lp=WindowManager.LayoutParams(dp(w.toFloat()),dp(h.toFloat()),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,PixelFormat.TRANSLUCENT)
            .apply{gravity=Gravity.TOP or Gravity.CENTER_HORIZONTAL;x=dp(x.toFloat());y=dp(y.toFloat())}
        try{wm!!.addView(island,lp);island!!.scaleX=.94f;island!!.scaleY=.94f;island!!.animate().scaleX(1f).scaleY(1f).setDuration(220).start()}catch(_:Exception){island=null;stopSelf()}
    }

    private fun toggleExpanded(){if(expanded)collapseNow() else {expanded=true;detail?.visibility=View.VISIBLE;actions?.visibility=View.VISIBLE;if(title?.text.isNullOrBlank())title?.text="Dynamic Island";resize(dp(300f),dp(118f));scheduleCollapse()}}
    private fun resize(w:Int,h:Int){val lp=island?.layoutParams as? WindowManager.LayoutParams?:return;lp.width=w;lp.height=h;try{wm?.updateViewLayout(island,lp)}catch(_:Exception){}}
    private fun collapseNow(){collapse?.let{Handler(Looper.getMainLooper()).removeCallbacks(it)};expanded=false;detail?.visibility=View.GONE;actions?.visibility=View.GONE;title?.text="";val p=getSharedPreferences("island_settings",MODE_PRIVATE);resize(dp(p.getInt("width",162).toFloat()),dp(p.getInt("height",38).toFloat()))}
    private fun scheduleCollapse(){collapse?.let{Handler(Looper.getMainLooper()).removeCallbacks(it)};val r=Runnable{collapseNow()};collapse=r;Handler(Looper.getMainLooper()).postDelayed(r,6500)}

    fun showEvent(t:String,d:String,type:String="notification"){Handler(Looper.getMainLooper()).post{
        if(island==null&&Settings.canDrawOverlays(this))showIsland()
        title?.text=t.take(30);detail?.text=d.take(70);detail?.visibility=View.VISIBLE;expanded=true
        actions?.removeAllViews();actions?.visibility=View.GONE
        if(type=="media"){
            actions?.visibility=View.VISIBLE
            addAction("⏮"){IslandNotificationListener.mediaPrevious()}
            addAction("▶/⏸"){IslandNotificationListener.mediaPlayPause()}
            addAction("⏭"){IslandNotificationListener.mediaNext()}
        } else if(type=="call"){
            actions?.visibility=View.VISIBLE
            addAction("Open Phone"){openPhone()}
        }
        resize(dp(300f),if(actions?.visibility==View.VISIBLE)dp(118f) else dp(76f));scheduleCollapse()
    }}

    private fun addAction(label:String,onClick:()->Unit){val b=Button(island!!.context).apply{text=label;textSize=11f;setOnClickListener{onClick()};setAllCaps(false)}
        actions?.addView(b,LinearLayout.LayoutParams(0,dp(36f),1f))}
    private fun openPhone(){try{startActivity(Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}catch(_:Exception){}}

    private fun createChannel(){getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("island","Dynamic Island",NotificationManager.IMPORTANCE_LOW))}
    private fun notification():Notification=NotificationCompat.Builder(this,"island").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Dynamic Island is active").setContentText("Live events are enabled.").setOngoing(true).setCategory(Notification.CATEGORY_SERVICE).build()
    override fun onDestroy(){collapse?.let{Handler(Looper.getMainLooper()).removeCallbacks(it)};if(telephony!=null&&phoneListener!=null)try{telephony?.listen(phoneListener,PhoneStateListener.LISTEN_NONE)}catch(_:Exception){};island?.let{try{wm?.removeView(it)}catch(_:Exception){}};island=null;instance=null;super.onDestroy()}
    override fun onBind(intent:Intent?)=null
    companion object{var instance:IslandOverlayService?=null}
}