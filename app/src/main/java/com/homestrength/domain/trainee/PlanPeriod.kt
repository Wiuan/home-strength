package com.homestrength.domain.trainee

import java.time.LocalDate
import java.time.YearMonth

enum class PlanPeriodKind {
    MONTH,
    QUARTER,
    YEAR
}

data class PlanPeriod(
    val kind: PlanPeriodKind,
    val year: Int,
    val month: Int = 1,
    val quarter: Int = 1
) {
    val key: String
        get() = when (kind) {
            PlanPeriodKind.MONTH -> "%04d-%02d".format(year, month)
            PlanPeriodKind.QUARTER -> "$year-Q$quarter"
            PlanPeriodKind.YEAR -> year.toString()
        }

    fun label(): String = when (kind) {
        PlanPeriodKind.MONTH -> "${year} 年 ${month} 月"
        PlanPeriodKind.QUARTER -> "${year} 年 Q$quarter"
        PlanPeriodKind.YEAR -> "${year} 年"
    }

    fun planTitle(): String = when (kind) {
        PlanPeriodKind.MONTH -> "月计划"
        PlanPeriodKind.QUARTER -> "季计划"
        PlanPeriodKind.YEAR -> "年计划"
    }

    fun rhythmTitle(): String = when (kind) {
        PlanPeriodKind.MONTH -> "月节奏"
        PlanPeriodKind.QUARTER -> "季节奏"
        PlanPeriodKind.YEAR -> "年节奏"
    }

    fun months(): List<YearMonth> = when (kind) {
        PlanPeriodKind.MONTH -> listOf(YearMonth.of(year, month))
        PlanPeriodKind.QUARTER -> {
            val start = (quarter - 1) * 3 + 1
            (0 until 3).map { YearMonth.of(year, start + it) }
        }
        PlanPeriodKind.YEAR -> (1..12).map { YearMonth.of(year, it) }
    }

    fun shift(delta: Int): PlanPeriod = when (kind) {
        PlanPeriodKind.MONTH -> {
            val ym = YearMonth.of(year, month).plusMonths(delta.toLong())
            month(ym)
        }
        PlanPeriodKind.QUARTER -> {
            var y = year
            var q = quarter + delta
            while (q > 4) {
                q -= 4
                y++
            }
            while (q < 1) {
                q += 4
                y--
            }
            quarter(y, q)
        }
        PlanPeriodKind.YEAR -> year(year + delta)
    }

    fun clamp(minYear: Int, maxYear: Int): PlanPeriod {
        val y = year.coerceIn(minYear, maxYear)
        return when (kind) {
            PlanPeriodKind.MONTH -> copy(year = y, month = month.coerceIn(1, 12))
            PlanPeriodKind.QUARTER -> copy(year = y, quarter = quarter.coerceIn(1, 4))
            PlanPeriodKind.YEAR -> copy(year = y)
        }
    }

    fun isCurrent(today: LocalDate = LocalDate.now()): Boolean = when (kind) {
        PlanPeriodKind.MONTH -> year == today.year && month == today.monthValue
        PlanPeriodKind.QUARTER -> year == today.year && quarter == ((today.monthValue - 1) / 3 + 1)
        PlanPeriodKind.YEAR -> year == today.year
    }

    companion object {
        fun month(ym: YearMonth) = PlanPeriod(PlanPeriodKind.MONTH, ym.year, month = ym.monthValue)
        fun quarter(year: Int, quarter: Int) = PlanPeriod(PlanPeriodKind.QUARTER, year, quarter = quarter)
        fun year(year: Int) = PlanPeriod(PlanPeriodKind.YEAR, year)
        fun current(kind: PlanPeriodKind, today: LocalDate = LocalDate.now()): PlanPeriod = when (kind) {
            PlanPeriodKind.MONTH -> month(YearMonth.from(today))
            PlanPeriodKind.QUARTER -> quarter(today.year, (today.monthValue - 1) / 3 + 1)
            PlanPeriodKind.YEAR -> year(today.year)
        }
    }
}
