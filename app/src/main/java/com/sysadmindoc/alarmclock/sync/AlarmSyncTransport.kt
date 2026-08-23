package com.sysadmindoc.alarmclock.sync

import com.sysadmindoc.alarmclock.data.model.Alarm

/** Transport-neutral boundary between sync logic and Phone <-> Watch transport. */
interface AlarmSyncTransport {
    suspend fun send(envelope: AlarmSyncEnvelope): Result<Unit>

    /** Publishes the complete logical alarm collection for Wear list/resync. */
    suspend fun publishSnapshot(alarms: List<Alarm>): Result<Unit> = Result.success(Unit)
}
