package com.sysadmindoc.alarmclock.sync

/**
 * Transport-neutral envelope for one alarm synchronization mutation.
 *
 * alarmId is the current local Room id. A future stable syncId will identify
 * the same alarm across devices; keeping that identity separate from Room's
 * local primary key avoids coupling sync to the existing database schema.
 */
data class AlarmSyncEnvelope(
    val protocolVersion: Int = CURRENT_PROTOCOL_VERSION,
    val deviceId: String,
    val alarmId: Long,
    val operation: AlarmSyncOperation,
    val source: AlarmSyncSource,
    val revision: Long,
    val timestamp: Long,
    val payload: String? = null
) {
    companion object {
        const val CURRENT_PROTOCOL_VERSION = 1
    }

    fun isNewerThan(other: AlarmSyncEnvelope): Boolean = when {
        revision != other.revision -> revision > other.revision
        timestamp != other.timestamp -> timestamp > other.timestamp
        else -> false
    }
}
