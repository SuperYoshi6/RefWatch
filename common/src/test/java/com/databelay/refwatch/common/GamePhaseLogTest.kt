package com.databelay.refwatch.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GamePhaseLogTest {
    @Test
    fun preGameAndKickoffSelectionPhasesAreHiddenFromLogs() {
        assertFalse(GamePhase.PRE_GAME.shouldBeLogged())
        assertFalse(GamePhase.KICK_OFF_SELECTION_FIRST_HALF.shouldBeLogged())
        assertFalse(GamePhase.KICK_OFF_SELECTION_EXTRA_TIME.shouldBeLogged())
        assertFalse(GamePhase.KICK_OFF_SELECTION_PENALTIES.shouldBeLogged())
    }

    @Test
    fun playablePhasesAreStillLogged() {
        assertTrue(GamePhase.FIRST_HALF.shouldBeLogged())
        assertTrue(GamePhase.SECOND_HALF.shouldBeLogged())
        assertTrue(GamePhase.PENALTIES.shouldBeLogged())
    }
}
