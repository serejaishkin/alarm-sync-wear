package com.wakesync.app.ui.alarmlist

import com.wakesync.app.data.model.Alarm
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

class AlarmListReorderTest {

    @Test
    fun manualSortUsesPersistedOrderBeforeClockTime() {
        val alarms = listOf(
            Alarm(id = 1L, hour = 9, minute = 0, sortOrder = 3_000),
            Alarm(id = 2L, hour = 7, minute = 30, sortOrder = 1_000),
            Alarm(id = 3L, hour = 6, minute = 45, sortOrder = 2_000),
            Alarm(id = 4L, hour = 5, minute = 15, sortOrder = 2_000)
        )

        val sorted = sortAlarmsForList(alarms, AlarmSortOrder.MANUAL)

        assertEquals(listOf(2L, 4L, 3L, 1L), sorted.map { it.id })
    }

    @Test
    fun reorderAlarmIdsMovesDraggedIdBeforeTarget() {
        val visibleIds = listOf(10L, 20L, 30L, 40L)

        assertEquals(
            listOf(10L, 30L, 20L, 40L),
            reorderAlarmIds(visibleIds, movedId = 30L, targetId = 20L)
        )
        assertEquals(
            listOf(10L, 30L, 40L, 20L),
            reorderAlarmIds(visibleIds, movedId = 20L, targetId = 40L)
        )
    }

    @Test
    fun reorderAlarmIdsIgnoresMissingIdsAndNoOps() {
        val visibleIds = listOf(10L, 20L, 30L)

        assertEquals(visibleIds, reorderAlarmIds(visibleIds, movedId = 99L, targetId = 20L))
        assertEquals(visibleIds, reorderAlarmIds(visibleIds, movedId = 20L, targetId = 99L))
        assertEquals(visibleIds, reorderAlarmIds(visibleIds, movedId = 20L, targetId = 20L))
    }

    @Test
    fun findDuplicateExtrasKeepsLowestIdOfEachDuplicateGroup() {
        val ids = 1L..6L
        val alarms = listOf(
            alarm(1L, 6, 0, "Wake Up"),
            alarm(2L, 6, 0, "Wake Up"),
            alarm(3L, 6, 0, "Wake Up"),
            alarm(4L, 8, 45, "Morning"),
            alarm(5L, 7, 30, "Gym"),
            alarm(6L, 7, 30, "Gym")
        )

        val extras = findDuplicateExtras(alarms)

        assertEquals(listOf(2L, 3L, 6L), extras.map { it.id })
    }

    @Test
    fun findDuplicateExtrasIgnoresDifferentTimeDaysOrLabel() {
        val alarms = listOf(
            alarm(1L, 6, 0, "Wake Up"),
            alarm(2L, 6, 0, "Wake up "),
            alarm(3L, 6, 1, "Wake Up"),
            alarm(4L, 6, 0, "Gym"),
            alarm(5L, 6, 0, "Wake Up", days = setOf(DayOfWeek.MONDAY)),
            alarm(6L, 6, 0, "Wake Up", days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
        )

        val extras = findDuplicateExtras(alarms)

        assertEquals(listOf(2L), extras.map { it.id })
    }

    @Test
    fun findDuplicateExtrasHandlesEmptyList() {
        assertEquals(emptyList<Alarm>(), findDuplicateExtras(emptyList()))
    }

    private fun alarm(
        id: Long,
        hour: Int,
        minute: Int,
        label: String,
        days: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY)
    ) = Alarm(id = id, hour = hour, minute = minute, label = label, repeatDays = days)
}
