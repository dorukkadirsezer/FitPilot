package com.doruk.fitprogress.feature.nutrition.ai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doruk.fitprogress.core.ai.AiFoodCandidate
import com.doruk.fitprogress.core.ai.AiRepository
import com.doruk.fitprogress.core.database.dao.NutritionDao
import com.doruk.fitprogress.core.database.entity.MealLogEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ReviewableFoodItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val servingDescription: String,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val mealType: String = "SNACK",
    val isSelected: Boolean = true
)

data class AiFoodReviewUiState(
    val isParsing: Boolean = true,
    val error: String? = null,
    val items: List<ReviewableFoodItem> = emptyList(),
    val nutritionDayId: String = ""
)

@HiltViewModel
class AiFoodReviewViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val nutritionDao: NutritionDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiFoodReviewUiState())
    val uiState: StateFlow<AiFoodReviewUiState> = _uiState.asStateFlow()

    private val rawInput: String = checkNotNull(savedStateHandle["rawInput"])
    private val nutritionDayId: String = checkNotNull(savedStateHandle["nutritionDayId"])

    init {
        _uiState.update { it.copy(nutritionDayId = nutritionDayId) }
        parseInput(rawInput)
    }

    private fun parseInput(input: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isParsing = true, error = null) }
            val result = aiRepository.parseFoodText(input)
            
            if (result.isSuccess) {
                val candidates = result.getOrNull() ?: emptyList()
                val reviewableItems = candidates.map {
                    ReviewableFoodItem(
                        name = it.name,
                        servingDescription = it.servingDescription,
                        calories = it.calories,
                        protein = it.protein,
                        carbs = it.carbs,
                        fat = it.fat
                    )
                }
                _uiState.update { it.copy(isParsing = false, items = reviewableItems) }
            } else {
                _uiState.update { 
                    it.copy(
                        isParsing = false, 
                        error = result.exceptionOrNull()?.message ?: "Failed to parse input."
                    )
                }
            }
        }
    }

    fun toggleSelection(id: String) {
        _uiState.update { state ->
            val updatedItems = state.items.map {
                if (it.id == id) it.copy(isSelected = !it.isSelected) else it
            }
            state.copy(items = updatedItems)
        }
    }

    fun updateItem(id: String, update: (ReviewableFoodItem) -> ReviewableFoodItem) {
        _uiState.update { state ->
            val updatedItems = state.items.map {
                if (it.id == id) update(it) else it
            }
            state.copy(items = updatedItems)
        }
    }

    fun saveSelectedItems(onComplete: () -> Unit) {
        viewModelScope.launch {
            val selectedItems = _uiState.value.items.filter { it.isSelected }
            val mealLogs = selectedItems.map {
                MealLogEntity(
                    id = UUID.randomUUID().toString(),
                    nutritionDayId = nutritionDayId,
                    foodItemId = null,
                    mealType = it.mealType,
                    foodNameSnapshot = it.name,
                    servingSizeSnapshot = it.servingDescription,
                    servingMultiplier = 1f,
                    calories = it.calories,
                    protein = it.protein,
                    carbs = it.carbs,
                    fat = it.fat,
                    notes = "Parsed by AI",
                    timestamp = System.currentTimeMillis(),
                    estimatedByAi = true
                )
            }
            nutritionDao.insertMealLogs(mealLogs)
            onComplete()
        }
    }
}
