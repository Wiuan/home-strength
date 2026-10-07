package com.homestrength.domain.trainee

enum class TraineeGrade(val label: String, val minFans: Int) {
    F("F 级练习生", 0),
    E("E 级练习生", 500),
    D("D 级练习生", 1000),
    C("C 级练习生", 2000),
    B("B 级练习生", 3500),
    A("A 级练习生", 5500),
    PRE_DEBUT("出道预备", 8000);

    companion object {
        fun fromFans(fans: Int): TraineeGrade {
            val ordered = entries.sortedByDescending { it.minFans }
            return ordered.firstOrNull { fans >= it.minFans } ?: F
        }

        fun progressInLevel(fans: Int): Pair<TraineeGrade, Int?> {
            val current = fromFans(fans)
            val next = entries.sortedBy { it.minFans }.firstOrNull { it.minFans > current.minFans }
            return current to next?.minFans
        }

        /** 0f..1f progress within the current grade toward the next. */
        fun progressFraction(fans: Int): Float {
            val current = fromFans(fans)
            val nextMin = progressInLevel(fans).second ?: return 1f
            val span = (nextMin - current.minFans).coerceAtLeast(1)
            return ((fans - current.minFans).toFloat() / span).coerceIn(0f, 1f)
        }

        fun letter(grade: TraineeGrade): String =
            if (grade == PRE_DEBUT) "★" else grade.name
    }
}

/** Default amounts; live values live on [TraineeProfileEntity] and are editable in Settings. */
object TraineeRewards {
    const val STRENGTH_FANS = 100
    const val STRENGTH_COINS = 12
    const val LIGHT_FANS = 40
    const val LIGHT_COINS = 5
    /** Manual checklist tick (custom item or re-check). */
    const val CHECKLIST_FANS = 20
    const val CHECKLIST_COINS = 3
    const val SLEEP_ENERGY = 20
    const val SLEEP_FANS = 10
    const val SLEEP_COINS = 2
    /** Small restore; unlimited times (capped by ENERGY_MAX). */
    const val MEDITATION_ENERGY = 5
    const val MEDITATION_FANS = 5
    const val MEDITATION_COINS = 1
    const val ENERGY_MAX = 100
}
