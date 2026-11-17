package com.wakesync.app.service

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmPlaybackGainTest {

    @Test
    fun `call mute has priority over challenge ducking and ramp`() {
        assertEquals(0f, alarmPlaybackGain(true, true, 35, 0.8f), 0.0001f)
    }

    @Test
    fun `challenge ducking scales the current fade ramp`() {
        assertEquals(0.175f, alarmPlaybackGain(false, true, 35, 0.5f), 0.0001f)
        assertEquals(0.5f, alarmPlaybackGain(false, false, 35, 0.5f), 0.0001f)
    }

    @Test
    fun `fade in curve is monotonic and quiet early`() {
        assertEquals(0f, fadeInGain(0f), 0.0001f)
        assertEquals(1f, fadeInGain(1f), 0.0001f)
        org.junit.Assert.assertTrue(fadeInGain(0.1f) < 0.03f)
        org.junit.Assert.assertTrue(fadeInGain(0.5f) < 0.15f)
        var prev = 0f
        for (i in 1..50) {
            val g = fadeInGain(i / 50f)
            org.junit.Assert.assertTrue(g >= prev)
            prev = g
        }
    }

    @Test
    fun `challenge percent is bounded`() {
        assertEquals(0.1f, alarmPlaybackGain(false, true, 0, 1f), 0.0001f)
        assertEquals(0.8f, alarmPlaybackGain(false, true, 100, 1f), 0.0001f)
    }
}
