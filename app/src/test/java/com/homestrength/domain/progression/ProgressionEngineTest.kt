package com.homestrength.domain.progression

import com.homestrength.data.local.entity.TargetUnit
import com.homestrength.domain.band.BandCombinationCalculator
import com.homestrength.domain.band.OwnedBand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionEngineTest {

    private val combinations = BandCombinationCalculator.generate(
        listOf(
            OwnedBand(10, 1),
            OwnedBand(15, 1),
            OwnedBand(20, 1),
            OwnedBand(25, 1),
            OwnedBand(30, 1)
        )
    )

    @Test
    fun noHistory_firstTime_noGuessedReps() {
        val suggestion = ProgressionEngine.suggest(
            targetMin = 8,
            targetMax = 15,
            unit = TargetUnit.REPS,
            previousValues = emptyList(),
            previousResistance = 0,
            combinations = combinations
        )
        assertEquals(ProgressionType.FIRST_TIME, suggestion.type)
        assertTrue(suggestion.suggestedValues.isEmpty())
        assertTrue(suggestion.suggestedValuesLabel.contains("8–15"))
    }

    @Test
    fun twelveEleven_doesNotIncreaseResistance() {
        val suggestion = ProgressionEngine.suggest(
            targetMin = 8,
            targetMax = 15,
            unit = TargetUnit.REPS,
            previousValues = listOf(12, 11),
            previousResistance = 20,
            combinations = combinations
        )
        assertEquals(ProgressionType.ADD_REPS, suggestion.type)
        assertEquals(20, suggestion.suggestedResistance)
        assertEquals(listOf(13, 12), suggestion.suggestedValues)
    }

    @Test
    fun fifteenFifteen_recommendsHigherResistance() {
        val suggestion = ProgressionEngine.suggest(
            targetMin = 8,
            targetMax = 15,
            unit = TargetUnit.REPS,
            previousValues = listOf(15, 15),
            previousResistance = 20,
            combinations = combinations
        )
        assertEquals(ProgressionType.ADD_RESISTANCE, suggestion.type)
        assertEquals(25, suggestion.suggestedResistance)
        assertEquals(25, suggestion.suggestedCombination?.totalResistance)
        assertTrue(suggestion.suggestedValuesLabel.contains("8–10"))
    }

    @Test
    fun belowMin_suggestsConsiderLower() {
        val suggestion = ProgressionEngine.suggest(
            targetMin = 8,
            targetMax = 15,
            unit = TargetUnit.REPS,
            previousValues = listOf(6, 5),
            previousResistance = 20,
            combinations = combinations
        )
        assertEquals(ProgressionType.CONSIDER_LOWER, suggestion.type)
        assertEquals(20, suggestion.suggestedResistance)
        assertTrue(suggestion.detailMessage!!.contains("降低阻力"))
    }

    @Test
    fun thirtyFive_nextIsForty() {
        val next = BandCombinationCalculator.nextHigher(combinations, 35)
        assertEquals(40, next?.totalResistance)
    }

    @Test
    fun atMaxWithNoHigherBand_staysPut() {
        val onlyTwenty = BandCombinationCalculator.generate(listOf(OwnedBand(20, 1)))
        val suggestion = ProgressionEngine.suggest(
            targetMin = 8,
            targetMax = 15,
            unit = TargetUnit.REPS,
            previousValues = listOf(15, 15),
            previousResistance = 20,
            combinations = onlyTwenty
        )
        assertEquals(ProgressionType.ADD_REPS, suggestion.type)
        assertNull(
            BandCombinationCalculator.nextHigher(onlyTwenty, 20)
        )
        assertTrue(suggestion.message.contains("暂无更高"))
    }
}
