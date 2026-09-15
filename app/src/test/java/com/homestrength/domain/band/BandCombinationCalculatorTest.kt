package com.homestrength.domain.band

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BandCombinationCalculatorTest {

    @Test
    fun singleBandsCannotBeDuplicated() {
        val combinations = BandCombinationCalculator.generate(
            listOf(
                OwnedBand(10, 1),
                OwnedBand(15, 1),
                OwnedBand(20, 1),
                OwnedBand(25, 1),
                OwnedBand(30, 1)
            )
        )
        val totals = combinations.map { it.totalResistance }.toSet()
        assertTrue(0 in totals)
        assertTrue(10 in totals)
        assertTrue(35 in totals) // 10+25 or 15+20
        assertTrue(45 in totals)
        assertTrue(100 in totals) // 10+15+20+25+30
        // Cannot use 20 twice when quantity is 1
        assertFalse(combinations.any { it.countsByResistance[20] == 2 })
    }

    @Test
    fun twoSameResistanceBandsCanCombine() {
        val combinations = BandCombinationCalculator.generate(
            listOf(OwnedBand(20, 2))
        )
        assertTrue(combinations.any { it.totalResistance == 40 && it.countsByResistance[20] == 2 })
    }

    @Test
    fun nextHigherPrefersFewerBands() {
        val combinations = BandCombinationCalculator.generate(
            listOf(
                OwnedBand(10, 1),
                OwnedBand(15, 1),
                OwnedBand(20, 1),
                OwnedBand(25, 1),
                OwnedBand(30, 1)
            )
        )
        val next = BandCombinationCalculator.nextHigher(combinations, currentTotal = 20)
        assertEquals(25, next?.totalResistance)
        assertEquals(1, next?.bandCount)
    }
}
