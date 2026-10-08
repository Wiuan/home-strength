package com.homestrength.data.backup

import com.homestrength.data.local.entity.AppSettingsEntity
import com.homestrength.data.local.entity.BandEntity
import com.homestrength.data.local.entity.ExerciseEntity
import com.homestrength.data.local.entity.ExerciseLogEntity
import com.homestrength.data.local.entity.ExerciseSetEntity
import com.homestrength.data.local.entity.LightPracticeEntity
import com.homestrength.data.local.entity.MovementRole
import com.homestrength.data.local.entity.PeriodGoalEntity
import com.homestrength.data.local.entity.PeriodGoalStatus
import com.homestrength.data.local.entity.PowerItemStatus
import com.homestrength.data.local.entity.PowerListItemEntity
import com.homestrength.data.local.entity.PracticeTrack
import com.homestrength.data.local.entity.RewardEntity
import com.homestrength.data.local.entity.RewardRedemptionEntity
import com.homestrength.data.local.entity.SetBandCrossRef
import com.homestrength.data.local.entity.SetSide
import com.homestrength.data.local.entity.TargetUnit
import com.homestrength.data.local.entity.TraineeProfileEntity
import com.homestrength.data.local.entity.TrainingMode
import com.homestrength.data.local.entity.WorkoutSessionEntity
import com.homestrength.data.local.entity.WorkoutType
import com.homestrength.domain.trainee.PlanPeriodKind
import org.json.JSONArray
import org.json.JSONObject

/**
 * Versioned JSON backup for phone migration / reinstall.
 * Format marker: [FORMAT_ID].
 */
object BackupCodec {
    const val FORMAT_ID = "home-strength-backup"
    const val FORMAT_VERSION = 1

    data class Payload(
        val exportedAt: Long,
        val dbVersion: Int,
        val profile: TraineeProfileEntity,
        val settings: AppSettingsEntity,
        val exercises: List<ExerciseEntity>,
        val bands: List<BandEntity>,
        val sessions: List<WorkoutSessionEntity>,
        val logs: List<ExerciseLogEntity>,
        val sets: List<ExerciseSetEntity>,
        val setBands: List<SetBandCrossRef>,
        val powerList: List<PowerListItemEntity>,
        val lightPractices: List<LightPracticeEntity>,
        val periodGoals: List<PeriodGoalEntity>,
        val rewards: List<RewardEntity>,
        val redemptions: List<RewardRedemptionEntity>
    )

    fun encode(payload: Payload): String {
        val root = JSONObject()
        root.put("format", FORMAT_ID)
        root.put("version", FORMAT_VERSION)
        root.put("exportedAt", payload.exportedAt)
        root.put("dbVersion", payload.dbVersion)
        root.put("profile", profileToJson(payload.profile))
        root.put("settings", settingsToJson(payload.settings))
        root.put("exercises", JSONArray().also { arr ->
            payload.exercises.forEach { arr.put(exerciseToJson(it)) }
        })
        root.put("bands", JSONArray().also { arr ->
            payload.bands.forEach { arr.put(bandToJson(it)) }
        })
        root.put("sessions", JSONArray().also { arr ->
            payload.sessions.forEach { arr.put(sessionToJson(it)) }
        })
        root.put("logs", JSONArray().also { arr ->
            payload.logs.forEach { arr.put(logToJson(it)) }
        })
        root.put("sets", JSONArray().also { arr ->
            payload.sets.forEach { arr.put(setToJson(it)) }
        })
        root.put("setBands", JSONArray().also { arr ->
            payload.setBands.forEach { arr.put(setBandToJson(it)) }
        })
        root.put("powerList", JSONArray().also { arr ->
            payload.powerList.forEach { arr.put(powerToJson(it)) }
        })
        root.put("lightPractices", JSONArray().also { arr ->
            payload.lightPractices.forEach { arr.put(lightToJson(it)) }
        })
        root.put("periodGoals", JSONArray().also { arr ->
            payload.periodGoals.forEach { arr.put(periodGoalToJson(it)) }
        })
        root.put("rewards", JSONArray().also { arr ->
            payload.rewards.forEach { arr.put(rewardToJson(it)) }
        })
        root.put("redemptions", JSONArray().also { arr ->
            payload.redemptions.forEach { arr.put(redemptionToJson(it)) }
        })
        return root.toString(2)
    }

    fun decode(json: String): Payload {
        val root = JSONObject(json)
        require(root.optString("format") == FORMAT_ID) { "不是练习生备份文件" }
        val version = root.optInt("version", 0)
        require(version in 1..FORMAT_VERSION) { "不支持的备份版本: $version" }
        return Payload(
            exportedAt = root.optLong("exportedAt", 0L),
            dbVersion = root.optInt("dbVersion", 0),
            profile = profileFromJson(root.getJSONObject("profile")),
            settings = settingsFromJson(root.getJSONObject("settings")),
            exercises = root.optJSONArray("exercises").mapObjects(::exerciseFromJson),
            bands = root.optJSONArray("bands").mapObjects(::bandFromJson),
            sessions = root.optJSONArray("sessions").mapObjects(::sessionFromJson),
            logs = root.optJSONArray("logs").mapObjects(::logFromJson),
            sets = root.optJSONArray("sets").mapObjects(::setFromJson),
            setBands = root.optJSONArray("setBands").mapObjects(::setBandFromJson),
            powerList = root.optJSONArray("powerList").mapObjects(::powerFromJson),
            lightPractices = root.optJSONArray("lightPractices").mapObjects(::lightFromJson),
            periodGoals = root.optJSONArray("periodGoals").mapObjects(::periodGoalFromJson),
            rewards = root.optJSONArray("rewards").mapObjects(::rewardFromJson),
            redemptions = root.optJSONArray("redemptions").mapObjects(::redemptionFromJson)
        )
    }

    private fun <T> JSONArray?.mapObjects(mapper: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) add(mapper(getJSONObject(i)))
        }
    }

    private fun profileToJson(p: TraineeProfileEntity) = JSONObject()
        .put("id", p.id)
        .put("nickname", p.nickname)
        .put("fans", p.fans)
        .put("coins", p.coins)
        .put("energy", p.energy)
        .put("energyDate", p.energyDate)
        .put("showCultivation", p.showCultivation)
        .put("checklistFans", p.checklistFans)
        .put("checklistCoins", p.checklistCoins)
        .put("lightFans", p.lightFans)
        .put("lightCoins", p.lightCoins)
        .put("strengthFans", p.strengthFans)
        .put("strengthCoins", p.strengthCoins)
        .put("sleepEnergyRestore", p.sleepEnergyRestore)
        .put("sleepFans", p.sleepFans)
        .put("sleepCoins", p.sleepCoins)
        .put("sleepDate", p.sleepDate)
        .put("meditationEnergyRestore", p.meditationEnergyRestore)
        .put("meditationFans", p.meditationFans)
        .put("meditationCoins", p.meditationCoins)
        .put("celebratedGrade", p.celebratedGrade)
        .put("coinsPerYuan", p.coinsPerYuan)

    private fun profileFromJson(o: JSONObject) = TraineeProfileEntity(
        id = o.optInt("id", 1),
        nickname = o.optString("nickname", "W"),
        fans = o.optInt("fans"),
        coins = o.optInt("coins"),
        energy = o.optInt("energy", 100),
        energyDate = o.optString("energyDate", ""),
        showCultivation = o.optBoolean("showCultivation", true),
        checklistFans = o.optInt("checklistFans", 20),
        checklistCoins = o.optInt("checklistCoins", 3),
        lightFans = o.optInt("lightFans", 40),
        lightCoins = o.optInt("lightCoins", 5),
        strengthFans = o.optInt("strengthFans", 100),
        strengthCoins = o.optInt("strengthCoins", 12),
        sleepEnergyRestore = o.optInt("sleepEnergyRestore", 20),
        sleepFans = o.optInt("sleepFans", 10),
        sleepCoins = o.optInt("sleepCoins", 2),
        sleepDate = o.optString("sleepDate", ""),
        meditationEnergyRestore = o.optInt("meditationEnergyRestore", 5),
        meditationFans = o.optInt("meditationFans", 5),
        meditationCoins = o.optInt("meditationCoins", 1),
        celebratedGrade = o.optString("celebratedGrade", ""),
        coinsPerYuan = o.optInt("coinsPerYuan", 10)
    )

    private fun settingsToJson(s: AppSettingsEntity) = JSONObject()
        .put("id", s.id)
        .put("nextWorkoutType", s.nextWorkoutType.name)
        .put("weeklyGoal", s.weeklyGoal)
        .put("defaultSets", s.defaultSets)
        .put("defaultRestSeconds", s.defaultRestSeconds)

    private fun settingsFromJson(o: JSONObject) = AppSettingsEntity(
        id = o.optInt("id", 1),
        nextWorkoutType = enumValue(o.optString("nextWorkoutType"), WorkoutType.A),
        weeklyGoal = o.optInt("weeklyGoal", 3),
        defaultSets = o.optInt("defaultSets", 2),
        defaultRestSeconds = o.optInt("defaultRestSeconds", 90)
    )

    private fun exerciseToJson(e: ExerciseEntity) = JSONObject()
        .put("id", e.id)
        .put("name", e.name)
        .put("workoutType", e.workoutType.name)
        .put("targetMin", e.targetMin)
        .put("targetMax", e.targetMax)
        .put("targetUnit", e.targetUnit.name)
        .put("defaultSets", e.defaultSets)
        .put("isUnilateral", e.isUnilateral)
        .put("description", e.description)
        .put("sortOrder", e.sortOrder)
        .put("movementRole", e.movementRole.name)

    private fun exerciseFromJson(o: JSONObject) = ExerciseEntity(
        id = o.getLong("id"),
        name = o.getString("name"),
        workoutType = enumValue(o.getString("workoutType"), WorkoutType.A),
        targetMin = o.getInt("targetMin"),
        targetMax = o.getInt("targetMax"),
        targetUnit = enumValue(o.getString("targetUnit"), TargetUnit.REPS),
        defaultSets = o.optInt("defaultSets", 2),
        isUnilateral = o.optBoolean("isUnilateral", false),
        description = o.optString("description", ""),
        sortOrder = o.getInt("sortOrder"),
        movementRole = enumValue(o.optString("movementRole"), MovementRole.PUSH)
    )

    private fun bandToJson(b: BandEntity) = JSONObject()
        .put("id", b.id)
        .put("resistance", b.resistance)
        .put("quantity", b.quantity)
        .put("enabled", b.enabled)

    private fun bandFromJson(o: JSONObject) = BandEntity(
        id = o.getLong("id"),
        resistance = o.getInt("resistance"),
        quantity = o.optInt("quantity", 1),
        enabled = o.optBoolean("enabled", true)
    )

    private fun sessionToJson(s: WorkoutSessionEntity) = JSONObject()
        .put("id", s.id)
        .put("dateTime", s.dateTime)
        .put("workoutType", s.workoutType.name)
        .put("trainingMode", s.trainingMode.name)
        .put("feeling", s.feeling)
        .put("completed", s.completed)
        .put("updatedAt", s.updatedAt)

    private fun sessionFromJson(o: JSONObject) = WorkoutSessionEntity(
        id = o.getLong("id"),
        dateTime = o.getLong("dateTime"),
        workoutType = enumValue(o.getString("workoutType"), WorkoutType.A),
        trainingMode = enumValue(o.getString("trainingMode"), TrainingMode.NORMAL),
        feeling = if (o.isNull("feeling")) null else o.getInt("feeling"),
        completed = o.optBoolean("completed", false),
        updatedAt = o.optLong("updatedAt", o.getLong("dateTime"))
    )

    private fun logToJson(l: ExerciseLogEntity) = JSONObject()
        .put("id", l.id)
        .put("sessionId", l.sessionId)
        .put("exerciseId", l.exerciseId)
        .put("skipped", l.skipped)
        .put("sortOrder", l.sortOrder)
        .put("prescribedSets", l.prescribedSets)

    private fun logFromJson(o: JSONObject) = ExerciseLogEntity(
        id = o.getLong("id"),
        sessionId = o.getLong("sessionId"),
        exerciseId = o.getLong("exerciseId"),
        skipped = o.optBoolean("skipped", false),
        sortOrder = o.getInt("sortOrder"),
        prescribedSets = o.getInt("prescribedSets")
    )

    private fun setToJson(s: ExerciseSetEntity) = JSONObject()
        .put("id", s.id)
        .put("exerciseLogId", s.exerciseLogId)
        .put("setNumber", s.setNumber)
        .put("reps", s.reps)
        .put("durationSeconds", s.durationSeconds)
        .put("side", s.side?.name)
        .put("totalResistance", s.totalResistance)

    private fun setFromJson(o: JSONObject) = ExerciseSetEntity(
        id = o.getLong("id"),
        exerciseLogId = o.getLong("exerciseLogId"),
        setNumber = o.getInt("setNumber"),
        reps = if (o.isNull("reps")) null else o.getInt("reps"),
        durationSeconds = if (o.isNull("durationSeconds")) null else o.getInt("durationSeconds"),
        side = o.nullableString("side")?.let { enumValue(it, SetSide.LEFT) },
        totalResistance = o.optInt("totalResistance", 0)
    )

    private fun setBandToJson(r: SetBandCrossRef) = JSONObject()
        .put("setId", r.setId)
        .put("bandId", r.bandId)
        .put("countUsed", r.countUsed)

    private fun setBandFromJson(o: JSONObject) = SetBandCrossRef(
        setId = o.getLong("setId"),
        bandId = o.getLong("bandId"),
        countUsed = o.optInt("countUsed", 1)
    )

    private fun powerToJson(p: PowerListItemEntity) = JSONObject()
        .put("id", p.id)
        .put("dayKey", p.dayKey)
        .put("title", p.title)
        .put("track", p.track?.name)
        .put("status", p.status.name)
        .put("sortOrder", p.sortOrder)
        .put("note", p.note)
        .put("rewardFans", p.rewardFans)
        .put("rewardCoins", p.rewardCoins)
        .put("targetDurationSeconds", p.targetDurationSeconds)
        .put("practicedMinutes", p.practicedMinutes)
        .put("linkedPracticeId", p.linkedPracticeId)
        .put("completedViaPractice", p.completedViaPractice)

    private fun powerFromJson(o: JSONObject) = PowerListItemEntity(
        id = o.getLong("id"),
        dayKey = o.getString("dayKey"),
        title = o.getString("title"),
        track = o.nullableString("track")?.let { enumValue(it, PracticeTrack.LIFE) },
        status = enumValue(o.optString("status"), PowerItemStatus.PENDING),
        sortOrder = o.optInt("sortOrder"),
        note = o.optString("note", ""),
        rewardFans = o.optInt("rewardFans"),
        rewardCoins = o.optInt("rewardCoins"),
        targetDurationSeconds = o.optInt("targetDurationSeconds"),
        practicedMinutes = o.optInt("practicedMinutes"),
        linkedPracticeId = o.optLong("linkedPracticeId"),
        completedViaPractice = o.optBoolean("completedViaPractice", false)
    )

    private fun lightToJson(l: LightPracticeEntity) = JSONObject()
        .put("id", l.id)
        .put("track", l.track.name)
        .put("startedAt", l.startedAt)
        .put("durationSeconds", l.durationSeconds)
        .put("note", l.note)
        .put("noteTitle", l.noteTitle)
        .put("completed", l.completed)
        .put("fansEarned", l.fansEarned)
        .put("coinsEarned", l.coinsEarned)

    private fun lightFromJson(o: JSONObject) = LightPracticeEntity(
        id = o.getLong("id"),
        track = enumValue(o.getString("track"), PracticeTrack.CULTIVATION),
        startedAt = o.getLong("startedAt"),
        durationSeconds = o.optInt("durationSeconds"),
        note = o.optString("note", ""),
        noteTitle = o.optString("noteTitle", ""),
        completed = o.optBoolean("completed", false),
        fansEarned = o.optInt("fansEarned"),
        coinsEarned = o.optInt("coinsEarned")
    )

    private fun periodGoalToJson(g: PeriodGoalEntity) = JSONObject()
        .put("id", g.id)
        .put("periodKind", g.periodKind.name)
        .put("periodKey", g.periodKey)
        .put("title", g.title)
        .put("track", g.track?.name)
        .put("status", g.status.name)
        .put("sortOrder", g.sortOrder)
        .put("targetParts", g.targetParts)
        .put("doneParts", g.doneParts)

    private fun periodGoalFromJson(o: JSONObject) = PeriodGoalEntity(
        id = o.getLong("id"),
        periodKind = enumValue(o.getString("periodKind"), PlanPeriodKind.MONTH),
        periodKey = o.getString("periodKey"),
        title = o.getString("title"),
        track = o.nullableString("track")?.let { enumValue(it, PracticeTrack.LIFE) },
        status = enumValue(o.optString("status"), PeriodGoalStatus.ACTIVE),
        sortOrder = o.optInt("sortOrder"),
        targetParts = o.optInt("targetParts", 1),
        doneParts = o.optInt("doneParts")
    )

    private fun rewardToJson(r: RewardEntity) = JSONObject()
        .put("id", r.id)
        .put("title", r.title)
        .put("cost", r.cost)
        .put("sortOrder", r.sortOrder)

    private fun rewardFromJson(o: JSONObject) = RewardEntity(
        id = o.getLong("id"),
        title = o.getString("title"),
        cost = o.getInt("cost"),
        sortOrder = o.optInt("sortOrder")
    )

    private fun redemptionToJson(r: RewardRedemptionEntity) = JSONObject()
        .put("id", r.id)
        .put("rewardId", r.rewardId)
        .put("title", r.title)
        .put("cost", r.cost)
        .put("redeemedAt", r.redeemedAt)

    private fun redemptionFromJson(o: JSONObject) = RewardRedemptionEntity(
        id = o.getLong("id"),
        rewardId = o.getLong("rewardId"),
        title = o.getString("title"),
        cost = o.getInt("cost"),
        redeemedAt = o.getLong("redeemedAt")
    )

    private inline fun <reified T : Enum<T>> enumValue(name: String?, default: T): T {
        if (name.isNullOrBlank()) return default
        return runCatching { enumValueOf<T>(name) }.getOrDefault(default)
    }

    private fun JSONObject.nullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val value = optString(key, "")
        return value.takeIf { it.isNotBlank() && it != "null" }
    }
}
