package com.doruk.fitprogress.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.database.entity.WorkoutHistoryListItemDto
import com.doruk.fitprogress.core.domain.usecase.CalculateProgressionUseCase
import com.doruk.fitprogress.core.domain.usecase.CalculateProgressionReadinessUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutHistoryViewModel @Inject constructor(
    private val dao: WorkoutDao,
    private val calculateProgressionUseCase: CalculateProgressionUseCase,
    private val calculateProgressionReadinessUseCase: CalculateProgressionReadinessUseCase
) : ViewModel() {

    private val _history = MutableStateFlow<List<WorkoutHistoryListItemDto>>(emptyList())
    val history: StateFlow<List<WorkoutHistoryListItemDto>> = _history.asStateFlow()

    init {
        viewModelScope.launch {
            dao.getWorkoutHistoryList().collectLatest { sessions ->
                _history.value = sessions
            }
        }
    }
}
