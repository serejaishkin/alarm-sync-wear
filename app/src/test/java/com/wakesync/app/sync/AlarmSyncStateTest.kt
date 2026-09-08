package com.wakesync.app.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmSyncStateTest {
    @Test
    fun generatedIdentityIsValid() {
        val id = AlarmSyncIdentity.newId()

        assertTrue(AlarmSyncIdentity.isValid(id))
        assertFalse(AlarmSyncIdentity.isValid("not-a-uuid"))
    }

    @Test
    fun nextRevisionKeepsIdentityAndAdvancesMetadata() {
        val id = AlarmSyncIdentity.newId()
        val state = AlarmSyncState(
            syncId = id,
            revision = 4L,
            updatedAt = 100L,
            updatedBy = AlarmSyncSource.PHONE
        )

        val next = state.next(AlarmSyncSource.WATCH, now = 200L)

        assertEquals(id, next.syncId)
        assertEquals(5L, next.revision)
        assertEquals(200L, next.updatedAt)
        assertEquals(AlarmSyncSource.WATCH, next.updatedBy)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidIdentityIsRejected() {
        AlarmSyncState(syncId = "invalid")
    }
}
