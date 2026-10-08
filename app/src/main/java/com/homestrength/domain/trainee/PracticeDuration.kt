package com.homestrength.domain.trainee

import com.homestrength.data.local.entity.PracticeTrack

/**
 * Converts elapsed practice seconds to checklist minutes (drop leftover seconds).
 */
fun secondsToPracticedMinutes(seconds: Int): Int =
    seconds.coerceAtLeast(0) / 60

/** Timed light tracks that support countdown practice from the checklist. */
fun PracticeTrack?.isTimedLightTrack(): Boolean = when (this) {
    PracticeTrack.ALGORITHM,
    PracticeTrack.VOCAL,
    PracticeTrack.CULTIVATION,
    PracticeTrack.LIFE,
    PracticeTrack.READING,
    PracticeTrack.CALLIGRAPHY -> true
    else -> false
}
