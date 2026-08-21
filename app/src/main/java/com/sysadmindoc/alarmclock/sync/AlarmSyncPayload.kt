package com.sysadmindoc.alarmclock.sync

import com.squareup.moshi.JsonClass

/**
 * Wire payload for one alarm mutation.
 *
 * The alarm itself is represented by the existing AlarmShareCodec token so
 * WakeSync does not create a second Alarm serialization format.
 */
@JsonClass(generateAdapter = true)
data class AlarmSyncPayload(
    val protocolVersion: Int = AlarmSyncEnvelope.CURRENT_PROTOCOL_VERSION,
    val syncId: String,
    val operation: AlarmSyncOperation,
    val source: AlarmSyncSource,
    val revision: Long,
    val timestamp: Long,
    val alarmToken: String? = null
)
