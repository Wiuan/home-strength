package com.homestrength.data.repository

import com.homestrength.data.local.dao.AppSettingsDao
import com.homestrength.data.local.dao.BandDao
import com.homestrength.data.local.dao.ExerciseDao
import com.homestrength.data.local.dao.WorkoutSessionDao
import com.homestrength.data.local.entity.AppSettingsEntity
import com.homestrength.data.local.entity.BandEntity
import com.homestrength.data.local.entity.ExerciseEntity
import com.homestrength.data.local.entity.ExerciseLogEntity
import com.homestrength.data.local.entity.ExerciseSetEntity
import com.homestrength.data.local.entity.SetBandCrossRef
import com.homestrength.data.local.entity.SetSide
import com.homestrength.data.local.entity.TargetUnit
import com.homestrength.data.local.entity.TrainingMode
import com.homestrength.data.local.entity.WorkoutSessionEntity
import com.homestrength.data.local.entity.WorkoutType
import com.homestrength.data.local.relation.ExerciseLogWithSets
import com.homestrength.data.local.relation.SessionWithLogs
import com.homestrength.domain.band.BandCombination
import com.homestrength.domain.band.BandCombinationCalculator
import com.homestrength.domain.band.OwnedBand
import com.homestrength.domain.progression.ProgressionEngine
import com.homestrength.domain.progression.ProgressionSuggestion
import com.homestrength.domain.workout.PreviousPerformance
import com.homestrength.domain.workout.PreviousPerformanceFormatter
import com.homestrength.domain.workout.WorkoutPlanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HomeStrengthRepository(
    private val exerciseDao: ExerciseDao,
    private val bandDao: BandDao,
    private val settingsDao: AppSettingsDao,
    private val sessionDao: WorkoutSessionDao,
    private val traineeRepository: TraineeRepository? = null
) {
    fun observeSettings(): Flow<AppSettingsEntity> =
        settingsDao.observe().map { it ?: AppSettingsEntity() }

    suspend fun getSettings(): AppSettingsEntity =
        settingsDao.get() ?: AppSettingsEntity().also { settingsDao.upsert(it) }

    suspend fun updateSettings(settings: AppSettingsEntity) {
        settingsDao.upsert(settings)
    }

    fun observeExercises(type: WorkoutType): Flow<List<ExerciseEntity>> =
        exerciseDao.observeByWorkoutType(type).map { it.distinctByWorkoutSlot() }

    suspend fun getExercises(type: WorkoutType): List<ExerciseEntity> =
        exerciseDao.getByWorkoutType(type).distinctByWorkoutSlot()

    suspend fun getExercise(id: Long): ExerciseEntity? = exerciseDao.getById(id)

    suspend fun getRecentLogs(exerciseId: Long, limit: Int = 20): List<ExerciseLogWithSets> =
        exerciseDao.getRecentCompletedLogs(exerciseId, limit)

    fun observeBands(): Flow<List<BandEntity>> = bandDao.observeAll()

    suspend fun getEnabledBands(): List<BandEntity> = bandDao.getEnabled()

    suspend fun availableCombinations(): List<BandCombination> {
        val owned = getEnabledBands().map { OwnedBand(it.resistance, it.quantity) }
        return BandCombinationCalculator.generate(owned)
    }

    /**
     * Save band. Same resistance merges into quantity when creating new.
     */
    suspend fun saveBand(resistance: Int, quantity: Int, id: Long = 0L, enabled: Boolean = true): Long {
        val qty = quantity.coerceAtLeast(1)
        val res = resistance.coerceAtLeast(1)
        if (id != 0L) {
            bandDao.update(BandEntity(id = id, resistance = res, quantity = qty, enabled = enabled))
            return id
        }
        val existing = bandDao.getByResistance(res)
        return if (existing != null) {
            bandDao.update(existing.copy(quantity = qty, enabled = enabled))
            existing.id
        } else {
            bandDao.insert(BandEntity(resistance = res, quantity = qty, enabled = enabled))
        }
    }

    suspend fun deleteBand(band: BandEntity) = bandDao.delete(band)

    fun observeHistory(): Flow<List<SessionWithLogs>> = sessionDao.observeAllSessions()

    fun observeCompletedHistory(): Flow<List<SessionWithLogs>> =
        sessionDao.observeAllSessions().map { list -> list.filter { it.session.completed } }

    fun observeLastCompleted(): Flow<SessionWithLogs?> = sessionDao.observeLastCompleted()

    fun observeRecentCompleted(limit: Int = 8): Flow<List<SessionWithLogs>> =
        sessionDao.observeRecentCompleted(limit)

    suspend fun getCompletedSessionsInRange(startMillis: Long, endMillis: Long) =
        sessionDao.getCompletedInRange(startMillis, endMillis)

    fun observeIncomplete(): Flow<SessionWithLogs?> = sessionDao.observeIncomplete()

    fun observeSession(sessionId: Long): Flow<SessionWithLogs?> =
        sessionDao.observeSessionWithLogs(sessionId)

    suspend fun getSession(sessionId: Long): SessionWithLogs? =
        sessionDao.getSessionWithLogs(sessionId)

    fun observeWeeklyCompletedCount(weekStart: Long, weekEnd: Long): Flow<Int> =
        sessionDao.observeCompletedCountInRange(weekStart, weekEnd)

    suspend fun getPreviousPerformance(exerciseId: Long): PreviousPerformance? {
        val log = exerciseDao.getRecentCompletedLogs(exerciseId, limit = 1).firstOrNull() ?: return null
        return PreviousPerformanceFormatter.fromLog(log)
    }

    suspend fun getPreviousPerformanceMap(exerciseIds: List<Long>): Map<Long, PreviousPerformance> {
        return exerciseIds.mapNotNull { id ->
            getPreviousPerformance(id)?.let { id to it }
        }.toMap()
    }

    suspend fun getProgressionSuggestionMap(exerciseIds: List<Long>): Map<Long, ProgressionSuggestion> {
        val combinations = availableCombinations()
        return exerciseIds.associateWith { id ->
            val log = exerciseDao.getRecentCompletedLogs(id, limit = 1).firstOrNull()
            val exercise = exerciseDao.getById(id)
            if (log == null || exercise == null) {
                ProgressionEngine.suggest(
                    targetMin = exercise?.targetMin ?: 0,
                    targetMax = exercise?.targetMax ?: 0,
                    unit = exercise?.targetUnit ?: TargetUnit.REPS,
                    previousValues = emptyList(),
                    previousResistance = 0,
                    combinations = combinations
                )
            } else {
                ProgressionEngine.suggestFromLog(log, combinations)
            }
        }
    }

    /** Starts a new workout. Any existing incomplete session is discarded. */
    suspend fun startWorkout(mode: TrainingMode): Long {
        sessionDao.getIncomplete()?.let { sessionDao.deleteSession(it.id) }

        val settings = getSettings()
        val workoutType = settings.nextWorkoutType
        val exercises = WorkoutPlanner.selectExercises(
            allForType = getExercises(workoutType),
            mode = mode
        )
        val combinations = availableCombinations()
        val now = System.currentTimeMillis()
        val sessionId = sessionDao.insertSession(
            WorkoutSessionEntity(
                dateTime = now,
                workoutType = workoutType,
                trainingMode = mode,
                completed = false,
                updatedAt = now
            )
        )

        exercises.forEachIndexed { index, exercise ->
            val prescribed = WorkoutPlanner.prescribedSets(mode, settings.defaultSets)
            val logId = sessionDao.insertLog(
                ExerciseLogEntity(
                    sessionId = sessionId,
                    exerciseId = exercise.id,
                    skipped = false,
                    sortOrder = index,
                    prescribedSets = prescribed
                )
            )
            for (setNumber in 1..prescribed) {
                if (exercise.isUnilateral) {
                    sessionDao.insertSet(
                        ExerciseSetEntity(
                            exerciseLogId = logId,
                            setNumber = setNumber,
                            side = SetSide.LEFT
                        )
                    )
                    sessionDao.insertSet(
                        ExerciseSetEntity(
                            exerciseLogId = logId,
                            setNumber = setNumber,
                            side = SetSide.RIGHT
                        )
                    )
                } else {
                    sessionDao.insertSet(
                        ExerciseSetEntity(
                            exerciseLogId = logId,
                            setNumber = setNumber,
                            side = null
                        )
                    )
                }
            }
            // Carry over last resistance (not reps) when available.
            val previous = getPreviousPerformance(exercise.id)
            if (previous != null && previous.totalResistance > 0) {
                val combo = combinations.firstOrNull { it.totalResistance == previous.totalResistance }
                    ?: BandCombination(previous.totalResistance, emptyMap())
                applyCombinationToExerciseLog(logId, combo)
            }
        }
        return sessionId
    }

    suspend fun applyCombinationToExerciseLog(logId: Long, combination: BandCombination) {
        val log = sessionDao.getLogById(logId) ?: return
        val session = sessionDao.getSessionWithLogs(log.sessionId) ?: return
        val logWithSets = session.logs.firstOrNull { it.log.id == logId } ?: return
        val bandsByResistance = getEnabledBands().associateBy { it.resistance }

        logWithSets.sets.forEach { sw ->
            sessionDao.clearSetBands(sw.set.id)
            sessionDao.updateSet(sw.set.copy(totalResistance = combination.totalResistance))
            val refs = combination.countsByResistance.mapNotNull { (resistance, count) ->
                val band = bandsByResistance[resistance] ?: return@mapNotNull null
                SetBandCrossRef(
                    setId = sw.set.id,
                    bandId = band.id,
                    countUsed = count
                )
            }
            if (refs.isNotEmpty()) {
                sessionDao.insertSetBands(refs)
            }
        }
        sessionDao.updateSession(session.session.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateSetValue(
        setId: Long,
        value: Int?,
        unit: TargetUnit,
        totalResistance: Int? = null
    ) {
        val current = sessionDao.getSetById(setId) ?: return
        val updated = when (unit) {
            TargetUnit.REPS -> current.copy(
                reps = value,
                durationSeconds = null,
                totalResistance = totalResistance ?: current.totalResistance
            )
            TargetUnit.SECONDS -> current.copy(
                durationSeconds = value,
                reps = null,
                totalResistance = totalResistance ?: current.totalResistance
            )
        }
        sessionDao.updateSet(updated)
        touchSessionForSet(setId)
    }

    suspend fun updateSetResistance(setId: Long, totalResistance: Int) {
        val current = sessionDao.getSetById(setId) ?: return
        sessionDao.updateSet(current.copy(totalResistance = totalResistance.coerceAtLeast(0)))
        touchSessionForSet(setId)
    }

    suspend fun skipExercise(logId: Long) {
        val log = sessionDao.getLogById(logId) ?: return
        sessionDao.updateLog(log.copy(skipped = true))
        val session = sessionDao.getSessionWithLogs(log.sessionId) ?: return
        sessionDao.updateSession(session.session.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun unskipExercise(logId: Long) {
        val log = sessionDao.getLogById(logId) ?: return
        sessionDao.updateLog(log.copy(skipped = false))
        val session = sessionDao.getSessionWithLogs(log.sessionId) ?: return
        sessionDao.updateSession(session.session.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun completeWorkout(sessionId: Long, feeling: Int?): SessionWithLogs? {
        val withLogs = sessionDao.getSessionWithLogs(sessionId) ?: return null
        val now = System.currentTimeMillis()
        sessionDao.updateSession(
            withLogs.session.copy(
                completed = true,
                feeling = feeling?.coerceIn(1, 5),
                updatedAt = now
            )
        )
        val settings = getSettings()
        settingsDao.upsert(
            settings.copy(nextWorkoutType = WorkoutPlanner.nextType(withLogs.session.workoutType))
        )
        traineeRepository?.awardStrengthCompletion()
        return sessionDao.getSessionWithLogs(sessionId)
    }

    suspend fun deleteSessionAndRecomputeNext(sessionId: Long) {
        sessionDao.deleteSession(sessionId)
        val completed = sessionDao.getAllCompletedOrdered()
        val next = WorkoutPlanner.recomputeNextType(completed.map { it.workoutType })
        val settings = getSettings()
        settingsDao.upsert(settings.copy(nextWorkoutType = next))
    }

    private suspend fun touchSessionForSet(setId: Long) {
        val sessionId = sessionDao.getSessionIdForSet(setId) ?: return
        val session = sessionDao.getSessionWithLogs(sessionId) ?: return
        sessionDao.updateSession(session.session.copy(updatedAt = System.currentTimeMillis()))
    }

    /** Keeps one row per plan slot when legacy DB still has duplicate exercise seeds. */
    private fun List<ExerciseEntity>.distinctByWorkoutSlot(): List<ExerciseEntity> =
        groupBy { it.sortOrder }
            .map { (_, group) -> group.minBy { it.id } }
            .sortedBy { it.sortOrder }
}
