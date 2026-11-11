package com.homestrength.ui.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.homestrength.data.room.RoomFeedItem
import com.homestrength.data.room.RoomSyncRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class RoomFeedDay(
    val day: LocalDate,
    val items: List<RoomFeedItem>
)

data class RoomFeedUiState(
    val loading: Boolean = false,
    val days: List<RoomFeedDay> = emptyList(),
    val peerName: String? = null,
    val myName: String? = null,
    val emptyHint: String = "同步后会显示双方近期做了什么"
)

class RoomFeedViewModel(
    private val roomSyncRepository: RoomSyncRepository
) : ViewModel() {

    private val zone = ZoneId.systemDefault()
    private val _ui = MutableStateFlow(RoomFeedUiState())
    val uiState: StateFlow<RoomFeedUiState> = _ui.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        applySnapshot()
        refresh(silent = true)
    }

    fun refresh(silent: Boolean = false) {
        if (!roomSyncRepository.isReady()) {
            _ui.update {
                it.copy(
                    loading = false,
                    emptyHint = "先在设置里打开练习室并填写令牌"
                )
            }
            if (!silent) {
                viewModelScope.launch { _messages.emit("练习室未配置") }
            }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(loading = true) }
            roomSyncRepository.syncAndCompare()
                .onSuccess {
                    applySnapshot()
                    if (!silent) {
                        _messages.emit(
                            if (it.peer == null) "已上传 · 对方还没同步"
                            else "已更新动态"
                        )
                    }
                }
                .onFailure {
                    applySnapshot()
                    if (!silent) _messages.emit(it.message ?: "同步失败")
                }
            _ui.update { it.copy(loading = false) }
        }
    }

    private fun applySnapshot() {
        val snap = roomSyncRepository.lastSnapshot.value
        if (snap == null) {
            _ui.update {
                it.copy(
                    days = emptyList(),
                    peerName = null,
                    myName = null
                )
            }
            return
        }
        val feed = roomSyncRepository.mergedFeed(snap)
        val days = feed
            .groupBy { Instant.ofEpochMilli(it.activity.at).atZone(zone).toLocalDate() }
            .toSortedMap(compareByDescending { it })
            .map { (day, items) -> RoomFeedDay(day, items) }
        _ui.update {
            it.copy(
                days = days,
                myName = snap.me.nickname,
                peerName = snap.peer?.nickname,
                emptyHint = when {
                    days.isNotEmpty() -> ""
                    snap.peer == null -> "还没有动态 · 对方同步后会出现在这里"
                    else -> "近两周还没有可展示的完成记录"
                }
            )
        }
    }

    companion object {
        fun factory(roomSyncRepository: RoomSyncRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RoomFeedViewModel(roomSyncRepository) as T
                }
            }
    }
}
