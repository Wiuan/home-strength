package com.homestrength.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.homestrength.data.local.entity.WorkoutType
import com.homestrength.data.local.relation.SessionWithLogs
import com.homestrength.data.repository.HomeStrengthRepository
import com.homestrength.ui.bands.BandManageScreen
import com.homestrength.ui.bands.BandManageViewModel
import com.homestrength.ui.exercise.ExerciseDetailScreen
import com.homestrength.ui.exercise.ExerciseDetailViewModel
import com.homestrength.ui.history.HistoryDetailScreen
import com.homestrength.ui.history.HistoryScreen
import com.homestrength.ui.history.HistoryViewModel
import com.homestrength.ui.home.HomeScreen
import com.homestrength.ui.home.HomeViewModel
import com.homestrength.ui.placeholder.PlanScreen
import com.homestrength.ui.placeholder.planTitle
import com.homestrength.ui.settings.SettingsScreen
import com.homestrength.ui.settings.SettingsViewModel
import com.homestrength.ui.workout.ActiveWorkoutScreen
import com.homestrength.ui.workout.ActiveWorkoutViewModel
import com.homestrength.ui.workout.CompleteWorkoutScreen
import com.homestrength.ui.workout.ModeSelectScreen
import com.homestrength.ui.workout.ModeSelectViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeStrengthNavHost(
    navController: NavHostController,
    repository: HomeStrengthRepository,
    modifier: Modifier = Modifier,
    onError: (String) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Home.route,
        modifier = modifier
    ) {
        composable(Routes.Home.route) {
            val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(repository))
            HomeScreen(
                viewModel = vm,
                onStartWorkout = { navController.navigate(Routes.ModeSelect.route) },
                onContinueWorkout = { sessionId ->
                    navController.navigate(Routes.ActiveWorkout.create(sessionId))
                },
                onOpenHistory = { navController.navigate(Routes.History.route) },
                onOpenSettings = { navController.navigate(Routes.Settings.route) }
            )
        }

        composable(Routes.History.route) {
            val vm: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(repository))
            HistoryScreen(
                viewModel = vm,
                onOpenDetail = { id -> navController.navigate(Routes.HistoryDetail.create(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.HistoryDetail.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { entry ->
            val sessionId = entry.arguments?.getLong("sessionId") ?: return@composable
            var session by remember { mutableStateOf<SessionWithLogs?>(null) }
            val scope = rememberCoroutineScope()
            LaunchedEffect(sessionId) {
                session = repository.getSession(sessionId)
            }
            HistoryDetailScreen(
                session = session,
                onDelete = {
                    scope.launch {
                        repository.deleteSessionAndRecomputeNext(sessionId)
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.Settings.route) {
            val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(repository))
            SettingsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenBands = { navController.navigate(Routes.Bands.route) },
                onOpenPlanA = { navController.navigate(Routes.PlanA.route) },
                onOpenPlanB = { navController.navigate(Routes.PlanB.route) }
            )
        }

        composable(Routes.Bands.route) {
            val vm: BandManageViewModel = viewModel(factory = BandManageViewModel.factory(repository))
            BandManageScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PlanA.route) {
            PlanScreen(
                title = planTitle(WorkoutType.A),
                exercises = repository.observeExercises(WorkoutType.A),
                onBack = { navController.popBackStack() },
                onOpenExercise = { id -> navController.navigate(Routes.ExerciseDetail.create(id)) }
            )
        }

        composable(Routes.PlanB.route) {
            PlanScreen(
                title = planTitle(WorkoutType.B),
                exercises = repository.observeExercises(WorkoutType.B),
                onBack = { navController.popBackStack() },
                onOpenExercise = { id -> navController.navigate(Routes.ExerciseDetail.create(id)) }
            )
        }

        composable(
            route = Routes.ExerciseDetail.route,
            arguments = listOf(navArgument("exerciseId") { type = NavType.LongType })
        ) { entry ->
            val exerciseId = entry.arguments?.getLong("exerciseId") ?: return@composable
            val vm: ExerciseDetailViewModel = viewModel(
                factory = ExerciseDetailViewModel.factory(repository, exerciseId)
            )
            ExerciseDetailScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ModeSelect.route) {
            val vm: ModeSelectViewModel = viewModel(factory = ModeSelectViewModel.factory(repository))
            ModeSelectScreen(
                viewModel = vm,
                onStarted = { sessionId ->
                    navController.navigate(Routes.ActiveWorkout.create(sessionId)) {
                        popUpTo(Routes.ModeSelect.route) { inclusive = true }
                    }
                },
                onContinueIncomplete = { sessionId ->
                    navController.navigate(Routes.ActiveWorkout.create(sessionId)) {
                        popUpTo(Routes.ModeSelect.route) { inclusive = true }
                    }
                },
                onError = onError,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.ActiveWorkout.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { entry ->
            val sessionId = entry.arguments?.getLong("sessionId") ?: return@composable
            val vm: ActiveWorkoutViewModel = viewModel(
                factory = ActiveWorkoutViewModel.factory(repository, sessionId)
            )
            ActiveWorkoutScreen(
                viewModel = vm,
                onFinish = {
                    navController.navigate(Routes.CompleteWorkout.create(sessionId)) {
                        popUpTo(Routes.ActiveWorkout.create(sessionId)) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack(Routes.Home.route, inclusive = false) },
                onOpenExercise = { id -> navController.navigate(Routes.ExerciseDetail.create(id)) }
            )
        }

        composable(
            route = Routes.CompleteWorkout.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { entry ->
            val sessionId = entry.arguments?.getLong("sessionId") ?: return@composable
            CompleteWorkoutScreen(
                repository = repository,
                sessionId = sessionId,
                onDone = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Home.route) { inclusive = true }
                    }
                },
                onError = onError
            )
        }
    }
}
