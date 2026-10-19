package com.doruk.fitprogress.feature.nutrition

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doruk.fitprogress.core.database.dao.NutritionDao
import com.doruk.fitprogress.core.database.entity.FoodItemEntity
import com.doruk.fitprogress.core.database.entity.MealLogEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AddFoodState(
    val dateIso: String = "",
    val mealType: String = "BREAKFAST",
    
    val name: String = "",
    val servingSize: String = "",
    val servingUnit: String = "g",
    val calories: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
    
    val saveForFuture: Boolean = false,
    val isSaved: Boolean = false,
    val foodItemIdToEdit: String? = null
)

@HiltViewModel
class AddFoodViewModel @Inject constructor(
    private val nutritionDao: NutritionDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddFoodState())
    val uiState: StateFlow<AddFoodState> = _uiState.asStateFlow()

    init {
        val date = savedStateHandle.get<String>("dateIso") ?: ""
        val mealType = savedStateHandle.get<String>("mealType") ?: "BREAKFAST"
        val foodItemId = savedStateHandle.get<String>("foodItemId")
        
        _uiState.value = _uiState.value.copy(
            dateIso = date, 
            mealType = mealType,
            foodItemIdToEdit = if (foodItemId.isNullOrBlank()) null else foodItemId,
            saveForFuture = !foodItemId.isNullOrBlank() // default to true when editing
        )
        
        if (!foodItemId.isNullOrBlank()) {
            viewModelScope.launch {
                val food = nutritionDao.getFoodItemById(foodItemId)
                if (food != null) {
                    _uiState.value = _uiState.value.copy(
                        name = food.name,
                        servingSize = food.servingSize.toString(),
                        servingUnit = food.servingUnit,
                        calories = food.calories.toString(),
                        protein = food.protein.toString(),
                        carbs = food.carbs.toString(),
                        fat = food.fat.toString()
                    )
                }
            }
        }
    }

    fun onNameChange(newValue: String) = _uiState.value.let { _uiState.value = it.copy(name = newValue) }
    fun onServingSizeChange(newValue: String) = _uiState.value.let { _uiState.value = it.copy(servingSize = newValue) }
    fun onServingUnitChange(newValue: String) = _uiState.value.let { _uiState.value = it.copy(servingUnit = newValue) }
    fun onCaloriesChange(newValue: String) = _uiState.value.let { _uiState.value = it.copy(calories = newValue) }
    fun onProteinChange(newValue: String) = _uiState.value.let { _uiState.value = it.copy(protein = newValue) }
    fun onCarbsChange(newValue: String) = _uiState.value.let { _uiState.value = it.copy(carbs = newValue) }
    fun onFatChange(newValue: String) = _uiState.value.let { _uiState.value = it.copy(fat = newValue) }
    fun onSaveForFutureChange(newValue: Boolean) = _uiState.value.let { _uiState.value = it.copy(saveForFuture = newValue) }
    fun onMealTypeChange(newValue: String) = _uiState.value.let { _uiState.value = it.copy(mealType = newValue) }

    fun saveFoodAndLog() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.name.isBlank() || state.calories.isBlank()) return@launch

            val cals = state.calories.toFloatOrNull() ?: 0f
            val pro = state.protein.toFloatOrNull() ?: 0f
            val carbs = state.carbs.toFloatOrNull() ?: 0f
            val fat = state.fat.toFloatOrNull() ?: 0f
            val servingSizeFloat = state.servingSize.toFloatOrNull() ?: 100f

            var foodItemId: String? = state.foodItemIdToEdit
            
            if (state.saveForFuture || foodItemId != null) {
                if (foodItemId == null) foodItemId = UUID.randomUUID().toString()
                
                val foodItem = FoodItemEntity(
                    id = foodItemId,
                    name = state.name,
                    brandName = null,
                    barcode = null,
                    servingSize = servingSizeFloat,
                    servingUnit = state.servingUnit,
                    calories = cals,
                    protein = pro,
                    carbs = carbs,
                    fat = fat
                )
                
                if (state.foodItemIdToEdit != null) {
                    // Update existing
                    nutritionDao.updateFoodItem(foodItem)
                } else {
                    nutritionDao.insertFoodItem(foodItem)
                }
            }

            // Only log meal if a date was provided
            if (state.dateIso.isNotBlank()) {
                val day = nutritionDao.getNutritionDayByDate(state.dateIso)
                if (day != null) {
                    val mealLog = MealLogEntity(
                        id = UUID.randomUUID().toString(),
                        nutritionDayId = day.id,
                        foodItemId = foodItemId,
                        mealType = state.mealType,
                        foodNameSnapshot = state.name,
                        servingSizeSnapshot = "${state.servingSize} ${state.servingUnit}",
                        servingMultiplier = 1f,
                        calories = cals,
                        protein = pro,
                        carbs = carbs,
                        fat = fat,
                        timestamp = System.currentTimeMillis()
                    )
                    nutritionDao.insertMealLog(mealLog)
                }
            }
            
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }
}
