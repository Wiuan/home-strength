package com.homestrength.domain.progression

import com.homestrength.data.local.entity.TargetUnit
import com.homestrength.data.local.relation.ExerciseLogWithSets
import com.homestrength.domain.band.BandCombination
import com.homestrength.domain.band.BandCombinationCalculator

enum class ProgressionType {
    FIRST_TIME,
    ADD_REPS,
    ADD_RESISTANCE,
    CONSIDER_LOWER
}

data class ProgressionSuggestion(
    val type: ProgressionType,
    val previousResistance: Int,
    val suggestedResistance: Int,
    val suggestedCombination: BandCombination?,
    val suggestedValues: List<Int>,
    val suggestedValuesLabel: String,
    val message: String,
    val detailMessage: String? = null
) {
    val shouldHighlightResistanceBump: Boolean
        get() = type == ProgressionType.ADD_RESISTANCE
}

object ProgressionEngine {

    fun suggest(
        targetMin: Int,
        targetMax: Int,
        unit: TargetUnit,
        previousValues: List<Int>,
        previousResistance: Int,
        combinations: List<BandCombination>
    ): ProgressionSuggestion {
        val unitWord = when (unit) {
            TargetUnit.REPS -> "次"
            TargetUnit.SECONDS -> "秒"
        }

        if (previousValues.isEmpty()) {
            return ProgressionSuggestion(
                type = ProgressionType.FIRST_TIME,
                previousResistance = 0,
                suggestedResistance = 0,
                suggestedCombination = combinations.firstOrNull { it.totalResistance == 0 },
                suggestedValues = emptyList(),
                suggestedValuesLabel = "$targetMin–$targetMax $unitWord",
                message = "第一次训练",
                detailMessage = "按目标区间自行选择合适强度，不自动猜测次数。"
            )
        }

        val allAtOrAboveMax = previousValues.all { it >= targetMax }
        val anyBelowMin = previousValues.any { it < targetMin }

        if (allAtOrAboveMax) {
            val next = BandCombinationCalculator.nextHigher(combinations, previousResistance)
            val resetHigh = (targetMin + 2).coerceAtMost(targetMax)
            return if (next != null) {
                ProgressionSuggestion(
                    type = ProgressionType.ADD_RESISTANCE,
                    previousResistance = previousResistance,
                    suggestedResistance = next.totalResistance,
                    suggestedCombination = next,
                    suggestedValues = emptyList(),
                    suggestedValuesLabel = "$targetMin–$resetHigh $unitWord",
                    message = "建议增加阻力",
                    detailMessage = "下一档 ${next.displayLabel()}，次数回到 $targetMin–$resetHigh $unitWord"
                )
            } else {
                ProgressionSuggestion(
                    type = ProgressionType.ADD_REPS,
                    previousResistance = previousResistance,
                    suggestedResistance = previousResistance,
                    suggestedCombination = combinations.firstOrNull { it.totalResistance == previousResistance },
                    suggestedValues = previousValues.map { targetMax },
                    suggestedValuesLabel = formatValues(previousValues.map { targetMax }, unit),
                    message = "已达上限，暂无更高阻力",
                    detailMessage = "可在设置中添加更重的弹力带。"
                )
            }
        }

        if (anyBelowMin) {
            return ProgressionSuggestion(
                type = ProgressionType.CONSIDER_LOWER,
                previousResistance = previousResistance,
                suggestedResistance = previousResistance,
                suggestedCombination = combinations.firstOrNull { it.totalResistance == previousResistance },
                suggestedValues = previousValues,
                suggestedValuesLabel = formatValues(previousValues, unit),
                message = "保持 ${previousResistance} LB",
                detailMessage = "上次有一组低于目标范围，可以考虑降低阻力。"
            )
        }

        val nextValues = previousValues.map { (it + 1).coerceAtMost(targetMax) }
        return ProgressionSuggestion(
            type = ProgressionType.ADD_REPS,
            previousResistance = previousResistance,
            suggestedResistance = previousResistance,
            suggestedCombination = combinations.firstOrNull { it.totalResistance == previousResistance },
            suggestedValues = nextValues,
            suggestedValuesLabel = formatValues(nextValues, unit),
            message = "保持 ${previousResistance} LB",
            detailMessage = "下一次争取 ${formatValues(nextValues, unit)}"
        )
    }

    fun suggestFromLog(
        log: ExerciseLogWithSets,
        combinations: List<BandCombination>
    ): ProgressionSuggestion {
        val extracted = extractPrevious(log)
        return suggest(
            targetMin = log.exercise.targetMin,
            targetMax = log.exercise.targetMax,
            unit = log.exercise.targetUnit,
            previousValues = extracted.values,
            previousResistance = extracted.resistance,
            combinations = combinations
        )
    }

    fun extractPrevious(log: ExerciseLogWithSets): ExtractedPrevious {
        val unit = log.exercise.targetUnit
        val sets = log.sets.sortedWith(
            compareBy({ it.set.setNumber }, { it.set.side?.ordinal ?: -1 })
        )
        val resistance = sets.map { it.set.totalResistance }.firstOrNull { it > 0 }
            ?: sets.firstOrNull()?.set?.totalResistance
            ?: 0

        val values = if (log.exercise.isUnilateral) {
            sets.mapNotNull { valueOf(it.set.reps, it.set.durationSeconds, unit) }
        } else {
            sets.mapNotNull { valueOf(it.set.reps, it.set.durationSeconds, unit) }
        }

        return ExtractedPrevious(resistance = resistance, values = values)
    }

    private fun valueOf(reps: Int?, seconds: Int?, unit: TargetUnit): Int? =
        when (unit) {
            TargetUnit.REPS -> reps
            TargetUnit.SECONDS -> seconds
        }

    private fun formatValues(values: List<Int>, unit: TargetUnit): String {
        if (values.isEmpty()) return "—"
        return when (unit) {
            TargetUnit.REPS -> values.joinToString(" / ")
            TargetUnit.SECONDS -> values.joinToString(" / ") { "${it}s" }
        }
    }
}

data class ExtractedPrevious(
    val resistance: Int,
    val values: List<Int>
)
