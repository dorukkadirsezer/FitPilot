package com.doruk.fitprogress.core.database.dao

import androidx.room.*
import com.doruk.fitprogress.core.database.entity.FoodItemEntity
import com.doruk.fitprogress.core.database.entity.MealLogEntity
import com.doruk.fitprogress.core.database.entity.NutritionDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNutritionDay(day: NutritionDayEntity)

    @Update
    suspend fun updateNutritionDay(day: NutritionDayEntity)

    @Query("SELECT * FROM nutrition_days WHERE date = :date LIMIT 1")
    suspend fun getNutritionDayByDate(date: String): NutritionDayEntity?
    
    @Query("SELECT * FROM nutrition_days WHERE date = :date LIMIT 1")
    fun getNutritionDayByDateFlow(date: String): Flow<NutritionDayEntity?>

    @Query("SELECT * FROM nutrition_days ORDER BY date DESC LIMIT 1")
    suspend fun getLatestNutritionDay(): NutritionDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItem(foodItem: FoodItemEntity)

    @Update
    suspend fun updateFoodItem(foodItem: FoodItemEntity)

    @Delete
    suspend fun deleteFoodItem(foodItem: FoodItemEntity)

    @Query("SELECT * FROM food_items")
    fun getAllSavedFoods(): Flow<List<FoodItemEntity>>
    
    @Query("SELECT * FROM food_items WHERE id = :id LIMIT 1")
    suspend fun getFoodItemById(id: String): FoodItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealLog(mealLog: MealLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealLogs(mealLogs: List<MealLogEntity>)

    @Update
    suspend fun updateMealLog(mealLog: MealLogEntity)

    @Delete
    suspend fun deleteMealLog(mealLog: MealLogEntity)

    @Query("SELECT * FROM meal_logs WHERE nutritionDayId = :dayId ORDER BY timestamp ASC")
    fun getMealLogsForDay(dayId: String): Flow<List<MealLogEntity>>
    
    @Query("SELECT * FROM meal_logs WHERE nutritionDayId = :dayId ORDER BY timestamp ASC")
    suspend fun getMealLogsForDaySync(dayId: String): List<MealLogEntity>

    // Aggregations
    @Query("SELECT COALESCE(SUM(calories), 0) FROM meal_logs WHERE nutritionDayId = :dayId")
    fun getTotalCaloriesForDay(dayId: String): Flow<Float>

    @Query("SELECT COALESCE(SUM(protein), 0) FROM meal_logs WHERE nutritionDayId = :dayId")
    fun getTotalProteinForDay(dayId: String): Flow<Float>

    @Query("SELECT COALESCE(SUM(carbs), 0) FROM meal_logs WHERE nutritionDayId = :dayId")
    fun getTotalCarbsForDay(dayId: String): Flow<Float>

    @Query("SELECT COALESCE(SUM(fat), 0) FROM meal_logs WHERE nutritionDayId = :dayId")
    fun getTotalFatForDay(dayId: String): Flow<Float>

    @Query("SELECT * FROM nutrition_days WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getNutritionDaysBetweenDates(startDate: String, endDate: String): List<NutritionDayEntity>

    @Query("SELECT meal_logs.* FROM meal_logs INNER JOIN nutrition_days ON meal_logs.nutritionDayId = nutrition_days.id WHERE nutrition_days.date BETWEEN :startDate AND :endDate")
    suspend fun getMealLogsBetweenDates(startDate: String, endDate: String): List<MealLogEntity>
}
