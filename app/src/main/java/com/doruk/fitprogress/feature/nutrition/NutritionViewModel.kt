package com.doruk.fitprogress.feature.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doruk.fitprogress.core.database.dao.NutritionDao
import com.doruk.fitprogress.core.database.entity.MealLogEntity
import com.doruk.fitprogress.core.database.entity.NutritionDayEntity
import com.doruk.fitprogress.core.ai.AiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

data class NutritionDashboardState(
    val currentDateIso: String = "",
    val nutritionDay: NutritionDayEntity? = null,
    val mealLogs: List<MealLogEntity> = emptyList(),
    val totalCalories: Float = 0f,
    val totalProtein: Float = 0f,
    val totalCarbs: Float = 0f,
    val totalFat: Float = 0f,
    val remainingCalories: Float = 0f,
    val remainingProtein: Float = 0f,
    val remainingCarbs: Float = 0f,
    val remainingFat: Float = 0f,
    val showCopyConfirmation: Boolean = false,
    val isAiEnabled: Boolean = false,
    val showAiDialog: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val nutritionDao: NutritionDao,
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _currentDate = MutableStateFlow(LocalDate.now())
    private val _showCopyConfirmation = MutableStateFlow(false)
    private val _showAiDialog = MutableStateFlow(false)
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    val uiState: StateFlow<NutritionDashboardState> = _currentDate
        .flatMapLatest { date ->
            val dateIso = date.format(dateFormatter)
            
            // Ensure day exists
            val existingDay = nutritionDao.getNutritionDayByDate(dateIso)
            if (existingDay == null) {
                // Get latest for targets fallback
                val latest = nutritionDao.getLatestNutritionDay()
                val newDay = NutritionDayEntity(
                    id = UUID.randomUUID().toString(),
                    date = dateIso,
                    targetCalories = latest?.targetCalories ?: 2500,
                    targetProtein = latest?.targetProtein ?: 150,
                    targetCarbs = latest?.targetCarbs ?: 250,
                    targetFat = latest?.targetFat ?: 80
                )
                nutritionDao.insertNutritionDay(newDay)
            }
            
            val dayFlow = nutritionDao.getNutritionDayByDateFlow(dateIso).filterNotNull()

            val macrosFlow = combine(
                dayFlow.flatMapLatest { nutritionDao.getTotalCaloriesForDay(it.id) },
                dayFlow.flatMapLatest { nutritionDao.getTotalProteinForDay(it.id) },
                dayFlow.flatMapLatest { nutritionDao.getTotalCarbsForDay(it.id) },
                dayFlow.flatMapLatest { nutritionDao.getTotalFatForDay(it.id) }
            ) { cals, pro, carbs, fat ->
                listOf(cals, pro, carbs, fat)
            }

            combine(
                dayFlow,
                dayFlow.flatMapLatest { nutritionDao.getMealLogsForDay(it.id) },
                macrosFlow,
                _showCopyConfirmation,
                _showAiDialog,
                aiRepository.isAiEnabledFlow
            ) { args ->
                val day = args[0] as NutritionDayEntity
                @Suppress("UNCHECKED_CAST")
                val logs = args[1] as List<MealLogEntity>
                @Suppress("UNCHECKED_CAST")
                val macros = args[2] as List<Float>
                val showConfirm = args[3] as Boolean
                val showAi = args[4] as Boolean
                val isAiEnabled = args[5] as Boolean

                NutritionDashboardState(
                    currentDateIso = dateIso,
                    nutritionDay = day,
                    mealLogs = logs,
                    totalCalories = macros[0],
                    totalProtein = macros[1],
                    totalCarbs = macros[2],
                    totalFat = macros[3],
                    remainingCalories = day.targetCalories - macros[0],
                    remainingProtein = day.targetProtein - macros[1],
                    remainingCarbs = day.targetCarbs - macros[2],
                    remainingFat = day.targetFat - macros[3],
                    showCopyConfirmation = showConfirm,
                    showAiDialog = showAi,
                    isAiEnabled = isAiEnabled,
                    isLoading = false
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            NutritionDashboardState()
        )

    fun toggleAiDialog(show: Boolean) {
        _showAiDialog.value = show
    }

    fun changeDate(offsetDays: Long) {
        _currentDate.value = _currentDate.value.plusDays(offsetDays)
    }

    fun deleteMealLog(mealLog: MealLogEntity) {
        viewModelScope.launch {
            nutritionDao.deleteMealLog(mealLog)
        }
    }

    fun requestCopyYesterdayMeals() {
        viewModelScope.launch {
            val todayDate = _currentDate.value
            val todayIso = todayDate.format(dateFormatter)
            val todayDay = nutritionDao.getNutritionDayByDate(todayIso)
            
            if (todayDay != null) {
                val todayLogs = nutritionDao.getMealLogsForDaySync(todayDay.id)
                if (todayLogs.isNotEmpty()) {
                    _showCopyConfirmation.value = true
                    return@launch
                }
            }
            // If empty, proceed directly
            confirmCopyYesterdayMeals()
        }
    }

    fun dismissCopyConfirmation() {
        _showCopyConfirmation.value = false
    }

    fun confirmCopyYesterdayMeals() {
        viewModelScope.launch {
            _showCopyConfirmation.value = false
            val todayDate = _currentDate.value
            val yesterdayIso = todayDate.minusDays(1).format(dateFormatter)
            val todayIso = todayDate.format(dateFormatter)

            val yesterdayDay = nutritionDao.getNutritionDayByDate(yesterdayIso) ?: return@launch
            val todayDay = nutritionDao.getNutritionDayByDate(todayIso) ?: return@launch

            val yesterdayLogs = nutritionDao.getMealLogsForDaySync(yesterdayDay.id)
            if (yesterdayLogs.isEmpty()) return@launch

            val copiedLogs = yesterdayLogs.map { oldLog ->
                oldLog.copy(
                    id = UUID.randomUUID().toString(),
                    nutritionDayId = todayDay.id,
                    timestamp = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                )
            }
            nutritionDao.insertMealLogs(copiedLogs)
        }
    }

    fun updateTargets(calories: Int, protein: Int, carbs: Int, fat: Int) {
        viewModelScope.launch {
            val day = uiState.value.nutritionDay ?: return@launch
            val updated = day.copy(
                targetCalories = calories,
                targetProtein = protein,
                targetCarbs = carbs,
                targetFat = fat
            )
            nutritionDao.updateNutritionDay(updated)
        }
    }
}
