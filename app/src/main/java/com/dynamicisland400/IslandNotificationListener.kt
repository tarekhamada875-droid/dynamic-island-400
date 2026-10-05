package com.dynamicisland400

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class IslandNotificationListener : NotificationListenerService(){
    override fun onNotificationPosted(sbn:StatusBarNotification){
        if(sbn.packageName==packageName)return
        val n=sbn.notification
        val title=n.extras.getString(Notification.EXTRA_TITLE)?.takeIf{it.isNotBlank()}?:return
        val text=n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.takeIf{it.isNotBlank()}?:""
        val type=if(n.category==Notification.CATEGORY_TRANSPORT) "media" else "notification"
        IslandOverlayService.instance?.showEvent(title,text,type)
    }
    companion object{
        var lastService:IslandNotificationListener?=null
        fun mediaPlayPause(){ }
        fun mediaNext(){ }
        fun mediaPrevious(){ }
    }
    override fun onListenerConnected(){lastService=this}
    override fun onListenerDisconnected(){if(lastService===this)lastService=null}
}