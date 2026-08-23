package com.sysadmindoc.alarmclock.sync

import com.squareup.moshi.Moshi
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.share.AlarmShareCodec

/** Encodes and decodes the transport-neutral WakeSync payload. */
object AlarmSyncCodec {
    private const val MAX_PAYLOAD_LENGTH = 32 * 1024

    private val adapter = Moshi.Builder()
        .build()
        .adapter(AlarmSyncPayload::class.java)

    fun encode(payload: AlarmSyncPayload): String {
        require(payload.syncId.isNotBlank()) { "syncId must not be blank" }
        require(payload.revision >= 0L) { "revision must not be negative" }
        return adapter.toJson(payload)
    }

    fun decode(encoded: String): Result<AlarmSyncPayload> = runCatching {
        require(encoded.isNotBlank()) { "Empty sync payload" }
        require(encoded.length <= MAX_PAYLOAD_LENGTH) { "Sync payload exceeds maximum size" }
        val payload = adapter.fromJson(encoded)
            ?: throw IllegalArgumentException("Invalid sync payload")
        require(payload.protocolVersion == AlarmSyncEnvelope.CURRENT_PROTOCOL_VERSION) {
            "Unsupported sync protocol ${payload.protocolVersion}"
        }
        require(payload.syncId.isNotBlank()) { "Sync payload has no syncId" }
        require(payload.revision >= 0L) { "Sync payload has invalid revision" }
        payload
    }

    fun create(
        alarm: Alarm,
        syncId: String,
        operation: AlarmSyncOperation,
        source: AlarmSyncSource,
        revision: Long,
        timestamp: Long = System.currentTimeMillis()
    ): AlarmSyncPayload = AlarmSyncPayload(
        syncId = syncId,
        operation = operation,
        source = source,
        revision = revision,
        timestamp = timestamp,
        alarmToken = when (operation) {
            AlarmSyncOperation.DELETE -> null
            else -> AlarmShareCodec.encodeToken(alarm)
        },
        hour = alarm.hour,
        minute = alarm.minute,
        label = alarm.label,
        enabled = when (operation) {
            AlarmSyncOperation.ENABLE -> true
            AlarmSyncOperation.DISABLE -> false
            else -> alarm.isEnabled
        }
    )

    fun decodeAlarm(payload: AlarmSyncPayload): Result<Alarm> = runCatching {
        val token = payload.alarmToken
            ?: throw IllegalArgumentException("Sync operation has no alarm payload")
        AlarmShareCodec.decodeToken(token).getOrThrow()
    }
}
