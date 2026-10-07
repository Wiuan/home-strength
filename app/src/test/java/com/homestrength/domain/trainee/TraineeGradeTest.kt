package com.homestrength.domain.trainee

import org.junit.Assert.assertEquals
import org.junit.Test

class TraineeGradeTest {
    @Test
    fun gradeThresholds() {
        assertEquals(TraineeGrade.F, TraineeGrade.fromFans(0))
        assertEquals(TraineeGrade.F, TraineeGrade.fromFans(499))
        assertEquals(TraineeGrade.E, TraineeGrade.fromFans(500))
        assertEquals(TraineeGrade.A, TraineeGrade.fromFans(5500))
        assertEquals(TraineeGrade.PRE_DEBUT, TraineeGrade.fromFans(8000))
    }

    @Test
    fun progressFraction_midLevel() {
        // E is 500..999; at 750 → halfway to D (1000)
        assertEquals(0.5f, TraineeGrade.progressFraction(750), 0.01f)
    }
}
