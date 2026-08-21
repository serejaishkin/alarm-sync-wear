package com.sysadmindoc.alarmclock.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * WakeSync database migrations are kept separate from the legacy alarm schema
 * while the integration is being staged. This migration creates persistent
 * identity metadata without changing the existing alarms primary key.
 */
object WakeSyncDatabaseMigrations {
    const val TARGET_VERSION = 24

    val MIGRATION_23_24 = object : Migration(23, TARGET_VERSION) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS alarm_sync_metadata (
                    alarmId INTEGER NOT NULL,
                    syncId TEXT NOT NULL,
                    revision INTEGER NOT NULL DEFAULT 1,
                    updatedAt INTEGER NOT NULL DEFAULT 0,
                    updatedBy TEXT NOT NULL DEFAULT 'MIGRATION',
                    PRIMARY KEY(alarmId),
                    FOREIGN KEY(alarmId) REFERENCES alarms(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_alarm_sync_metadata_syncId " +
                    "ON alarm_sync_metadata(syncId)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_alarm_sync_metadata_alarmId " +
                    "ON alarm_sync_metadata(alarmId)"
            )

            // Existing alarms need a stable identity before the first sync.
            // SQLite does not provide UUID(), so generate UUID-shaped values
            // from random blobs. New alarms will use the app-side UUID source.
            db.execSQL(
                """
                INSERT INTO alarm_sync_metadata(alarmId, syncId, revision, updatedAt, updatedBy)
                SELECT
                    id,
                    lower(
                        hex(randomblob(4)) || '-' ||
                        hex(randomblob(2)) || '-4' ||
                        substr(hex(randomblob(2)), 2) || '-' ||
                        substr('89ab', abs(random()) % 4 + 1, 1) ||
                        substr(hex(randomblob(2)), 2) || '-' ||
                        hex(randomblob(6))
                    ),
                    1,
                    createdAt,
                    'MIGRATION'
                FROM alarms
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM alarm_sync_metadata metadata
                    WHERE metadata.alarmId = alarms.id
                )
                """.trimIndent()
            )
        }
    }
}
