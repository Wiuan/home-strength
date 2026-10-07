package com.homestrength.domain.trainee

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class PlanPeriodTest {
    @Test
    fun monthKeyAndShift() {
        val p = PlanPeriod.month(YearMonth.of(2026, 3))
        assertEquals("2026-03", p.key)
        assertEquals("2026-04", p.shift(1).key)
        assertEquals("2025-12", p.shift(-3).key)
    }

    @Test
    fun quarterKeyAndMonths() {
        val p = PlanPeriod.quarter(2026, 2)
        assertEquals("2026-Q2", p.key)
        assertEquals(
            listOf(
                YearMonth.of(2026, 4),
                YearMonth.of(2026, 5),
                YearMonth.of(2026, 6)
            ),
            p.months()
        )
        assertEquals("2026-Q3", p.shift(1).key)
        assertEquals("2025-Q4", p.shift(-2).key)
    }

    @Test
    fun yearKeyAndCurrent() {
        val p = PlanPeriod.year(2026)
        assertEquals("2026", p.key)
        assertEquals(12, p.months().size)
        assertTrue(PlanPeriod.current(PlanPeriodKind.YEAR, LocalDate.of(2026, 10, 7)).isCurrent(LocalDate.of(2026, 10, 7)))
    }
}
