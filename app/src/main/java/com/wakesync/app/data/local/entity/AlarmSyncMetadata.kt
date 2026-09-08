package com.wakesync.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Persistent synchronization metadata kept separate from Alarm's local Room id.
 *
 * One row represents the identity of one logical alarm on this device. The same
 * syncId is used by the phone and watch; alarmId remains device-local.
 */
@Entity(
    tableName = "alarm_sync_metadata",
    primaryKeys = ["alarmId"],
    indices = [Index(value = ["syncId"], unique = true)]
)
data class AlarmSyncMetadata(
    val alarmId: Long,
    val syncId: String,
    val revision: Long = 0L,
    val updatedAt: Long = 0L,
    val updatedBy: String = ""
)
