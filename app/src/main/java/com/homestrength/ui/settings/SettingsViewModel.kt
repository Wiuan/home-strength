package com.homestrength.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.homestrength.data.backup.BackupRepository
import com.homestrength.data.local.entity.AppSettingsEntity
import com.homestrength.data.local.entity.TraineeProfileEntity
import com.homestrength.data.repository.HomeStrengthRepository
import com.homestrength.data.repository.TraineeRepository
import com.homestrength.data.room.RoomPrefs
import com.homestrength.data.room.RoomSyncRepository
import com.homestrength.data.update.AppUpdateRepository
import com.homestrength.data.update.RemoteAppVersion
import com.homestrength.data.update.UpdateCheckResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class RoomSettingsUi(
    val enabled: Boolean = false,
    val owner: String = "wyx5555",
    val repo: String = "trainee-room",
    val branch: String = "master",
    val accessToken: String = "",
    val myMemberId: String = "wyx",
    val peerMemberId: String = "friend",
    val syncing: Boolean = false
)

data class UpdateUiState(
    val localLabel: String = "",
    val checking: Boolean = false,
    val downloading: Boolean = false,
    val progress: Float = 0f,
    val available: RemoteAppVersion? = null
)

class SettingsViewModel(
    private val repository: HomeStrengthRepository,
    private val traineeRepository: TraineeRepository,
    private val backupRepository: BackupRepository,
    private val roomSyncRepository: RoomSyncRepository,
    private val appUpdateRepository: AppUpdateRepository
) : ViewModel() {

    private val roomPrefs: RoomPrefs = roomSyncRepository.prefs()

    val settings: StateFlow<AppSettingsEntity> = repository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettingsEntity())

    val profile: StateFlow<TraineeProfileEntity> = traineeRepository.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TraineeProfileEntity())

    private val _room = MutableStateFlow(readRoom())
    val room: StateFlow<RoomSettingsUi> = _room.asStateFlow()

    private val _update = MutableStateFlow(
        UpdateUiState(
            localLabel = "${appUpdateRepository.localVersionName()} (${appUpdateRepository.localVersionCode()})"
        )
    )
    val update: StateFlow<UpdateUiState> = _update.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _importFinished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val importFinished: SharedFlow<Unit> = _importFinished.asSharedFlow()

    private val _needInstallPermission = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val needInstallPermission: SharedFlow<Unit> = _needInstallPermission.asSharedFlow()

    private var pendingApk: File? = null

    private fun readRoom(): RoomSettingsUi = RoomSettingsUi(
        enabled = roomPrefs.enabled,
        owner = roomPrefs.owner,
        repo = roomPrefs.repo,
        branch = roomPrefs.branch,
        accessToken = roomPrefs.accessToken,
        myMemberId = roomPrefs.myMemberId,
        peerMemberId = roomPrefs.peerMemberId
    )

    fun updateRoomDraft(
        enabled: Boolean? = null,
        owner: String? = null,
        repo: String? = null,
        branch: String? = null,
        accessToken: String? = null,
        myMemberId: String? = null,
        peerMemberId: String? = null
    ) {
        val cur = _room.value
        val nextToken = accessToken ?: cur.accessToken
        // Pasting a token implies the user wants sync on — no separate hunt for the switch.
        val nextEnabled = when {
            enabled != null -> enabled
            accessToken != null && nextToken.isNotBlank() -> true
            else -> cur.enabled
        }
        _room.value = cur.copy(
            enabled = nextEnabled,
            owner = owner ?: cur.owner,
            repo = repo ?: cur.repo,
            branch = branch ?: cur.branch,
            accessToken = nextToken,
            myMemberId = myMemberId ?: cur.myMemberId,
            peerMemberId = peerMemberId ?: cur.peerMemberId
        )
    }

    fun setRoomEnabled(enabled: Boolean) {
        roomPrefs.enabled = enabled
        _room.value = _room.value.copy(enabled = enabled)
        viewModelScope.launch {
            _messages.emit(
                when {
                    enabled && roomPrefs.accessToken.isBlank() -> "已开启 · 还差私人令牌"
                    enabled && roomPrefs.isConfigured() -> "练习室已开启"
                    enabled -> "已开启"
                    else -> "练习室已关闭"
                }
            )
        }
    }

    fun saveRoomSettings() {
        persistRoomDraft(autoEnableIfToken = true)
        viewModelScope.launch {
            _messages.emit(
                when {
                    roomPrefs.isConfigured() -> "练习室已保存并开启"
                    roomPrefs.accessToken.isBlank() -> "已保存 · 还差私人令牌"
                    !roomPrefs.enabled -> "已保存 · 请打开「开启同步」"
                    else -> "已保存 · 请检查仓库与成员 ID"
                }
            )
        }
    }

    fun testRoomSync() {
        viewModelScope.launch {
            persistRoomDraft(autoEnableIfToken = true)
            if (!roomPrefs.isConfigured()) {
                _messages.emit(
                    when {
                        roomPrefs.accessToken.isBlank() -> "请先填写 Gitee 私人令牌"
                        !roomPrefs.enabled -> "请先打开「开启同步」"
                        else -> "练习室配置不完整"
                    }
                )
                return@launch
            }
            _room.value = _room.value.copy(syncing = true)
            roomSyncRepository.syncAndCompare()
                .onSuccess { snap ->
                    val peerName = snap.peer?.nickname ?: "（对方尚未同步）"
                    _messages.emit("同步成功 · 对方：$peerName")
                }
                .onFailure { _messages.emit(it.message ?: "同步失败") }
            _room.value = _room.value.copy(syncing = false)
        }
    }

    private fun persistRoomDraft(autoEnableIfToken: Boolean) {
        val draft = _room.value
        val token = draft.accessToken.trim()
        val enabled = if (autoEnableIfToken && token.isNotBlank()) true else draft.enabled
        roomPrefs.enabled = enabled
        roomPrefs.owner = draft.owner
        roomPrefs.repo = draft.repo
        roomPrefs.branch = draft.branch
        roomPrefs.accessToken = token
        roomPrefs.myMemberId = draft.myMemberId
        roomPrefs.peerMemberId = draft.peerMemberId
        _room.value = readRoom().copy(syncing = draft.syncing)
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            if (!appUpdateRepository.canCheck()) {
                _messages.emit("请先填写练习室 Gitee 令牌（更新也走同一仓库）")
                return@launch
            }
            _update.value = _update.value.copy(checking = true, available = null, progress = 0f)
            when (val result = appUpdateRepository.checkForUpdate()) {
                is UpdateCheckResult.UpToDate -> {
                    _update.value = _update.value.copy(
                        checking = false,
                        localLabel = "${result.localName} (${result.localCode})",
                        available = null
                    )
                    _messages.emit("已是最新 · ${result.localName}")
                }
                is UpdateCheckResult.Available -> {
                    _update.value = _update.value.copy(
                        checking = false,
                        localLabel = "${result.localName} (${result.localCode})",
                        available = result.remote
                    )
                    _messages.emit("发现新版本 ${result.remote.versionName}")
                }
                is UpdateCheckResult.Failed -> {
                    _update.value = _update.value.copy(checking = false)
                    _messages.emit(result.message)
                }
            }
        }
    }

    fun downloadAndInstallUpdate() {
        val remote = _update.value.available ?: return
        viewModelScope.launch {
            _update.value = _update.value.copy(downloading = true, progress = 0f)
            appUpdateRepository.downloadApk(remote) { p ->
                _update.value = _update.value.copy(progress = p)
            }.onSuccess { file ->
                pendingApk = file
                _update.value = _update.value.copy(downloading = false, progress = 1f)
                installPendingApk()
            }.onFailure {
                _update.value = _update.value.copy(downloading = false)
                _messages.emit(it.message ?: "下载失败")
            }
        }
    }

    fun installPendingApk() {
        val file = pendingApk ?: return
        if (!appUpdateRepository.canInstallPackages()) {
            viewModelScope.launch { _needInstallPermission.emit(Unit) }
            return
        }
        appUpdateRepository.installApk(file)
            .onSuccess { viewModelScope.launch { _messages.emit("请按系统提示安装") } }
            .onFailure {
                if (it.message == "NEED_INSTALL_PERMISSION") {
                    viewModelScope.launch { _needInstallPermission.emit(Unit) }
                } else {
                    viewModelScope.launch { _messages.emit(it.message ?: "无法安装") }
                }
            }
    }

    fun openUnknownAppSettings(context: Context) {
        context.startActivity(appUpdateRepository.intentOpenUnknownAppSettings())
    }

    fun suggestedBackupFileName(): String {
        val day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        return "trainee-backup-$day.json"
    }

    fun exportBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            backupRepository.exportToUri(context, uri)
                .onSuccess { _messages.emit("已导出备份") }
                .onFailure { _messages.emit(it.message ?: "导出失败") }
        }
    }

    fun importBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            backupRepository.importFromUri(context, uri)
                .onSuccess {
                    _messages.emit("导入成功，即将重启以加载数据")
                    _importFinished.emit(Unit)
                }
                .onFailure { _messages.emit(it.message ?: "导入失败") }
        }
    }

    fun updateWeeklyGoal(goal: Int) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.updateSettings(current.copy(weeklyGoal = goal.coerceIn(1, 7)))
        }
    }

    fun updateDefaultSets(sets: Int) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.updateSettings(current.copy(defaultSets = sets.coerceIn(1, 5)))
        }
    }

    fun updateDefaultRestSeconds(seconds: Int) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.updateSettings(current.copy(defaultRestSeconds = seconds.coerceIn(30, 300)))
        }
    }

    fun updateRewardRules(
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
        viewModelScope.launch {
            traineeRepository.updateRewardRules(
                checklistFans = checklistFans,
                checklistCoins = checklistCoins,
                lightFans = lightFans,
                lightCoins = lightCoins,
                strengthFans = strengthFans,
                strengthCoins = strengthCoins,
                sleepEnergyRestore = sleepEnergyRestore,
                sleepFans = sleepFans,
                sleepCoins = sleepCoins,
                meditationEnergyRestore = meditationEnergyRestore,
                meditationFans = meditationFans,
                meditationCoins = meditationCoins
            )
        }
    }

    companion object {
        fun factory(
            repository: HomeStrengthRepository,
            traineeRepository: TraineeRepository,
            backupRepository: BackupRepository,
            roomSyncRepository: RoomSyncRepository,
            appUpdateRepository: AppUpdateRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(
                        repository,
                        traineeRepository,
                        backupRepository,
                        roomSyncRepository,
                        appUpdateRepository
                    ) as T
                }
            }
    }
}
