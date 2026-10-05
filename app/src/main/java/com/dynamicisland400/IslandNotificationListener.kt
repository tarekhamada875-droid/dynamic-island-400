package com.dynamicisland400

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class IslandNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val n=sbn.notification
        val title=n.extras.getString(Notification.EXTRA_TITLE)?.takeIf{it.isNotBlank()} ?: return
        val text=n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.takeIf{it.isNotBlank()} ?: ""
        val serviceIntent=android.content.Intent(this, IslandOverlayService::class.java).apply {
            action="SHOW_EVENT"
            putExtra("title",title)
            putExtra("detail",text)
        }
        try { startService(serviceIntent) } catch(_:Exception) {}
    }
}