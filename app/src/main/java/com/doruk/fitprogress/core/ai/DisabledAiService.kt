package com.doruk.fitprogress.core.ai

class DisabledAiService : AiService {
    override suspend fun parseFoodText(text: String, apiKey: String): Result<List<AiFoodCandidate>> {
        return Result.failure(IllegalStateException("AI is disabled."))
    }

    override suspend fun summarizeWorkout(input: AiWorkoutSummaryInput, apiKey: String): Result<String> {
        return Result.failure(IllegalStateException("AI is disabled."))
    }

    override suspend fun explainProgression(input: AiProgressionExplanationInput, apiKey: String): Result<String> {
        return Result.failure(IllegalStateException("AI is disabled."))
    }

    override suspend fun generateWeeklyReview(input: AiWeeklyReviewInput, apiKey: String): Result<String> {
        return Result.failure(IllegalStateException("AI is disabled."))
    }
}
