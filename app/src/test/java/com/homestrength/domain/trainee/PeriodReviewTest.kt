package com.homestrength.domain.trainee

import com.homestrength.data.local.entity.PracticeTrack
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class PeriodReviewTest {

    @Test
    fun aggregatesStrengthDaysAcrossQuarter() {
        val m1 = YearMonth.of(2026, 4)
        val m2 = YearMonth.of(2026, 5)
        val m3 = YearMonth.of(2026, 6)
        fun month(ym: YearMonth, strengthDays: Set<Int>): MonthReview {
            val days = (1..ym.lengthOfMonth()).map { d ->
                val day = ym.atDay(d)
                if (d in strengthDays) {
                    DayActivity(
                        day,
                        1,
                        setOf(PracticeTrack.STRENGTH),
                        listOf(DayEntry("力量", track = PracticeTrack.STRENGTH))
                    )
                } else {
                    DayActivity(day, 0, emptySet(), emptyList())
                }
            }
            return MonthReview(
                month = ym,
                days = days,
                activeDays = days.count { it.doneCount > 0 },
                totalDone = days.sumOf { it.doneCount }
            )
        }
        val review = PeriodReviewBuilder.fromMonths(
            PlanPeriod.quarter(2026, 2),
            listOf(month(m1, setOf(1, 3)), month(m2, setOf(10)), month(m3, emptySet()))
        )
        assertEquals(3, review.strengthDays)
        assertEquals(2, review.monthBuckets[0].strengthDays)
        assertEquals(1, review.monthBuckets[1].strengthDays)
        assertEquals(0, review.monthBuckets[2].strengthDays)
    }
}
