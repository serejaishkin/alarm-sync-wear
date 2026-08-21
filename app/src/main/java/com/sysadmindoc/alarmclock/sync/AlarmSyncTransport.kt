package com.sysadmindoc.alarmclock.sync

/**
 * Transport-neutral boundary between sync logic and Phone <-> Watch transport.
 *
 * The first production implementation will use the Wear OS Data Layer in the
 * Play flavor. A direct BLE implementation can be added later without changing
 * the sync protocol or alarm domain code.
 */
interface AlarmSyncTransport {
    suspend fun send(envelope: AlarmSyncEnvelope): Result<Unit>
}
