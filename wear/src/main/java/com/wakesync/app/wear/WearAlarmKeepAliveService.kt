package com.wakesync.app.wear

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

/**
 * Safe stub. Certified Wear OS uses AlarmManager.setExactAndAllowWhileIdle
 * to wake the device at alarm time without requiring a battery-draining
 * 24/7 background service.
 */
class WearAlarmKeepAliveService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun refresh(context: Context) {
            // No-op on Wear OS: alarms wake via AlarmManager RTC_WAKEUP
        }
    }
}
