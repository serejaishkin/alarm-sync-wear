package com.wakesync.app.service

import com.wakesync.app.data.model.Alarm

internal data class AlarmFlashlightStrobePlan(
    val onMillis: Long = 200L,
    val offMillis: Long = 300L
)

internal object AlarmFlashlightController {
    fun strobePlan(alarm: Alarm, flashingAllowed: Boolean = true): AlarmFlashlightStrobePlan? {
        return if (alarm.flashlightStrobe && flashingAllowed) AlarmFlashlightStrobePlan() else null
    }
}
