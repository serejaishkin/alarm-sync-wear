package com.sysadmindoc.alarmclock.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AlarmSyncChangeTest {
    @Test
    fun newIdsAreUniqueAndValid() {
        val first = AlarmSyncIdentity.newId()
        val second = AlarmSyncIdentity.newId()

        assertNotEquals(first, second)
        assertEquals(true, AlarmSyncIdentity.isValid(first))
        assertEquals(true, AlarmSyncIdentity.isValid(second))
    }

    @Test
    fun higherRevisionWins() {
        val local = envelope(revision = 4, timestamp = 100)
        val incoming = envelope(revision = 5, timestamp = 1)

        assertEquals(incoming, AlarmSyncConflictResolver.choose(local, incoming))
    }

    @Test
    fun timestampBreaksRevisionTie() {
        val local = envelope(revision = 4, timestamp = 100)
        val incoming = envelope(revision = 4, timestamp = 101)

        assertEquals(incoming, AlarmSyncConflictResolver.choose(local, incoming))
    }

    @Test
    fun olderChangeCannotOverwriteLocalState() {
        val local = envelope(revision = 5, timestamp = 100)
        val incoming = envelope(revision = 4, timestamp = 999)

        assertEquals(local, AlarmSyncConflictResolver.choose(local, incoming))
    }

    private fun envelope(revision: Long, timestamp: Long) = AlarmSyncEnvelope(
        deviceId = "test-device",
        syncId = "550e8400-e29b-41d4-a716-446655440000",
        alarmId = 1L,
        operation = AlarmSyncOperation.UPDATE,
        source = AlarmSyncSource.PHONE,
        revision = revision,
        timestamp = timestamp
    )
}
