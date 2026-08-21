package com.sysadmindoc.alarmclock.sync

import com.sysadmindoc.alarmclock.data.local.AlarmSyncMetadataDao
import com.sysadmindoc.alarmclock.data.local.entity.AlarmSyncMetadata
import java.util.UUID

/**
 * Creates the stable cross-device identity for an alarm without changing the
 * existing local Room primary key.
 *
 * This class intentionally has no DI annotation yet. The DAO becomes a Room
 * database dependency in the next storage integration block.
 */
class AlarmSyncMetadataProvisioner(
    private val dao: AlarmSyncMetadataDao
) {
    suspend fun getOrCreate(alarmId: Long): AlarmSyncMetadata {
        require(alarmId > 0L) { "alarmId must be a persisted Room id" }
        dao.getByAlarmId(alarmId)?.let { return it }

        val metadata = AlarmSyncMetadata(
            alarmId = alarmId,
            syncId = UUID.randomUUID().toString(),
            revision = 0L,
            updatedAt = System.currentTimeMillis(),
            updatedBy = ""
        )
        dao.upsert(metadata)
        return metadata
    }
}
