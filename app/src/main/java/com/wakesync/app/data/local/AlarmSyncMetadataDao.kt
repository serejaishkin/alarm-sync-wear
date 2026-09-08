package com.wakesync.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wakesync.app.data.local.entity.AlarmSyncMetadata
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmSyncMetadataDao {
    @Query("SELECT * FROM alarm_sync_metadata WHERE alarmId = :alarmId LIMIT 1")
    suspend fun getByAlarmId(alarmId: Long): AlarmSyncMetadata?

    @Query("SELECT * FROM alarm_sync_metadata WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): AlarmSyncMetadata?

    @Query("SELECT * FROM alarm_sync_metadata ORDER BY alarmId")
    fun observeAll(): Flow<List<AlarmSyncMetadata>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: AlarmSyncMetadata)

    @Query("DELETE FROM alarm_sync_metadata WHERE alarmId = :alarmId")
    suspend fun deleteByAlarmId(alarmId: Long)

    @Query("DELETE FROM alarm_sync_metadata WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)
}
