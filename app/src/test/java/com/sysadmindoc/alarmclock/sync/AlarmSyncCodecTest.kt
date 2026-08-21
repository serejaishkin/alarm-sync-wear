package com.sysadmindoc.alarmclock.sync

import com.sysadmindoc.alarmclock.data.model.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmSyncCodecTest {
    @Test
    fun roundTripKeepsSyncMetadataAndAlarm() {
        val alarm = Alarm(
            id = 42L,
            hour = 7,
            minute = 30,
            label = "Work",
            isEnabled = true
        )

        val payload = AlarmSyncCodec.create(
            alarm = alarm,
            syncId = "sync-42",
            operation = AlarmSyncOperation.UPDATE,
            source = AlarmSyncSource.PHONE,
            revision = 7L,
            timestamp = 1234L
        )

        val decoded = AlarmSyncCodec.decode(AlarmSyncCodec.encode(payload)).getOrThrow()
        assertEquals("sync-42", decoded.syncId)
        assertEquals(AlarmSyncOperation.UPDATE, decoded.operation)
        assertEquals(AlarmSyncSource.PHONE, decoded.source)
        assertEquals(7L, decoded.revision)
        assertEquals(1234L, decoded.timestamp)

        val decodedAlarm = AlarmSyncCodec.decodeAlarm(decoded).getOrThrow()
        assertEquals(alarm.hour, decodedAlarm.hour)
        assertEquals(alarm.minute, decodedAlarm.minute)
        assertEquals(alarm.label, decodedAlarm.label)
    }

    @Test
    fun deleteDoesNotCarryAlarmToken() {
        val payload = AlarmSyncCodec.create(
            alarm = Alarm(label = "Deleted"),
            syncId = "sync-delete",
            operation = AlarmSyncOperation.DELETE,
            source = AlarmSyncSource.WATCH,
            revision = 2L,
            timestamp = 99L
        )

        assertTrue(payload.alarmToken == null)
    }

    @Test
    fun rejectsUnsupportedProtocol() {
        val payload = AlarmSyncPayload(
            protocolVersion = 999,
            syncId = "sync-1",
            operation = AlarmSyncOperation.UPDATE,
            source = AlarmSyncSource.PHONE,
            revision = 1L,
            timestamp = 1L
        )

        val result = AlarmSyncCodec.decode(
            com.squareup.moshi.Moshi.Builder()
                .build()
                .adapter(AlarmSyncPayload::class.java)
                .toJson(payload)
        )

        assertTrue(result.isFailure)
    }
}
