package com.homestrength.data.local.entity

enum class WorkoutType {
    A,
    B
}

enum class TargetUnit {
    REPS,
    SECONDS
}

enum class MovementRole {
    PUSH,
    PULL,
    SQUAT,
    HINGE,
    CORE
}

enum class TrainingMode {
    NORMAL,
    TIRED,
    EXHAUSTED
}

enum class SetSide {
    LEFT,
    RIGHT
}

/**
 * Life tracks for the Trainee shell.
 * READING / CALLIGRAPHY kept for old rows; UI uses [CULTIVATION].
 */
enum class PracticeTrack {
    STRENGTH,
    ALGORITHM,
    VOCAL,
    READING,
    CALLIGRAPHY,
    /** 修养 = 阅读 + 书法 */
    CULTIVATION,
    /** 生活小事 + 备注（自定义清单） */
    LIFE,
    SLEEP,
    /** 放下手机也算；少回元气、不限次数 */
    MEDITATION
}

enum class PowerItemStatus {
    PENDING,
    DONE
}
