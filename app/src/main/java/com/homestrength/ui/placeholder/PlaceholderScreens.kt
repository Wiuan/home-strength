package com.homestrength.ui.placeholder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homestrength.data.local.entity.ExerciseEntity
import com.homestrength.data.local.entity.TargetUnit
import com.homestrength.data.local.entity.WorkoutType
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    title: String,
    exercises: Flow<List<ExerciseEntity>>,
    onBack: () -> Unit,
    onOpenExercise: (Long) -> Unit = {}
) {
    val list by exercises.collectAsStateWithLifecycle(initialValue = emptyList())
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("返回") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(list, key = { it.id }) { exercise ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenExercise(exercise.id) },
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(exercise.name, style = MaterialTheme.typography.titleLarge)
                    val unit = when (exercise.targetUnit) {
                        TargetUnit.REPS -> "次"
                        TargetUnit.SECONDS -> "秒"
                    }
                    Text(
                        "${exercise.defaultSets} 组 · 目标 ${exercise.targetMin}–${exercise.targetMax} $unit" +
                            if (exercise.isUnilateral) " / 每侧" else ""
                    )
                    if (exercise.description.isNotBlank()) {
                        Text(
                            exercise.description,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

fun planTitle(type: WorkoutType): String = "Workout ${type.name}"
