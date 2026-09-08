package com.wakesync.app.sync

import com.wakesync.app.data.model.Alarm

/** Transport-neutral boundary between sync logic and Phone <-> Watch transport. */
interface AlarmSyncTransport {
    suspend fun send(envelope: AlarmSyncEnvelope): Result<Unit>

    /** Ask the Wear peer to return its complete durable alarm state. */
    fun requestWatchSnapshot(): Result<Unit> = Result.success(Unit)

    /** Legacy next-alarm publication hook. */
    suspend fun publishSnapshot(alarms: List<Alarm>): Result<Unit> = Result.success(Unit)

    /** Complete collection publication with the real stable sync IDs. */
    suspend fun publishFullSnapshot(entries: List<AlarmSyncSnapshotEntry>): Result<Unit> =
        publishSnapshot(entries.map { it.alarm })
}
