package com.doruk.fitprogress.core.domain.usecase

import com.doruk.fitprogress.core.database.entity.FoodItemEntity
import com.doruk.fitprogress.core.database.entity.MealLogEntity
import com.doruk.fitprogress.core.database.entity.NutritionDayEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class NutritionTrackingTest {

    private lateinit var fakeDao: FakeNutritionDao

    @Before
    fun setup() {
        fakeDao = FakeNutritionDao()
    }

    @Test
    fun testNutritionDayUniquenessAndGetOrCreate() = runTest {
        val dateIso = "2026-07-06"
        var day = fakeDao.getNutritionDayByDate(dateIso)
        if (day == null) {
            day = NutritionDayEntity(
                id = UUID.randomUUID().toString(),
                date = dateIso,
                targetCalories = 2500,
                targetProtein = 180,
                targetCarbs = 250,
                targetFat = 80
            )
            fakeDao.insertNutritionDay(day)
        }

        val existing = fakeDao.getNutritionDayByDate(dateIso)
        assertNotNull(existing)
        assertEquals(dateIso, existing?.date)
        assertEquals(2500, existing?.targetCalories)
        
        // Re-inserting the same date should update (handled by unique constraint in Room, handled by date check in Fake)
        val updatedDay = existing!!.copy(targetCalories = 2600)
        fakeDao.insertNutritionDay(updatedDay)
        
        val newExisting = fakeDao.getNutritionDayByDate(dateIso)
        assertEquals(2600, newExisting?.targetCalories)
    }

    @Test
    fun testMealLogSnapshotBehavior() = runTest {
        val dayId = UUID.randomUUID().toString()
        val foodItemId = UUID.randomUUID().toString()
        
        // 1. Create a food item
        val foodItem = FoodItemEntity(
            id = foodItemId,
            name = "Chicken Breast",
            brandName = null,
            barcode = null,
            servingSize = 100f,
            servingUnit = "g",
            calories = 165f,
            protein = 31f,
            carbs = 0f,
            fat = 3.6f,
            isVerified = true
        )
        fakeDao.insertFoodItem(foodItem)
        
        // 2. Log a meal using the food item with a multiplier
        val servingMultiplier = 1.5f
        val mealLog = MealLogEntity(
            id = UUID.randomUUID().toString(),
            nutritionDayId = dayId,
            foodItemId = foodItem.id,
            mealType = "LUNCH",
            foodNameSnapshot = foodItem.name,
            servingSizeSnapshot = "${foodItem.servingSize} ${foodItem.servingUnit}",
            servingMultiplier = servingMultiplier,
            calories = foodItem.calories * servingMultiplier,
            protein = foodItem.protein * servingMultiplier,
            carbs = foodItem.carbs * servingMultiplier,
            fat = foodItem.fat * servingMultiplier,
            timestamp = System.currentTimeMillis()
        )
        fakeDao.insertMealLog(mealLog)
        
        // 3. Edit the food item
        val updatedFood = foodItem.copy(name = "Premium Chicken", calories = 180f)
        fakeDao.updateFoodItem(updatedFood)
        
        // 4. Verify the meal log preserved the original snapshot
        val logs = fakeDao.getMealLogsForDaySync(dayId)
        assertEquals(1, logs.size)
        val log = logs[0]
        assertEquals("Chicken Breast", log.foodNameSnapshot) // Not premium chicken
        assertEquals(165f * 1.5f, log.calories) // Not 180f
        assertEquals(31f * 1.5f, log.protein)
    }

    @Test
    fun testServingMultiplierCalculations() = runTest {
        val dayId = "day_1"
        val baseCalories = 100f
        
        val log1 = MealLogEntity(
            id = "log_1", nutritionDayId = dayId, foodItemId = null, mealType = "SNACK",
            foodNameSnapshot = "Test", servingSizeSnapshot = null,
            servingMultiplier = 0.5f, calories = baseCalories * 0.5f, protein = 0f, carbs = 0f, fat = 0f, timestamp = 0
        )
        val log2 = MealLogEntity(
            id = "log_2", nutritionDayId = dayId, foodItemId = null, mealType = "SNACK",
            foodNameSnapshot = "Test", servingSizeSnapshot = null,
            servingMultiplier = 2.0f, calories = baseCalories * 2.0f, protein = 0f, carbs = 0f, fat = 0f, timestamp = 0
        )
        
        fakeDao.insertMealLog(log1)
        fakeDao.insertMealLog(log2)
        
        assertEquals(50f, log1.calories)
        assertEquals(200f, log2.calories)
        
        val totalCal = fakeDao.getTotalCaloriesForDay(dayId).first()
        assertEquals(250f, totalCal)
    }

    @Test
    fun testCopyYesterdayMealsAndDuplicatePrevention() = runTest {
        val yesterdayId = "day_yesterday"
        val todayId = "day_today"
        
        val oldLog1 = MealLogEntity(
            id = "old_1", nutritionDayId = yesterdayId, foodItemId = null, mealType = "BREAKFAST",
            foodNameSnapshot = "Eggs", servingSizeSnapshot = null,
            servingMultiplier = 1f, calories = 200f, protein = 12f, carbs = 1f, fat = 10f, timestamp = 1000
        )
        fakeDao.insertMealLog(oldLog1)
        
        // Simulating the viewModel check
        val todayLogsBefore = fakeDao.getMealLogsForDaySync(todayId)
        var showConfirmation = false
        if (todayLogsBefore.isNotEmpty()) {
            showConfirmation = true
        } else {
            val yesterdayLogs = fakeDao.getMealLogsForDaySync(yesterdayId)
            val newLogs = yesterdayLogs.map { oldLog ->
                oldLog.copy(
                    id = UUID.randomUUID().toString(),
                    nutritionDayId = todayId,
                    timestamp = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                )
            }
            fakeDao.insertMealLogs(newLogs)
        }
        
        assertFalse(showConfirmation)
        val todayLogs = fakeDao.getMealLogsForDaySync(todayId)
        assertEquals(1, todayLogs.size)
        
        // Try again, it should trigger confirmation
        val todayLogsAfter = fakeDao.getMealLogsForDaySync(todayId)
        if (todayLogsAfter.isNotEmpty()) {
            showConfirmation = true
        }
        
        assertTrue(showConfirmation)
    }

    @Test
    fun testMacroRemainingAndClampLogic() = runTest {
        val target = 2000f
        val consumedUnder = 1500f
        val consumedOver = 2500f
        
        val remainingUnder = target - consumedUnder
        val remainingOver = target - consumedOver
        
        assertEquals(500f, remainingUnder)
        assertEquals(-500f, remainingOver)
        
        val progressUnder = if (target > 0) (consumedUnder / target).coerceIn(0f, 1f) else 0f
        val progressOver = if (target > 0) (consumedOver / target).coerceIn(0f, 1f) else 0f
        
        assertEquals(0.75f, progressUnder)
        assertEquals(1.0f, progressOver) // clamped
    }

    @Test
    fun testDailyNutritionTotalsAndDeletion() = runTest {
        val dayId = "day_1"
        
        val log1 = MealLogEntity(
            id = "log_1", nutritionDayId = dayId, foodItemId = null, mealType = "BREAKFAST",
            foodNameSnapshot = "Eggs", servingSizeSnapshot = null,
            servingMultiplier = 1f, calories = 200f, protein = 12f, carbs = 1f, fat = 10f, timestamp = 1000
        )
        val log2 = MealLogEntity(
            id = "log_2", nutritionDayId = dayId, foodItemId = null, mealType = "LUNCH",
            foodNameSnapshot = "Chicken", servingSizeSnapshot = null,
            servingMultiplier = 1f, calories = 300f, protein = 30f, carbs = 0f, fat = 5f, timestamp = 2000
        )
        
        fakeDao.insertMealLog(log1)
        fakeDao.insertMealLog(log2)
        
        var totalCals = fakeDao.getTotalCaloriesForDay(dayId).first()
        var totalPro = fakeDao.getTotalProteinForDay(dayId).first()
        
        assertEquals(500f, totalCals)
        assertEquals(42f, totalPro)
        
        // Delete log 1
        fakeDao.deleteMealLog(log1)
        
        totalCals = fakeDao.getTotalCaloriesForDay(dayId).first()
        totalPro = fakeDao.getTotalProteinForDay(dayId).first()
        
        assertEquals(300f, totalCals) // Only chicken remains
        assertEquals(30f, totalPro)
    }
}
