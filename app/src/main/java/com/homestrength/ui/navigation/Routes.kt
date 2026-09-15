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
}
