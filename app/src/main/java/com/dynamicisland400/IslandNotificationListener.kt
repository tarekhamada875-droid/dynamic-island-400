package com.dynamicisland400

import android.app.Notification
import android.content.ComponentName
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class IslandNotificationListener : NotificationListenerService(){
    private var mediaManager: MediaSessionManager?=null

    override fun onCreate(){
        super.onCreate()
        mediaManager=getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
    }

    override fun onNotificationPosted(sbn:StatusBarNotification){
        if(sbn.packageName==packageName)return
        val n=sbn.notification
        val title=n.extras.getString(Notification.EXTRA_TITLE)?.takeIf{it.isNotBlank()}?:return
        val text=n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.takeIf{it.isNotBlank()}?:""
        val type=if(n.category==Notification.CATEGORY_TRANSPORT) "media" else "notification"
        IslandOverlayService.instance?.showEvent(title,text,type)
        refreshController()
    }

    override fun onListenerConnected(){
        lastService=this
        refreshController()
    }

    override fun onListenerDisconnected(){
        if(lastService===this){lastService=null;controller=null}
    }

    private fun refreshController(){
        try{
            val sessions=mediaManager?.getActiveSessions(ComponentName(this,IslandNotificationListener::class.java)).orEmpty()
            controller=sessions.firstOrNull()
        }catch(_:Exception){}
    }

    companion object{
        private var lastService:IslandNotificationListener?=null
        private var controller:MediaController?=null
        fun mediaPlayPause(){
            val c=controller?:return
            if(c.playbackState?.state==android.media.session.PlaybackState.STATE_PLAYING)c.transportControls.pause()
            else c.transportControls.play()
        }
        fun mediaNext(){controller?.transportControls?.skipToNext()}
        fun mediaPrevious(){controller?.transportControls?.skipToPrevious()}
    }
}