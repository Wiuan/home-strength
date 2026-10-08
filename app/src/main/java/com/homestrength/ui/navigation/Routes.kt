package com.homestrength.ui.navigation

sealed class Routes(val route: String) {
    data object Home : Routes("home")
    data object History : Routes("history")
    data object Settings : Routes("settings")
    data object PlanA : Routes("plan/A")
    data object PlanB : Routes("plan/B")
    data object Bands : Routes("bands")
    data object ModeSelect : Routes("workout/mode")
    data object ActiveWorkout : Routes("workout/active/{sessionId}") {
        fun create(sessionId: Long) = "workout/active/$sessionId"
    }
    data object CompleteWorkout : Routes("workout/complete/{sessionId}") {
        fun create(sessionId: Long) = "workout/complete/$sessionId"
    }
    data object HistoryDetail : Routes("history/{sessionId}") {
        fun create(sessionId: Long) = "history/$sessionId"
    }
    data object ExerciseDetail : Routes("exercise/{exerciseId}") {
        fun create(exerciseId: Long) = "exercise/$exerciseId"
    }
    /**
     * Light practice. [itemId]=0 means ad-hoc (no checklist row).
     * [targetSeconds]=0 means unlimited count-up.
     */
    data object LightPractice : Routes("practice/{track}/{itemId}/{targetSeconds}") {
        fun create(track: String, itemId: Long = 0L, targetSeconds: Int = 0) =
            "practice/$track/$itemId/$targetSeconds"
    }
    data object Welfare : Routes("welfare")
    data object Profile : Routes("profile")
}
