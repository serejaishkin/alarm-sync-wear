package com.wakesync.app.sync

/**
 * A complete synchronization change for one logical alarm.
 *
 * The payload is intentionally opaque here. The alarm model must not depend
 * on a specific transport or serialization library.
 */
data class AlarmSyncChange(
    val envelope: AlarmSyncEnvelope,
    val payload: String? = envelope.payload
) {
    init {
        require(envelope.syncId.isNotBlank()) { "Sync id must not be blank" }
        require(envelope.deviceId.isNotBlank()) { "Device id must not be blank" }
        require(envelope.revision >= 0) { "Revision must not be negative" }
    }
}

/**
 * Resolves competing changes deterministically.
 *
 * Revision is authoritative. Timestamp is only the deterministic tie-breaker.
 */
object AlarmSyncConflictResolver {
    fun choose(
        local: AlarmSyncEnvelope,
        incoming: AlarmSyncEnvelope
    ): AlarmSyncEnvelope = when {
        incoming.isNewerThan(local) -> incoming
        else -> local
    }
}
