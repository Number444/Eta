package io.github.mangi.eta.ui.components

import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TurnTimingFormatTest {

    @Test
    fun durationUnderOneMinuteKeepsOneDecimalSecond() {
        assertEquals("0.0s", formatTurnDuration(0))
        assertEquals("8.6s", formatTurnDuration(8_640))
        assertEquals("59.9s", formatTurnDuration(59_949))
    }

    @Test
    fun durationOverOneMinuteUsesMinutesAndSeconds() {
        assertEquals("1m 0s", formatTurnDuration(60_000))
        assertEquals("2m 5s", formatTurnDuration(125_000))
        assertEquals("61m 0s", formatTurnDuration(3_660_000))
    }

    @Test
    fun finishTimeFollowsClockPreference() {
        val defaultZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"))
        try {
            // 2026-10-14 14:03:05 GMT+8
            val millis = 1_791_957_785_000L
            assertEquals("14:03", formatTurnFinishTime(millis, use24Hour = true))
            val twelveHour = formatTurnFinishTime(millis, use24Hour = false)
            assertTrue(twelveHour.startsWith("2:03"))
        } finally {
            TimeZone.setDefault(defaultZone)
        }
    }
}
