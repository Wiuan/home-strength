package com.homestrength.domain.band

/**
 * A concrete combination of owned bands.
 * [countsByResistance] maps resistance LB -> how many bands of that resistance are used.
 */
data class BandCombination(
    val totalResistance: Int,
    val countsByResistance: Map<Int, Int>
) {
    val bandCount: Int get() = countsByResistance.values.sum()

    fun displayLabel(): String {
        if (totalResistance == 0) return "0 LB"
        val parts = countsByResistance.entries
            .sortedBy { it.key }
            .flatMap { (resistance, count) -> List(count) { "$resistance LB" } }
        return if (parts.size <= 1) {
            "$totalResistance LB"
        } else {
            "$totalResistance LB（${parts.joinToString(" + ")}）"
        }
    }
}

object BandCombinationCalculator {
    fun generate(bands: List<OwnedBand>): List<BandCombination> {
        val inventory = bands
            .filter { it.quantity > 0 && it.resistance > 0 }
            .groupBy { it.resistance }
            .map { (resistance, items) -> resistance to items.sumOf { it.quantity } }
            .sortedBy { it.first }

        val results = linkedMapOf<String, BandCombination>()
        results[keyOf(emptyMap())] = BandCombination(0, emptyMap())

        fun dfs(index: Int, current: MutableMap<Int, Int>, total: Int) {
            if (index == inventory.size) {
                val snapshot = current.toMap().filterValues { it > 0 }
                val combination = BandCombination(total, snapshot)
                val key = keyOf(snapshot)
                val existing = results[key]
                if (existing == null || isBetter(combination, existing)) {
                    results[key] = combination
                }
                return
            }
            val (resistance, maxQty) = inventory[index]
            for (used in 0..maxQty) {
                if (used > 0) current[resistance] = used else current.remove(resistance)
                dfs(index + 1, current, total + resistance * used)
            }
            current.remove(resistance)
        }

        dfs(0, linkedMapOf(), 0)

        return results.values.sortedWith(
            compareBy<BandCombination> { it.totalResistance }
                .thenBy { it.bandCount }
                .thenBy { keyOf(it.countsByResistance) }
        )
    }

    fun nextHigher(
        combinations: List<BandCombination>,
        currentTotal: Int
    ): BandCombination? {
        val candidates = combinations.filter { it.totalResistance > currentTotal }
        if (candidates.isEmpty()) return null
        val targetTotal = candidates.minOf { it.totalResistance }
        return candidates
            .filter { it.totalResistance == targetTotal }
            .sortedWith(
                compareBy<BandCombination> { it.bandCount }
                    .thenBy { keyOf(it.countsByResistance) }
            )
            .firstOrNull()
    }

    private fun isBetter(candidate: BandCombination, existing: BandCombination): Boolean {
        return when {
            candidate.bandCount != existing.bandCount -> candidate.bandCount < existing.bandCount
            else -> keyOf(candidate.countsByResistance) < keyOf(existing.countsByResistance)
        }
    }

    private fun keyOf(counts: Map<Int, Int>): String =
        counts.entries.sortedBy { it.key }.joinToString(",") { "${it.key}x${it.value}" }
}

data class OwnedBand(
    val resistance: Int,
    val quantity: Int
)
