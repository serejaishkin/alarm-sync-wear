package com.sysadmindoc.alarmclock.wear

object WearAlarmData {
    const val PATH_NEXT_ALARM = "/alarms/next"
    const val PATH_LEGACY_NEXT_ALARM = "/wakesync/next_alarm"

    const val PATH_ACTION_SKIP = "/wakesync/action/skip"
    const val PATH_ACTION_SNOOZE = "/wakesync/action/snooze"
    const val PATH_ACTION_DISMISS = "/wakesync/action/dismiss"
    const val PATH_ACTION_DISABLE = "/wakesync/action/disable"

    const val PATH_WAKESYNC_ACTION_SKIP = "/wakesync/action/skip"
    const val PATH_WAKESYNC_ACTION_SNOOZE = "/wakesync/action/snooze"
    const val PATH_WAKESYNC_ACTION_DISMISS = "/wakesync/action/dismiss"
    const val PATH_WAKESYNC_ACTION_DISABLE = "/wakesync/action/disable"

    const val KEY_HAS_ALARM = "has_alarm"
    const val KEY_ALARM_ID = "alarm_id"
    const val KEY_LABEL = "label"
    const val KEY_TIME_LABEL = "time_label"
    const val KEY_TRIGGER_TIME = "trigger_time"
    const val KEY_IS_FIRING = "is_firing"
    const val KEY_UPDATED_AT = "updated_at"
    const val KEY_TIMEZONE_POLICY = "timezone_policy"
    const val KEY_FIXED_TIMEZONE_ID = "fixed_timezone_id"
}
