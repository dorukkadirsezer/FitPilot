package com.doruk.fitprogress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.doruk.fitprogress.core.domain.usecase.RepairDatabaseMojibakeUseCase
import com.doruk.fitprogress.core.datastore.WorkoutSettingsDataStore
import com.doruk.fitprogress.core.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    workoutSettingsDataStore: WorkoutSettingsDataStore,
    private val repairDatabaseMojibakeUseCase: RepairDatabaseMojibakeUseCase
) : ViewModel() {
    
    init {
        viewModelScope.launch {
            repairDatabaseMojibakeUseCase()
        }
    }
    
    val themeMode: StateFlow<ThemeMode> = workoutSettingsDataStore.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeMode.SYSTEM_DEFAULT
    )
}
