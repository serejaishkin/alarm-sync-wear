package com.sysadmindoc.alarmclock.sync

import com.squareup.moshi.JsonClass

/** Wire payload for one synchronized alarm mutation. */
@JsonClass(generateAdapter = true)
data class AlarmSyncPayload(
    val protocolVersion: Int = AlarmSyncEnvelope.CURRENT_PROTOCOL_VERSION,
    val syncId: String,
    val operation: AlarmSyncOperation,
    val source: AlarmSyncSource,
    val revision: Long,
    val timestamp: Long,
    val alarmToken: String? = null,
    // Lightweight fields let Wear render/edit the shared alarm list without
    // understanding the phone-only AlarmShareCodec token.
    val hour: Int? = null,
    val minute: Int? = null,
    val label: String? = null,
    val enabled: Boolean? = null,
    val repeatDays: List<Int> = emptyList(),
    val snoozeDurationMinutes: Int = 10,
    val vibrationEnabled: Boolean = true,
    val volume: Int = 100
)
