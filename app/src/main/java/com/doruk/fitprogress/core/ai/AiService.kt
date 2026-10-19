package com.doruk.fitprogress.core.ai

data class AiFoodCandidate(
    val name: String,
    val servingDescription: String,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float
)

data class AiWorkoutSummaryInput(
    val dayName: String,
    val totalExercises: Int,
    val completedSets: Int,
    val skippedSets: Int,
    val notes: String
)

data class AiProgressionExplanationInput(
    val exerciseName: String,
    val suggestion: String,
    val reasoning: String,
    val history: List<String>
)

data class AiWeeklyReviewInput(
    val workoutConsistency: String,
    val nutritionConsistency: String,
    val bodyTrends: String
)

interface AiService {
    suspend fun parseFoodText(text: String, apiKey: String): Result<List<AiFoodCandidate>>
    suspend fun summarizeWorkout(input: AiWorkoutSummaryInput, apiKey: String): Result<String>
    suspend fun explainProgression(input: AiProgressionExplanationInput, apiKey: String): Result<String>
    suspend fun generateWeeklyReview(input: AiWeeklyReviewInput, apiKey: String): Result<String>
}
