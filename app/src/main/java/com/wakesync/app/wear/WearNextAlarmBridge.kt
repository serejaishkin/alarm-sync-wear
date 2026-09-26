package com.wakesync.app.wear

import com.wakesync.app.data.model.Alarm

/**
 * Wear OS bridge.
 *
 * Publishes next-alarm state to Wear OS through the Data Layer so the watch
 * mirrors the phone. This is a hard dependency of every flavor: the watch
 * module is useless without a publisher, and a no-op implementation here
 * leaves the watch permanently out of sync.
 */
interface WearNextAlarmBridge {
    fun start()
    fun stop()
    fun publishAlarmFiring(alarm: Alarm)
    fun publishAlarmIdle(alarmId: Long)
}
