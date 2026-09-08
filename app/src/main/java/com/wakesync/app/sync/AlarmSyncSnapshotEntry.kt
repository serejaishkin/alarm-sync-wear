package com.wakesync.app.sync

import com.wakesync.app.data.model.Alarm

/** Stable identity and version metadata paired with an alarm in a complete peer snapshot. */
data class AlarmSyncSnapshotEntry(
    val alarm: Alarm,
    val syncId: String,
    val revision: Long,
    val updatedAt: Long,
    val source: AlarmSyncSource = AlarmSyncSource.PHONE,
    val originDeviceId: String = ""
)
