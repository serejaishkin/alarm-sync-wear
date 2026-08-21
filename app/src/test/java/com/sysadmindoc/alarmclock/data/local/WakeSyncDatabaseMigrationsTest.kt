package com.sysadmindoc.alarmclock.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeSyncDatabaseMigrationsTest {
    @Test
    fun migrationTargetVersionIsTwentyFour() {
        assertEquals(24, WakeSyncDatabaseMigrations.TARGET_VERSION)
        assertEquals(23, WakeSyncDatabaseMigrations.MIGRATION_23_24.startVersion)
        assertEquals(24, WakeSyncDatabaseMigrations.MIGRATION_23_24.endVersion)
    }

    @Test
    fun migrationHasStableContract() {
        assertTrue(WakeSyncDatabaseMigrations.MIGRATION_23_24.startVersion <
            WakeSyncDatabaseMigrations.MIGRATION_23_24.endVersion)
    }
}
