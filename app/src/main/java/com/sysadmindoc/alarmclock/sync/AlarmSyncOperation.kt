package com.sysadmindoc.alarmclock.sync

/**
 * Operations exchanged between the phone and Wear OS companion.
 *
 * The sync protocol is intentionally independent from the transport layer.
 */
enum class AlarmSyncOperation {
    CREATE,
    UPDATE,
    ENABLE,
    DISABLE,
    DELETE,
    SNOOZE,
    DISMISS,
    RINGING
}

/**
 * Identifies the device that produced a sync mutation.
 */
enum class AlarmSyncSource {
    PHONE,
    WATCH
}
