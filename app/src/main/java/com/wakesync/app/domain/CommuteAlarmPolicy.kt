package com.wakesync.app.domain

data class CommuteAlarmAdjustment(
    val totalLeadMinutes: Int,
    val routeExtraMinutes: Int
)

object CommuteAlarmPolicy {
    private const val MAX_EXTRA_MINUTES = 120

    fun adjustLeadMinutes(
        baseLeadMinutes: Int,
        baselineCommuteMinutes: Int,
        routeDurationMinutes: Int?
    ): CommuteAlarmAdjustment {
        val baseLead = baseLeadMinutes.coerceIn(0, 720)
        val baseline = baselineCommuteMinutes.coerceIn(0, 240).takeIf { it > 0 } ?: baseLead
        val routeExtra = routeDurationMinutes
            ?.coerceAtLeast(0)
            ?.minus(baseline)
            ?.coerceAtLeast(0)
            ?: 0
        val cappedExtra = routeExtra.coerceAtMost(MAX_EXTRA_MINUTES)
        return CommuteAlarmAdjustment(
            totalLeadMinutes = baseLead + cappedExtra,
            routeExtraMinutes = routeExtra.coerceAtMost(MAX_EXTRA_MINUTES)
        )
    }
}