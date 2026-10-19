package com.doruk.fitprogress.feature.nutrition

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doruk.fitprogress.core.database.dao.NutritionDao
import com.doruk.fitprogress.core.database.entity.FoodItemEntity
import com.doruk.fitprogress.core.database.entity.MealLogEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SavedFoodsState(
    val dateIso: String = "",
    val mealType: String = "BREAKFAST",
    val savedFoods: List<FoodItemEntity> = emptyList(),
    val isLogged: Boolean = false
)

@HiltViewModel
class SavedFoodsViewModel @Inject constructor(
    private val nutritionDao: NutritionDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val dateIso = savedStateHandle.get<String>("dateIso") ?: ""
    private val mealType = savedStateHandle.get<String>("mealType") ?: "BREAKFAST"

    private val _isLogged = MutableStateFlow(false)

    val uiState: StateFlow<SavedFoodsState> = combine(
        nutritionDao.getAllSavedFoods(),
        _isLogged
    ) { foods, logged ->
        SavedFoodsState(
            dateIso = dateIso,
            mealType = mealType,
            savedFoods = foods,
            isLogged = logged
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SavedFoodsState(dateIso = dateIso, mealType = mealType)
    )

    fun deleteSavedFood(food: FoodItemEntity) {
        viewModelScope.launch {
            nutritionDao.deleteFoodItem(food)
        }
    }

    fun logSavedFood(food: FoodItemEntity, multiplier: Float) {
        viewModelScope.launch {
            val day = nutritionDao.getNutritionDayByDate(dateIso) ?: return@launch

            val mealLog = MealLogEntity(
                id = UUID.randomUUID().toString(),
                nutritionDayId = day.id,
                foodItemId = food.id,
                mealType = mealType,
                foodNameSnapshot = food.name,
                servingSizeSnapshot = "${food.servingSize} ${food.servingUnit}",
                servingMultiplier = multiplier,
                calories = food.calories * multiplier,
                protein = food.protein * multiplier,
                carbs = food.carbs * multiplier,
                fat = food.fat * multiplier,
                timestamp = System.currentTimeMillis()
            )
            
            nutritionDao.insertMealLog(mealLog)
            _isLogged.value = true
        }
    }
}
