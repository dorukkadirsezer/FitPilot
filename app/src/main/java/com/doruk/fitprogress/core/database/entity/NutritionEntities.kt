package com.doruk.fitprogress.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "nutrition_days",
    indices = [Index(value = ["date"], unique = true)]
)
data class NutritionDayEntity(
    @PrimaryKey val id: String,
    val date: String, // ISO YYYY-MM-DD
    val targetCalories: Int,
    val targetProtein: Int,
    val targetCarbs: Int,
    val targetFat: Int,
    val smartFeedback: String? = null,
    val notes: String? = null
)

@Entity(tableName = "food_items")
data class FoodItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val brandName: String?,
    val barcode: String?,
    val servingSize: Float,
    val servingUnit: String,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val isVerified: Boolean = false,
    val notes: String? = null
)

@Entity(tableName = "meal_logs")
data class MealLogEntity(
    @PrimaryKey val id: String,
    val nutritionDayId: String,
    val foodItemId: String?,
    val mealType: String, // BREAKFAST, LUNCH, DINNER, SNACK, PRE_WORKOUT, POST_WORKOUT
    
    // Snapshot fields
    val foodNameSnapshot: String,
    val servingSizeSnapshot: String?, // Or serving description
    val servingMultiplier: Float,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    
    val notes: String? = null,
    val timestamp: Long,
    val estimatedByAi: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
