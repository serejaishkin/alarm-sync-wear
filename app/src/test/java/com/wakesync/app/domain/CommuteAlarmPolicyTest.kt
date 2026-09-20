package com.wakesync.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CommuteAlarmPolicyTest {
    @Test
    fun routeDurationAboveBaselineAddsExtraLead() {
        val adjustment = CommuteAlarmPolicy.adjustLeadMinutes(
            baseLeadMinutes = 45,
            baselineCommuteMinutes = 40,
            routeDurationMinutes = 70
        )

        assertEquals(75, adjustment.totalLeadMinutes)
        assertEquals(30, adjustment.routeExtraMinutes)
    }

    @Test
    fun slowerRouteThanBaselineAddsNoExtraLead() {
        val adjustment = CommuteAlarmPolicy.adjustLeadMinutes(
            baseLeadMinutes = 45,
            baselineCommuteMinutes = 40,
            routeDurationMinutes = 30
        )

        assertEquals(45, adjustment.totalLeadMinutes)
        assertEquals(0, adjustment.routeExtraMinutes)
    }

    @Test
    fun totalExtraLeadIsCapped() {
        val adjustment = CommuteAlarmPolicy.adjustLeadMinutes(
            baseLeadMinutes = 30,
            baselineCommuteMinutes = 30,
            routeDurationMinutes = 300
        )

        assertEquals(150, adjustment.totalLeadMinutes)
        assertEquals(120, adjustment.routeExtraMinutes)
    }
}