package com.sysadmindoc.alarmclock.sync

import com.sysadmindoc.alarmclock.data.model.Alarm

/**
 * Transport-neutral boundary between sync logic and Phone <-> Watch transport.
 *
 * Play builds use the Wear OS Data Layer. The core module stays independent of
 * Google Play Services and can still build without a Wear transport.
 */
interface AlarmSyncTransport {
    suspend fun send(envelope: AlarmSyncEnvelope): Result<Unit>

    /** Publish the small UI snapshot consumed by Tile/complication. */
    suspend fun publishSnapshot(alarm: Alarm?): Result<Unit> = Result.success(Unit)
}
