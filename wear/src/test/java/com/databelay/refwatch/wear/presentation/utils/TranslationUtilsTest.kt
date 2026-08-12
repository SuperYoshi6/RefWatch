package com.databelay.refwatch.wear.presentation.utils

import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.GenericLogEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class TranslationUtilsTest {
    @Test
    fun firstHalfMinutesAreDisplayedAsTheActualMatchMinute() {
        val event = GenericLogEvent(
            message = "test",
            gameTimeMillis = (24 * 60 * 1000L).toDouble(),
            phase = GamePhase.FIRST_HALF
        )

        assertEquals("24'", event.getMatchMinute(45))
    }

    @Test
    fun secondHalfMinutesContinueFromTheHalfTimeBase() {
        val event = GenericLogEvent(
            message = "test",
            gameTimeMillis = (27 * 60 * 1000L).toDouble(),
            phase = GamePhase.SECOND_HALF
        )

        assertEquals("72'", event.getMatchMinute(45))
    }

    @Test
    fun addedTimeIsLoggedWithAPlusMinutesSuffix() {
        val event = GenericLogEvent(
            message = "test",
            gameTimeMillis = ((47 * 60) * 1000L).toDouble(),
            phase = GamePhase.SECOND_HALF
        )

        assertEquals("90+2", event.getMatchMinute(45))
    }

    @Test
    fun secondHalfKickoffUsesTheHalfTimeBaseMinute() {
        val event = GenericLogEvent(
            message = "test",
            gameTimeMillis = 0.0,
            phase = GamePhase.SECOND_HALF
        )

        assertEquals("45'", event.getMatchMinute(45))
    }
}
