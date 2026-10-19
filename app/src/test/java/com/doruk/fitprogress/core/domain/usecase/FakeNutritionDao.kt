package com.doruk.fitprogress.core.domain.usecase

import com.doruk.fitprogress.core.database.dao.NutritionDao
import com.doruk.fitprogress.core.database.entity.FoodItemEntity
import com.doruk.fitprogress.core.database.entity.MealLogEntity
import com.doruk.fitprogress.core.database.entity.NutritionDayEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeNutritionDao : NutritionDao {
    private val nutritionDays = mutableListOf<NutritionDayEntity>()
    private val foodItems = mutableListOf<FoodItemEntity>()
    private val mealLogs = mutableListOf<MealLogEntity>()
    private val mealLogsFlow = MutableStateFlow<List<MealLogEntity>>(emptyList())
    private val nutritionDaysFlow = MutableStateFlow<List<NutritionDayEntity>>(emptyList())
    private val foodItemsFlow = MutableStateFlow<List<FoodItemEntity>>(emptyList())

    override suspend fun insertNutritionDay(day: NutritionDayEntity) {
        val existing = nutritionDays.find { it.date == day.date }
        if (existing != null) {
            nutritionDays.remove(existing)
        }
        nutritionDays.add(day)
        nutritionDaysFlow.value = nutritionDays.toList()
    }

    override suspend fun updateNutritionDay(day: NutritionDayEntity) {
        val index = nutritionDays.indexOfFirst { it.id == day.id }
        if (index != -1) {
            nutritionDays[index] = day
            nutritionDaysFlow.value = nutritionDays.toList()
        }
    }

    override suspend fun getNutritionDayByDate(date: String): NutritionDayEntity? {
        return nutritionDays.find { it.date == date }
    }

    override fun getNutritionDayByDateFlow(date: String): Flow<NutritionDayEntity?> {
        return nutritionDaysFlow.map { list -> list.find { it.date == date } }
    }

    override suspend fun getLatestNutritionDay(): NutritionDayEntity? {
        return nutritionDays.maxByOrNull { it.date }
    }

    override suspend fun insertFoodItem(foodItem: FoodItemEntity) {
        foodItems.removeIf { it.id == foodItem.id }
        foodItems.add(foodItem)
        foodItemsFlow.value = foodItems.toList()
    }

    override suspend fun updateFoodItem(foodItem: FoodItemEntity) {
        val index = foodItems.indexOfFirst { it.id == foodItem.id }
        if (index != -1) {
            foodItems[index] = foodItem
            foodItemsFlow.value = foodItems.toList()
        }
    }

    override suspend fun deleteFoodItem(foodItem: FoodItemEntity) {
        foodItems.removeIf { it.id == foodItem.id }
        foodItemsFlow.value = foodItems.toList()
    }

    override fun getAllSavedFoods(): Flow<List<FoodItemEntity>> {
        return foodItemsFlow
    }

    override suspend fun getFoodItemById(id: String): FoodItemEntity? {
        return foodItems.find { it.id == id }
    }

    override suspend fun insertMealLog(mealLog: MealLogEntity) {
        mealLogs.removeIf { it.id == mealLog.id }
        mealLogs.add(mealLog)
        mealLogsFlow.value = mealLogs.toList()
    }

    override suspend fun insertMealLogs(mealLogsList: List<MealLogEntity>) {
        mealLogsList.forEach { log ->
            mealLogs.removeIf { it.id == log.id }
            mealLogs.add(log)
        }
        mealLogsFlow.value = mealLogs.toList()
    }

    override suspend fun updateMealLog(mealLog: MealLogEntity) {
        val index = mealLogs.indexOfFirst { it.id == mealLog.id }
        if (index != -1) {
            mealLogs[index] = mealLog
            mealLogsFlow.value = mealLogs.toList()
        }
    }

    override suspend fun deleteMealLog(mealLog: MealLogEntity) {
        mealLogs.removeIf { it.id == mealLog.id }
        mealLogsFlow.value = mealLogs.toList()
    }

    override fun getMealLogsForDay(dayId: String): Flow<List<MealLogEntity>> {
        return mealLogsFlow.map { list ->
            list.filter { it.nutritionDayId == dayId }.sortedBy { it.timestamp }
        }
    }

    override suspend fun getMealLogsForDaySync(dayId: String): List<MealLogEntity> {
        return mealLogs.filter { it.nutritionDayId == dayId }.sortedBy { it.timestamp }
    }

    override fun getTotalCaloriesForDay(dayId: String): Flow<Float> {
        return mealLogsFlow.map { list ->
            list.filter { it.nutritionDayId == dayId }.sumOf { it.calories.toDouble() }.toFloat()
        }
    }

    override fun getTotalProteinForDay(dayId: String): Flow<Float> {
        return mealLogsFlow.map { list ->
            list.filter { it.nutritionDayId == dayId }.sumOf { it.protein.toDouble() }.toFloat()
        }
    }

    override fun getTotalCarbsForDay(dayId: String): Flow<Float> {
        return mealLogsFlow.map { list ->
            list.filter { it.nutritionDayId == dayId }.sumOf { it.carbs.toDouble() }.toFloat()
        }
    }

    override fun getTotalFatForDay(dayId: String): Flow<Float> {
        return mealLogsFlow.map { list ->
            list.filter { it.nutritionDayId == dayId }.sumOf { it.fat.toDouble() }.toFloat()
        }
    }
    
    override suspend fun getNutritionDaysBetweenDates(startDate: String, endDate: String): List<NutritionDayEntity> {
        return nutritionDays.filter { it.date in startDate..endDate }
    }

    override suspend fun getMealLogsBetweenDates(startDate: String, endDate: String): List<MealLogEntity> {
        val daysInRange = nutritionDays.filter { it.date in startDate..endDate }.map { it.id }.toSet()
        return mealLogs.filter { it.nutritionDayId in daysInRange }
    }
}
