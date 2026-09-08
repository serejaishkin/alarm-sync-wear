package com.wakesync.app.sync

import java.util.UUID

/**
 * Stable identity for one logical alarm across phone and Wear OS.
 *
 * This is deliberately separate from Room's local Alarm.id.
 */
object AlarmSyncIdentity {
    fun newId(): String = UUID.randomUUID().toString()

    fun isValid(id: String): Boolean = runCatching {
        UUID.fromString(id)
    }.isSuccess
}
