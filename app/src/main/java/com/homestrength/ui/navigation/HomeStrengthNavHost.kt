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
import com.homestrength.HomeStrengthApp
import com.homestrength.data.local.entity.PracticeTrack
import com.homestrength.data.local.entity.WorkoutType
import com.homestrength.data.local.relation.SessionWithLogs
import com.homestrength.data.repository.HomeStrengthRepository
import com.homestrength.data.repository.TraineeRepository
import androidx.compose.ui.platform.LocalContext
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
import com.homestrength.ui.practice.LightPracticeScreen
import com.homestrength.ui.profile.ProfileScreen
import com.homestrength.ui.profile.ProfileViewModel
import com.homestrength.ui.settings.SettingsScreen
import com.homestrength.ui.settings.SettingsViewModel
import com.homestrength.ui.welfare.WelfareScreen
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
    traineeRepository: TraineeRepository,
    modifier: Modifier = Modifier,
    onError: (String) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Home.route,
        modifier = modifier
    ) {
        composable(Routes.Home.route) {
            val vm: HomeViewModel = viewModel(
                factory = HomeViewModel.factory(repository, traineeRepository)
            )
            HomeScreen(
                viewModel = vm,
                onStartWorkout = { navController.navigate(Routes.ModeSelect.route) },
                onContinueWorkout = { sessionId ->
                    navController.navigate(Routes.ActiveWorkout.create(sessionId))
                },
                onOpenLightPractice = { nav ->
                    navController.navigate(
                        Routes.LightPractice.create(
                            track = nav.track.name,
                            itemId = nav.itemId,
                            targetSeconds = nav.targetSeconds
                        )
                    )
                },
                onOpenWelfare = { navController.navigate(Routes.Welfare.route) },
                onOpenHistory = { navController.navigate(Routes.History.route) },
                onOpenSettings = { navController.navigate(Routes.Settings.route) },
                onOpenProfile = { navController.navigate(Routes.Profile.route) },
                onMessage = onError
            )
        }

        composable(Routes.Profile.route) {
            val vm: ProfileViewModel = viewModel(
                factory = ProfileViewModel.factory(repository, traineeRepository)
            )
            ProfileScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onMessage = onError
            )
        }

        composable(Routes.Welfare.route) {
            WelfareScreen(
                traineeRepository = traineeRepository,
                onBack = { navController.popBackStack() },
                onMessage = onError
            )
        }

        composable(
            route = Routes.LightPractice.route,
            arguments = listOf(
                navArgument("track") { type = NavType.StringType },
                navArgument("itemId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("targetSeconds") { type = NavType.IntType; defaultValue = 0 }
            )
        ) { entry ->
            val trackName = entry.arguments?.getString("track") ?: return@composable
            val itemId = entry.arguments?.getLong("itemId") ?: 0L
            val targetSeconds = entry.arguments?.getInt("targetSeconds") ?: 0
            val parsed = runCatching { PracticeTrack.valueOf(trackName) }.getOrNull()
            val track = when (parsed) {
                PracticeTrack.ALGORITHM,
                PracticeTrack.VOCAL,
                PracticeTrack.CULTIVATION,
                PracticeTrack.LIFE,
                PracticeTrack.READING,
                PracticeTrack.CALLIGRAPHY -> TraineeRepository.canonicalTrack(parsed)
                else -> null
            }
            if (track == null) {
                navController.popBackStack()
                return@composable
            }
            LightPracticeScreen(
                track = track,
                traineeRepository = traineeRepository,
                powerListItemId = itemId,
                targetSeconds = targetSeconds,
                onDone = { _, _ ->
                    navController.popBackStack(Routes.Home.route, inclusive = false)
                },
                onBack = { navController.popBackStack() },
                onError = onError
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
            val app = LocalContext.current.applicationContext as HomeStrengthApp
            val vm: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory(
                    repository,
                    traineeRepository,
                    app.container.backupRepository
                )
            )
            SettingsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenBands = { navController.navigate(Routes.Bands.route) },
                onOpenPlanA = { navController.navigate(Routes.PlanA.route) },
                onOpenPlanB = { navController.navigate(Routes.PlanB.route) },
                onMessage = onError
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
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onError = onError,
                traineeRepository = traineeRepository
            )
        }
    }
}
