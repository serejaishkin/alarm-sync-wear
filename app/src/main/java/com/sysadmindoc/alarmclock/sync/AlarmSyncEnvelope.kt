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

    /**
     * Compares two versions of the same logical alarm deterministically.
     *
     * Revision is the primary version. Timestamp resolves equal revisions,
     * which is useful when phone and watch create competing changes while
     * temporarily disconnected. Source is the final deterministic tie-breaker
     * so both devices always choose the same winner even when every other
     * value is identical.
     */
    fun isNewerThan(other: AlarmSyncEnvelope): Boolean {
        require(syncId == other.syncId) { "Cannot compare different alarms" }

        return compareVersion(other) > 0
    }

    /**
     * Returns a positive value when this envelope wins over [other], zero when
     * they are equivalent, and a negative value when [other] wins.
     */
    fun compareVersion(other: AlarmSyncEnvelope): Int {
        require(syncId == other.syncId) { "Cannot compare different alarms" }

        return when {
            timestamp != other.timestamp -> timestamp.compareTo(other.timestamp)
            revision != other.revision -> revision.compareTo(other.revision)
            source != other.source -> sourcePriority(source).compareTo(sourcePriority(other.source))
            deviceId != other.deviceId -> deviceId.compareTo(other.deviceId)
            else -> 0
        }
    }

    private fun sourcePriority(source: AlarmSyncSource): Int = when (source) {
        AlarmSyncSource.PHONE -> 1
        AlarmSyncSource.WATCH -> 2
    }
}
