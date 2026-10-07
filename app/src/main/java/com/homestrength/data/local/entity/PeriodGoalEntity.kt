package com.homestrength.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.homestrength.domain.trainee.PlanPeriodKind

enum class PeriodGoalStatus {
    ACTIVE,
    DONE,
    DROPPED
}

@Entity(
    tableName = "period_goals",
    indices = [Index(value = ["periodKind", "periodKey", "sortOrder"])]
)
data class PeriodGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val periodKind: PlanPeriodKind,
    /** MONTH: yyyy-MM · QUARTER: yyyy-Qn · YEAR: yyyy */
    val periodKey: String,
    val title: String,
    val track: PracticeTrack? = null,
    val status: PeriodGoalStatus = PeriodGoalStatus.ACTIVE,
    val sortOrder: Int = 0,
    /** How many parts this goal is split into (e.g. 12 books → 12). */
    val targetParts: Int = 1,
    /** Completed parts so far (e.g. 1/12). */
    val doneParts: Int = 0
)
