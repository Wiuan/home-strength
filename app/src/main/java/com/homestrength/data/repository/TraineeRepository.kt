package com.homestrength.data.repository

import com.homestrength.data.local.dao.TraineeDao
import com.homestrength.data.local.entity.LightPracticeEntity
import com.homestrength.data.local.entity.PeriodGoalEntity
import com.homestrength.data.local.entity.PeriodGoalStatus
import com.homestrength.data.local.entity.PowerItemStatus
import com.homestrength.data.local.entity.PowerListItemEntity
import com.homestrength.data.local.entity.PracticeTrack
import com.homestrength.data.local.entity.RewardEntity
import com.homestrength.data.local.entity.RewardRedemptionEntity
import com.homestrength.data.local.entity.TraineeProfileEntity
import com.homestrength.data.local.entity.WorkoutSessionEntity
import com.homestrength.domain.trainee.MonthReview
import com.homestrength.domain.trainee.MonthReviewBuilder
import com.homestrength.domain.trainee.PeriodReview
import com.homestrength.domain.trainee.PeriodReviewBuilder
import com.homestrength.domain.trainee.PlanPeriod
import com.homestrength.domain.trainee.PlanPeriodKind
import com.homestrength.domain.trainee.TraineeGrade
import com.homestrength.domain.trainee.TraineeRewards
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class TraineeRepository(
    private val traineeDao: TraineeDao
) {
    private val zone = ZoneId.systemDefault()

    fun todayKey(): String = LocalDate.now(zone).toString()

    fun observeProfile(): Flow<TraineeProfileEntity> =
        traineeDao.observeProfile().map { it ?: TraineeProfileEntity() }

    suspend fun getProfile(): TraineeProfileEntity =
        ensureEnergyRefreshed(traineeDao.getProfile() ?: TraineeProfileEntity().also {
            traineeDao.upsertProfile(it)
        })

    fun observePowerList(): Flow<List<PowerListItemEntity>> =
        traineeDao.observePowerList(todayKey())

    suspend fun addPowerItem(title: String, track: PracticeTrack?): Boolean {
        val day = todayKey()
        val current = traineeDao.getPowerList(day)
        if (current.size >= 5) return false
        // Custom备注 → 生活；阅读/书法历史值归一到修养
        val resolved = canonicalTrack(track ?: PracticeTrack.LIFE)
        traineeDao.insertPowerItem(
            PowerListItemEntity(
                dayKey = day,
                title = title.trim().ifBlank { trackLabel(resolved) },
                track = resolved,
                sortOrder = current.size
            )
        )
        return true
    }

    suspend fun togglePowerItemDone(id: Long) {
        val day = todayKey()
        val item = traineeDao.getPowerList(day).firstOrNull { it.id == id } ?: return
        if (item.status == PowerItemStatus.DONE) {
            if (item.rewardFans > 0 || item.rewardCoins > 0) {
                award(-item.rewardFans, -item.rewardCoins, energyDelta = 0)
            }
            if (item.track == PracticeTrack.SLEEP) {
                undoEarlySleepState()
            }
            if (item.track == PracticeTrack.MEDITATION) {
                undoMeditationEnergy()
            }
            traineeDao.updatePowerItem(
                item.copy(status = PowerItemStatus.PENDING, rewardFans = 0, rewardCoins = 0)
            )
        } else {
            val profile = getProfile()
            val fans = profile.checklistFans
            val coins = profile.checklistCoins
            award(fans, coins, energyDelta = 0)
            traineeDao.updatePowerItem(
                item.copy(
                    status = PowerItemStatus.DONE,
                    rewardFans = fans,
                    rewardCoins = coins
                )
            )
        }
    }

    suspend fun deletePowerItem(id: Long) {
        val day = todayKey()
        val item = traineeDao.getPowerList(day).firstOrNull { it.id == id }
        if (item != null && item.status == PowerItemStatus.DONE) {
            if (item.rewardFans > 0 || item.rewardCoins > 0) {
                award(-item.rewardFans, -item.rewardCoins, energyDelta = 0)
            }
            if (item.track == PracticeTrack.SLEEP) {
                undoEarlySleepState()
            }
            if (item.track == PracticeTrack.MEDITATION) {
                undoMeditationEnergy()
            }
        }
        traineeDao.deletePowerItem(id)
    }

    suspend fun startLightPractice(track: PracticeTrack): Long {
        val canonical = canonicalTrack(track)
        require(canonical in timedLightTracks) { "This track is not a timed light practice" }
        return traineeDao.insertLightPractice(
            LightPracticeEntity(
                track = canonical,
                startedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getLightPractice(id: Long): LightPracticeEntity? = traineeDao.getLightPractice(id)

    /**
     * Completes a light track practice and awards fans/coins.
     * @return updated practice or null
     */
    suspend fun completeLightPractice(
        id: Long,
        durationSeconds: Int,
        note: String,
        noteTitle: String
    ): LightPracticeEntity? {
        val current = traineeDao.getLightPractice(id) ?: return null
        if (current.completed) return current
        val profile = getProfile()
        val fans = profile.lightFans
        val coins = profile.lightCoins
        val updated = current.copy(
            durationSeconds = durationSeconds.coerceAtLeast(0),
            note = note.trim(),
            noteTitle = noteTitle.trim(),
            completed = true,
            fansEarned = fans,
            coinsEarned = coins
        )
        traineeDao.updateLightPractice(updated)
        award(fans, coins, energyDelta = -10)
        val track = canonicalTrack(current.track)
        addPowerItemIfRoom(
            title = trackLabel(track),
            track = track,
            markDone = true,
            rewardFans = fans,
            rewardCoins = coins
        )
        return updated
    }

    suspend fun awardStrengthCompletion() {
        val profile = getProfile()
        val fans = profile.strengthFans
        val coins = profile.strengthCoins
        award(fans, coins, energyDelta = -25)
        addPowerItemIfRoom(
            title = "力量训练",
            track = PracticeTrack.STRENGTH,
            markDone = true,
            rewardFans = fans,
            rewardCoins = coins
        )
    }

    /**
     * Early sleep check-in: restore energy once per day.
     * @return null on success, otherwise a short message for the UI.
     */
    suspend fun completeEarlySleep(): String? {
        val profile = getProfile()
        val today = todayKey()
        if (profile.sleepDate == today) {
            return "今天已经早睡打卡过了"
        }
        val restore = profile.sleepEnergyRestore.coerceIn(1, TraineeRewards.ENERGY_MAX)
        val fans = profile.sleepFans.coerceAtLeast(0)
        val coins = profile.sleepCoins.coerceAtLeast(0)
        val energy = (profile.energy + restore).coerceIn(0, TraineeRewards.ENERGY_MAX)
        traineeDao.upsertProfile(
            profile.copy(
                fans = (profile.fans + fans).coerceAtLeast(0),
                coins = (profile.coins + coins).coerceAtLeast(0),
                energy = energy,
                sleepDate = today
            )
        )
        addPowerItemIfRoom(
            title = "早睡",
            track = PracticeTrack.SLEEP,
            markDone = true,
            rewardFans = fans,
            rewardCoins = coins
        )
        return null
    }

    /**
     * Meditation check-in: small energy restore, no daily limit (energy still caps at max).
     */
    suspend fun completeMeditation() {
        val profile = getProfile()
        val restore = profile.meditationEnergyRestore.coerceIn(0, 50)
        val fans = profile.meditationFans.coerceAtLeast(0)
        val coins = profile.meditationCoins.coerceAtLeast(0)
        award(fans, coins, energyDelta = restore)
        addPowerItemIfRoom(
            title = "冥想",
            track = PracticeTrack.MEDITATION,
            markDone = true,
            rewardFans = fans,
            rewardCoins = coins
        )
    }

    /** Undo today's early sleep (list item, or lock-only if the list was full). */
    suspend fun undoEarlySleep() {
        val day = todayKey()
        val item = traineeDao.getPowerList(day).firstOrNull {
            it.track == PracticeTrack.SLEEP && it.status == PowerItemStatus.DONE
        }
        if (item != null) {
            deletePowerItem(item.id)
            return
        }
        val profile = getProfile()
        if (profile.sleepDate != day) return
        val fans = profile.sleepFans
        val coins = profile.sleepCoins
        undoEarlySleepState()
        if (fans > 0 || coins > 0) {
            award(-fans, -coins, energyDelta = 0)
        }
    }

    suspend fun updateNickname(nickname: String) {
        val profile = getProfile()
        traineeDao.upsertProfile(profile.copy(nickname = nickname.trim().ifBlank { "W" }))
    }

    /**
     * Marks the current grade as celebrated so fireworks do not replay.
     * @return the new grade if this is a fresh upgrade worth celebrating, otherwise null.
     */
    suspend fun consumeGradeUpgradeIfAny(): TraineeGrade? {
        val profile = getProfile()
        val current = TraineeGrade.fromFans(profile.fans)
        if (profile.celebratedGrade.isBlank()) {
            traineeDao.upsertProfile(profile.copy(celebratedGrade = current.name))
            return null
        }
        val celebrated = runCatching { TraineeGrade.valueOf(profile.celebratedGrade) }
            .getOrDefault(TraineeGrade.F)
        if (current.ordinal <= celebrated.ordinal) return null
        traineeDao.upsertProfile(profile.copy(celebratedGrade = current.name))
        return current
    }

    fun observeRecentLightPractices(limit: Int = 12): Flow<List<LightPracticeEntity>> =
        traineeDao.observeRecentLightPractices(limit)

    fun observePeriodGoals(period: PlanPeriod): Flow<List<PeriodGoalEntity>> =
        traineeDao.observePeriodGoals(period.kind, period.key)

    fun currentMonthKey(): String = YearMonth.now(zone).toString()

    fun goalLimit(kind: PlanPeriodKind): Int = when (kind) {
        PlanPeriodKind.MONTH -> 5
        PlanPeriodKind.QUARTER -> 8
        PlanPeriodKind.YEAR -> 12
    }

    suspend fun addPeriodGoal(
        title: String,
        period: PlanPeriod,
        track: PracticeTrack? = null,
        targetParts: Int = 1
    ): String? {
        val cleaned = title.trim()
        if (cleaned.isBlank()) {
            return when (period.kind) {
                PlanPeriodKind.MONTH -> "写一句本月想做的事"
                PlanPeriodKind.QUARTER -> "写一句本季想做的事"
                PlanPeriodKind.YEAR -> "写一句今年想做的事"
            }
        }
        val current = traineeDao.getPeriodGoals(period.kind, period.key)
        val limit = goalLimit(period.kind)
        if (current.size >= limit) {
            return when (period.kind) {
                PlanPeriodKind.MONTH -> "该月最多 $limit 条计划"
                PlanPeriodKind.QUARTER -> "该季最多 $limit 条计划"
                PlanPeriodKind.YEAR -> "该年最多 $limit 条计划"
            }
        }
        val parts = targetParts.coerceIn(1, 366)
        traineeDao.insertPeriodGoal(
            PeriodGoalEntity(
                periodKind = period.kind,
                periodKey = period.key,
                title = cleaned,
                track = track?.let { canonicalTrack(it) },
                sortOrder = current.size,
                targetParts = parts,
                doneParts = 0
            )
        )
        return null
    }

    suspend fun setPeriodGoalStatus(id: Long, status: PeriodGoalStatus) {
        val goal = traineeDao.getPeriodGoal(id) ?: return
        val doneParts = when (status) {
            PeriodGoalStatus.DONE -> goal.targetParts.coerceAtLeast(1)
            PeriodGoalStatus.ACTIVE -> goal.doneParts.coerceAtMost(goal.targetParts - 1).coerceAtLeast(0)
            PeriodGoalStatus.DROPPED -> goal.doneParts
        }
        traineeDao.updatePeriodGoal(goal.copy(status = status, doneParts = doneParts))
    }

    /** Increment/decrement progress parts (e.g. 1/12 → 2/12). Auto marks DONE at full. */
    suspend fun bumpPeriodGoalProgress(id: Long, delta: Int): String? {
        val goal = traineeDao.getPeriodGoal(id) ?: return "计划不存在"
        if (goal.status == PeriodGoalStatus.DROPPED) return "已放下，先恢复再记进度"
        val target = goal.targetParts.coerceAtLeast(1)
        val next = (goal.doneParts + delta).coerceIn(0, target)
        val status = if (next >= target) PeriodGoalStatus.DONE else PeriodGoalStatus.ACTIVE
        traineeDao.updatePeriodGoal(goal.copy(doneParts = next, status = status))
        return null
    }

    suspend fun deletePeriodGoal(id: Long) {
        traineeDao.deletePeriodGoal(id)
    }

    /**
     * Places a period goal into today's checklist (生活/指定轨道).
     * @return null on success, otherwise a short message.
     */
    suspend fun placePeriodGoalIntoToday(id: Long): String? {
        val goal = traineeDao.getPeriodGoal(id) ?: return "计划不存在"
        val ok = addPowerItem(goal.title, goal.track ?: PracticeTrack.LIFE)
        return if (ok) null else "今天清单已满（最多 5 件）"
    }

    suspend fun loadMonthReview(
        month: YearMonth = YearMonth.now(zone),
        strengthSessions: List<WorkoutSessionEntity>
    ): MonthReview {
        val start = month.atDay(1)
        val endExclusive = month.plusMonths(1).atDay(1)
        val powerDone = traineeDao.getDonePowerItemsInRange(start.toString(), endExclusive.toString())
        val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = endExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
        val inMonth = strengthSessions.filter { session ->
            val day = java.time.Instant.ofEpochMilli(session.dateTime)
                .atZone(zone).toLocalDate()
            !day.isBefore(start) && day.isBefore(endExclusive)
        }
        val lightDone = traineeDao.getCompletedLightInRange(startMillis, endMillis)
        return MonthReviewBuilder.build(
            month = month,
            powerDone = powerDone,
            lightDone = lightDone,
            strengthDone = inMonth,
            zone = zone
        )
    }

    suspend fun loadPeriodReview(
        period: PlanPeriod,
        strengthSessions: List<WorkoutSessionEntity>
    ): PeriodReview {
        val months = period.months().map { loadMonthReview(it, strengthSessions) }
        return PeriodReviewBuilder.fromMonths(period, months)
    }

    /** Clears once-per-day lock and reverses the energy restore from early sleep. */
    private suspend fun undoEarlySleepState() {
        val profile = getProfile()
        if (profile.sleepDate != todayKey()) return
        val restore = profile.sleepEnergyRestore.coerceIn(1, TraineeRewards.ENERGY_MAX)
        traineeDao.upsertProfile(
            profile.copy(
                sleepDate = "",
                energy = (profile.energy - restore).coerceIn(0, TraineeRewards.ENERGY_MAX)
            )
        )
    }

    private suspend fun undoMeditationEnergy() {
        val profile = getProfile()
        val restore = profile.meditationEnergyRestore.coerceIn(0, 50)
        traineeDao.upsertProfile(
            profile.copy(
                energy = (profile.energy - restore).coerceIn(0, TraineeRewards.ENERGY_MAX)
            )
        )
    }

    suspend fun updateRewardRules(
        checklistFans: Int,
        checklistCoins: Int,
        lightFans: Int,
        lightCoins: Int,
        strengthFans: Int,
        strengthCoins: Int,
        sleepEnergyRestore: Int,
        sleepFans: Int,
        sleepCoins: Int,
        meditationEnergyRestore: Int,
        meditationFans: Int,
        meditationCoins: Int
    ) {
        val profile = getProfile()
        traineeDao.upsertProfile(
            profile.copy(
                checklistFans = checklistFans.coerceIn(0, 500),
                checklistCoins = checklistCoins.coerceIn(0, 99),
                lightFans = lightFans.coerceIn(0, 500),
                lightCoins = lightCoins.coerceIn(0, 99),
                strengthFans = strengthFans.coerceIn(0, 999),
                strengthCoins = strengthCoins.coerceIn(0, 99),
                sleepEnergyRestore = sleepEnergyRestore.coerceIn(1, TraineeRewards.ENERGY_MAX),
                sleepFans = sleepFans.coerceIn(0, 500),
                sleepCoins = sleepCoins.coerceIn(0, 99),
                meditationEnergyRestore = meditationEnergyRestore.coerceIn(0, 50),
                meditationFans = meditationFans.coerceIn(0, 500),
                meditationCoins = meditationCoins.coerceIn(0, 99)
            )
        )
    }

    fun observeRewards(): Flow<List<RewardEntity>> = traineeDao.observeRewards()

    fun observeRedemptions(limit: Int = 10): Flow<List<RewardRedemptionEntity>> =
        traineeDao.observeRedemptions(limit)

    suspend fun seedDefaultRewardsIfEmpty() {
        if (traineeDao.rewardCount() > 0) return
        listOf(
            "安心读一会儿喜欢的书" to 20,
            "一杯喜欢的饮品" to 30,
            "周末看一部电影" to 40
        ).forEachIndexed { index, (title, cost) ->
            traineeDao.insertReward(RewardEntity(title = title, cost = cost, sortOrder = index))
        }
    }

    suspend fun addReward(title: String, cost: Int): Long {
        val cleaned = title.trim().ifBlank { "小奖励" }
        val price = cost.coerceIn(1, 999)
        val order = traineeDao.getRewards().size
        return traineeDao.insertReward(RewardEntity(title = cleaned, cost = price, sortOrder = order))
    }

    suspend fun updateReward(id: Long, title: String, cost: Int) {
        val current = traineeDao.getReward(id) ?: return
        traineeDao.updateReward(
            current.copy(
                title = title.trim().ifBlank { current.title },
                cost = cost.coerceIn(1, 999)
            )
        )
    }

    suspend fun deleteReward(id: Long) {
        traineeDao.deleteReward(id)
    }

    /** Persist manual welfare order (custom sort). */
    suspend fun reorderRewards(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            val current = traineeDao.getReward(id) ?: return@forEachIndexed
            if (current.sortOrder != index) {
                traineeDao.updateReward(current.copy(sortOrder = index))
            }
        }
    }

    /**
     * @return null on success, otherwise an error message.
     */
    suspend fun redeemReward(id: Long): String? {
        val reward = traineeDao.getReward(id) ?: return "奖励不存在"
        val profile = getProfile()
        if (profile.coins < reward.cost) return "星光币不够，再攒一攒"
        traineeDao.upsertProfile(profile.copy(coins = profile.coins - reward.cost))
        traineeDao.insertRedemption(
            RewardRedemptionEntity(
                rewardId = reward.id,
                title = reward.title,
                cost = reward.cost,
                redeemedAt = System.currentTimeMillis()
            )
        )
        return null
    }

    suspend fun updateCoinsPerYuan(coinsPerYuan: Int) {
        val profile = getProfile()
        traineeDao.upsertProfile(
            profile.copy(coinsPerYuan = coinsPerYuan.coerceIn(1, 1000))
        )
    }

    /**
     * Spend star coins as yuan against a fund label (服装 / 旅游…).
     * cost = yuan × coinsPerYuan.
     */
    suspend fun redeemFundPurchase(
        fundLabel: String,
        itemName: String,
        yuan: Int
    ): String? {
        val fund = fundLabel.trim().ifBlank { return "选一个基金用途" }
        val item = itemName.trim().ifBlank { return "写一下买了什么" }
        val amount = yuan.coerceAtLeast(0)
        if (amount <= 0) return "金额要大于 0"
        val profile = getProfile()
        val rate = profile.coinsPerYuan.coerceIn(1, 1000)
        val cost = amount * rate
        if (profile.coins < cost) return "星光币不够（需要 $cost，当前 ${profile.coins}）"
        val title = "$fund · $item · ¥$amount"
        traineeDao.upsertProfile(profile.copy(coins = profile.coins - cost))
        traineeDao.insertRedemption(
            RewardRedemptionEntity(
                rewardId = 0,
                title = title,
                cost = cost,
                redeemedAt = System.currentTimeMillis()
            )
        )
        return null
    }

    private suspend fun addPowerItemIfRoom(
        title: String,
        track: PracticeTrack?,
        markDone: Boolean,
        rewardFans: Int = 0,
        rewardCoins: Int = 0
    ) {
        val day = todayKey()
        val current = traineeDao.getPowerList(day)
        val existing = current.firstOrNull { it.track == track && it.title == title }
        if (existing != null) {
            if (markDone && existing.status != PowerItemStatus.DONE) {
                // Rewards already applied by the practice/session; only record for clawback.
                traineeDao.updatePowerItem(
                    existing.copy(
                        status = PowerItemStatus.DONE,
                        rewardFans = rewardFans,
                        rewardCoins = rewardCoins
                    )
                )
            }
            return
        }
        if (current.size >= 5) return
        traineeDao.insertPowerItem(
            PowerListItemEntity(
                dayKey = day,
                title = title,
                track = track,
                status = if (markDone) PowerItemStatus.DONE else PowerItemStatus.PENDING,
                sortOrder = current.size,
                rewardFans = if (markDone) rewardFans else 0,
                rewardCoins = if (markDone) rewardCoins else 0
            )
        )
    }

    private suspend fun award(fans: Int, coins: Int, energyDelta: Int) {
        val profile = getProfile()
        val energy = (profile.energy + energyDelta).coerceIn(0, TraineeRewards.ENERGY_MAX)
        traineeDao.upsertProfile(
            profile.copy(
                fans = (profile.fans + fans).coerceAtLeast(0),
                coins = (profile.coins + coins).coerceAtLeast(0),
                energy = energy
            )
        )
    }

    private suspend fun ensureEnergyRefreshed(profile: TraineeProfileEntity): TraineeProfileEntity {
        val today = todayKey()
        if (profile.energyDate == today) return profile
        val refreshed = profile.copy(energy = TraineeRewards.ENERGY_MAX, energyDate = today)
        traineeDao.upsertProfile(refreshed)
        return refreshed
    }

    companion object {
        private val timedLightTracks = setOf(
            PracticeTrack.ALGORITHM,
            PracticeTrack.VOCAL,
            PracticeTrack.CULTIVATION,
            PracticeTrack.LIFE,
            PracticeTrack.READING,
            PracticeTrack.CALLIGRAPHY
        )

        fun canonicalTrack(track: PracticeTrack): PracticeTrack = when (track) {
            PracticeTrack.READING, PracticeTrack.CALLIGRAPHY -> PracticeTrack.CULTIVATION
            else -> track
        }

        fun trackLabel(track: PracticeTrack?): String = when (track) {
            PracticeTrack.STRENGTH -> "力量"
            PracticeTrack.ALGORITHM -> "算法"
            PracticeTrack.VOCAL -> "声乐"
            PracticeTrack.READING,
            PracticeTrack.CALLIGRAPHY,
            PracticeTrack.CULTIVATION -> "修养"
            PracticeTrack.LIFE -> "生活"
            PracticeTrack.SLEEP -> "早睡"
            PracticeTrack.MEDITATION -> "冥想"
            null -> "练习"
        }
    }
}
