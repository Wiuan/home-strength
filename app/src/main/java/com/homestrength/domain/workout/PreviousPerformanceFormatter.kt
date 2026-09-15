package com.homestrength.domain.workout

import com.homestrength.data.local.entity.SetSide
import com.homestrength.data.local.entity.TargetUnit
import com.homestrength.data.local.relation.ExerciseLogWithSets

data class PreviousPerformance(
    val totalResistance: Int,
    val valuesLabel: String,
    val summaryLabel: String
)

object PreviousPerformanceFormatter {
    fun fromLog(log: ExerciseLogWithSets): PreviousPerformance {
        val unit = log.exercise.targetUnit
        val sets = log.sets.sortedWith(
            compareBy({ it.set.setNumber }, { it.set.side?.ordinal ?: -1 })
        )
        val resistance = sets.map { it.set.totalResistance }.firstOrNull { it > 0 }
            ?: sets.firstOrNull()?.set?.totalResistance
            ?: 0

        val valuesLabel = if (log.exercise.isUnilateral) {
            sets.groupBy { it.set.setNumber }.toSortedMap().map { (_, sideSets) ->
                val left = sideSets.firstOrNull { it.set.side == SetSide.LEFT }
                val right = sideSets.firstOrNull { it.set.side == SetSide.RIGHT }
                "${valueText(left?.let { valueOf(it.set.reps, it.set.durationSeconds, unit) }, unit)} / " +
                    valueText(right?.let { valueOf(it.set.reps, it.set.durationSeconds, unit) }, unit)
            }.joinToString(" · ")
        } else {
            sets.map {
                valueText(valueOf(it.set.reps, it.set.durationSeconds, unit), unit)
            }.joinToString(" / ")
        }

        val summaryLabel = buildString {
            if (resistance > 0) append("$resistance LB · ")
            append(valuesLabel.ifBlank { "—" })
        }
        return PreviousPerformance(
            totalResistance = resistance,
            valuesLabel = valuesLabel.ifBlank { "—" },
            summaryLabel = summaryLabel
        )
    }

    private fun valueOf(reps: Int?, seconds: Int?, unit: TargetUnit): Int? =
        when (unit) {
            TargetUnit.REPS -> reps
            TargetUnit.SECONDS -> seconds
        }

    private fun valueText(value: Int?, unit: TargetUnit): String {
        if (value == null) return "—"
        return when (unit) {
            TargetUnit.REPS -> value.toString()
            TargetUnit.SECONDS -> "${value}s"
        }
    }
}
