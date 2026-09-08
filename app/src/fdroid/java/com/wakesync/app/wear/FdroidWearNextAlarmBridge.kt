package com.wakesync.app.wear

import com.wakesync.app.data.model.Alarm
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FdroidWearNextAlarmBridge @Inject constructor() : WearNextAlarmBridge {
    override fun start() { /* no-op */ }
    override fun stop() { /* no-op */ }
    override fun publishAlarmFiring(alarm: Alarm) { /* no-op */ }
    override fun publishAlarmIdle(alarmId: Long) { /* no-op */ }
}
