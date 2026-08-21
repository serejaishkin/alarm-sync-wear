package com.sysadmindoc.alarmclock.sync

/**
 * Transport-neutral envelope for one alarm synchronization mutation.
 *
 * alarmId is the current local Room id. syncId is the stable identity shared
 * by the same alarm on the phone and watch. Keeping those identities separate
 * avoids coupling sync to Room's local primary key.
 */
data class AlarmSyncEnvelope(
    val protocolVersion: Int = CURRENT_PROTOCOL_VERSION,
    val deviceId: String,
    val syncId: String,
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
