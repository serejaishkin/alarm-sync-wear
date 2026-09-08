package com.wakesync.app.sync

/**
 * Local synchronization metadata for one logical alarm.
 *
 * This is intentionally not a Room entity yet. The first integration step
 * keeps WakeSync's existing Alarm entity and migrations untouched.
 * Persistence will be added as a dedicated table after the sync contract is
 * covered by tests.
 */
data class AlarmSyncState(
    val syncId: String,
    val revision: Long = 0L,
    val updatedAt: Long = 0L,
    val updatedBy: AlarmSyncSource = AlarmSyncSource.PHONE
) {
    init {
        require(AlarmSyncIdentity.isValid(syncId)) { "syncId must be a UUID" }
        require(revision >= 0L) { "revision must be non-negative" }
        require(updatedAt >= 0L) { "updatedAt must be non-negative" }
    }

    fun next(source: AlarmSyncSource, now: Long = System.currentTimeMillis()): AlarmSyncState =
        copy(
            revision = revision + 1L,
            updatedAt = now,
            updatedBy = source
        )
}
