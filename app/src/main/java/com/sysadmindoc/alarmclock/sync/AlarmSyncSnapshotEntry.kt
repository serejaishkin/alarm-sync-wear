package com.sysadmindoc.alarmclock.sync

import com.sysadmindoc.alarmclock.data.model.Alarm

/** Stable identity paired with an alarm when publishing a complete peer snapshot. */
data class AlarmSyncSnapshotEntry(
    val alarm: Alarm,
    val syncId: String,
    val revision: Long
)
