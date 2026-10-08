package com.homestrength.domain.trainee

import com.homestrength.data.local.entity.PracticeTrack
import java.time.YearMonth

data class MonthBucket(
    val month: YearMonth,
    val activeDays: Int,
    val totalDone: Int,
    /** Distinct days with a strength session in this month. */
    val strengthDays: Int = 0,
    /** Distinct days with an early-sleep check-in in this month. */
    val sleepDays: Int = 0
)

data class PeriodReview(
    val period: PlanPeriod,
    /** Day-level detail when viewing a single month; empty for quarter/year. */
    val monthReview: MonthReview?,
    val monthBuckets: List<MonthBucket>,
    val trackShares: List<TrackShare>,
    val activeDays: Int,
    val totalDone: Int,
    /** Distinct days with strength practice across the period. */
    val strengthDays: Int = 0,
    /** Distinct days with early-sleep check-in across the period. */
    val sleepDays: Int = 0
) {
    val totalShareSeconds: Int get() = trackShares.sumOf { it.seconds }
}

object PeriodReviewBuilder {
    fun empty(period: PlanPeriod = PlanPeriod.current(PlanPeriodKind.MONTH)): PeriodReview =
        PeriodReview(
            period = period,
            monthReview = null,
            monthBuckets = emptyList(),
            trackShares = emptyList(),
            activeDays = 0,
            totalDone = 0,
            strengthDays = 0,
            sleepDays = 0
        )

    fun fromMonths(period: PlanPeriod, months: List<MonthReview>): PeriodReview {
        val buckets = months.map {
            MonthBucket(
                month = it.month,
                activeDays = it.activeDays,
                totalDone = it.totalDone,
                strengthDays = it.days.count { day -> PracticeTrack.STRENGTH in day.tracks },
                sleepDays = it.days.count { day -> PracticeTrack.SLEEP in day.tracks }
            )
        }
        val shares = months
            .flatMap { it.trackShares }
            .groupBy { it.track }
            .map { (track, list) ->
                TrackShare(
                    track = track,
                    seconds = list.sumOf { it.seconds },
                    count = list.sumOf { it.count }
                )
            }
            .sortedByDescending { it.seconds }
        val single = months.singleOrNull()?.takeIf { period.kind == PlanPeriodKind.MONTH }
        return PeriodReview(
            period = period,
            monthReview = single,
            monthBuckets = buckets,
            trackShares = shares,
            activeDays = months.sumOf { it.activeDays },
            totalDone = months.sumOf { it.totalDone },
            strengthDays = buckets.sumOf { it.strengthDays },
            sleepDays = buckets.sumOf { it.sleepDays }
        )
    }
}
