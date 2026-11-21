package com.wakesync.app.ui.clockimport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClockImportParserTest {

    @Test
    fun `parses common 24 hour forms`() {
        assertEquals(7 to 30, ClockImportParser.parseTime("7:30"))
        assertEquals(7 to 30, ClockImportParser.parseTime("07:30"))
        assertEquals(7 to 30, ClockImportParser.parseTime("07.30"))
        assertEquals(7 to 30, ClockImportParser.parseTime("730"))
        assertEquals(7 to 30, ClockImportParser.parseTime("0730"))
        assertEquals(23 to 59, ClockImportParser.parseTime("2359"))
        assertEquals(0 to 0, ClockImportParser.parseTime("00:00"))
    }

    @Test
    fun `hour only means top of the hour`() {
        assertEquals(7 to 0, ClockImportParser.parseTime("7"))
        assertEquals(18 to 0, ClockImportParser.parseTime("18"))
    }

    @Test
    fun `parses twelve hour forms`() {
        assertEquals(7 to 30, ClockImportParser.parseTime("7:30 am"))
        assertEquals(19 to 30, ClockImportParser.parseTime("7:30 PM"))
        assertEquals(0 to 15, ClockImportParser.parseTime("12:15am"))
        assertEquals(12 to 0, ClockImportParser.parseTime("12pm"))
    }

    @Test
    fun `rejects invalid input`() {
        assertNull(ClockImportParser.parseTime(""))
        assertNull(ClockImportParser.parseTime("   "))
        assertNull(ClockImportParser.parseTime("24:00"))
        assertNull(ClockImportParser.parseTime("7:60"))
        assertNull(ClockImportParser.parseTime("abc"))
        assertNull(ClockImportParser.parseTime("12345"))
        assertNull(ClockImportParser.parseTime("13pm"))
        assertNull(ClockImportParser.parseTime("0am"))
        assertNull(ClockImportParser.parseTime("am"))
    }
}
