package com.wakesync.app.data.repository

import com.wakesync.app.data.local.AlarmSyncMetadataDao
import com.wakesync.app.data.local.entity.AlarmSyncMetadata
import com.wakesync.app.sync.AlarmSyncIdentity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmSyncMetadataRepository @Inject constructor(
    private val dao: AlarmSyncMetadataDao
) {
    suspend fun getByAlarmId(alarmId: Long): AlarmSyncMetadata? = dao.getByAlarmId(alarmId)

    suspend fun getBySyncId(syncId: String): AlarmSyncMetadata? = dao.getBySyncId(syncId)

    /**
     * Creates the stable identity for a newly-created local alarm.
     */
    suspend fun createForAlarm(
        alarmId: Long,
        now: Long = System.currentTimeMillis(),
        deviceId: String = ""
    ): AlarmSyncMetadata {
        val metadata = AlarmSyncMetadata(
            alarmId = alarmId,
            syncId = AlarmSyncIdentity.newId(),
            revision = 1L,
            updatedAt = now,
            updatedBy = deviceId
        )
        dao.upsert(metadata)
        return metadata
    }

    suspend fun upsert(metadata: AlarmSyncMetadata) = dao.upsert(metadata)

    suspend fun deleteForAlarm(alarmId: Long) = dao.deleteByAlarmId(alarmId)
}
