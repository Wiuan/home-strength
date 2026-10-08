package com.homestrength.ui.home

import com.homestrength.data.local.entity.PracticeTrack
import org.junit.Assert.assertEquals
import org.junit.Test

class ChecklistDefaultsTest {

    @Test
    fun defaultTitles_byTrackAndMinutes() {
        assertEquals("打扫卫生", defaultChecklistTitle(PracticeTrack.LIFE, 0))
        assertEquals("阅读 10 分钟", defaultChecklistTitle(PracticeTrack.CULTIVATION, 10))
        assertEquals("阅读 15 分钟", defaultChecklistTitle(PracticeTrack.CULTIVATION, 15))
        assertEquals("算法10分钟", defaultChecklistTitle(PracticeTrack.ALGORITHM, 10))
        assertEquals("算法20分钟", defaultChecklistTitle(PracticeTrack.ALGORITHM, 20))
        assertEquals("声乐10分钟", defaultChecklistTitle(PracticeTrack.VOCAL, 10))
        assertEquals("声乐10分钟", defaultChecklistTitle(PracticeTrack.VOCAL, 0))
    }
}
