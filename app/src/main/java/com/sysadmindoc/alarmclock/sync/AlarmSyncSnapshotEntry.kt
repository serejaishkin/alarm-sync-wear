package com.sysadmindoc.alarmclock.sync

import com.sysadmindoc.alarmclock.data.model.Alarm

/** Stable identity paired with an alarm when publishing a complete peer snapshot. */
data class AlarmSyncSnapshotEntry(
    val alarm: Alarm,
    val syncId: String,
    val revision: Long,
    /**
     * Real last-change instant for this syncId, as tracked by
     * AlarmSyncCoordinator. Must NOT be regenerated at publish time —
     * periodic/reconciliation snapshots republish every alarm on every tick,
     * and if the transport stamps "now" on each of them, an unrelated
     * alarm's snapshot entry can look "newer" than the peer's real edit with
     * the same revision, which can make the peer's more recent local change
     * lose a tie-break it should have won.
     */
    val updatedAt: Long
)
