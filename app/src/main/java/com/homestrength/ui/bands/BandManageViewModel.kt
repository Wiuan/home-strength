package com.homestrength.ui.bands

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.homestrength.data.local.entity.BandEntity
import com.homestrength.data.repository.HomeStrengthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BandManageViewModel(
    private val repository: HomeStrengthRepository
) : ViewModel() {

    val bands: StateFlow<List<BandEntity>> = repository.observeBands()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(resistance: Int, quantity: Int, id: Long = 0L) {
        viewModelScope.launch {
            repository.saveBand(resistance = resistance, quantity = quantity, id = id)
        }
    }

    fun delete(band: BandEntity) {
        viewModelScope.launch { repository.deleteBand(band) }
    }

    companion object {
        fun factory(repository: HomeStrengthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BandManageViewModel(repository) as T
                }
            }
    }
}
