package com.homestrength.domain.trainee

import com.homestrength.data.local.entity.PracticeTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeDurationTest {

    @Test
    fun secondsToPracticedMinutes_dropsRemainder() {
        assertEquals(0, secondsToPracticedMinutes(0))
        assertEquals(0, secondsToPracticedMinutes(59))
        assertEquals(1, secondsToPracticedMinutes(60))
        assertEquals(9, secondsToPracticedMinutes(599))
        assertEquals(10, secondsToPracticedMinutes(600))
        assertEquals(10, secondsToPracticedMinutes(659))
    }

    @Test
    fun timedLightTrack_detectsTracks() {
        assertTrue(PracticeTrack.CULTIVATION.isTimedLightTrack())
        assertTrue(PracticeTrack.ALGORITHM.isTimedLightTrack())
        assertTrue(PracticeTrack.LIFE.isTimedLightTrack())
        assertFalse(PracticeTrack.STRENGTH.isTimedLightTrack())
        assertFalse(PracticeTrack.SLEEP.isTimedLightTrack())
        assertFalse((null as PracticeTrack?).isTimedLightTrack())
    }
}
