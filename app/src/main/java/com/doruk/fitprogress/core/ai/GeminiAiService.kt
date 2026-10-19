package com.doruk.fitprogress.core.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Implementation of AiService using raw HTTP to Google Gemini REST API.
 * This is for development MVP ONLY. 
 * Production direction: Firebase AI Logic or a secure backend proxy.
 */
class GeminiAiService : AiService {

    private suspend fun callGeminiApi(prompt: String, apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=\$apiKey")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 15000 // 15 seconds
            connection.readTimeout = 15000 // 15 seconds
            connection.doOutput = true

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            connection.outputStream.use { os ->
                val input = jsonBody.toString().toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val responseJson = JSONObject(response)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        Result.success(text)
                    } else {
                        Result.failure(Exception("Failed to parse Gemini response: No text part"))
                    }
                } else {
                    Result.failure(Exception("Failed to parse Gemini response: No candidates"))
                }
            } else {
                val errorResponse = try {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                } catch (e: Exception) {
                    "Failed to read error stream"
                }
                
                if (responseCode == 400 && errorResponse.contains("API key not valid")) {
                    Result.failure(Exception("Invalid API Key. Please check your AI Settings."))
                } else {
                    Result.failure(Exception("API Error (\$responseCode): \$errorResponse"))
                }
            }
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("Network timeout. Please check your internet connection."))
        } catch (e: java.net.UnknownHostException) {
            Result.failure(Exception("No internet connection or DNS error."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun parseFoodText(text: String, apiKey: String): Result<List<AiFoodCandidate>> {
        val prompt = """
            You are a nutrition parsing assistant. Parse the following food text into a strict JSON array of objects. 
            Do not include Markdown formatting, code blocks, or any extra text. ONLY return the JSON array.
            If a value is unknown, estimate it reasonably.
            Required JSON structure:
            [
              {
                "name": "Food Name",
                "servingDescription": "e.g., 2 large, 100g, 1 cup",
                "calories": 150.0,
                "protein": 10.5,
                "carbs": 5.0,
                "fat": 8.0
              }
            ]
            
            Food text to parse:
            \$text
        """.trimIndent()

        val result = callGeminiApi(prompt, apiKey)
        return result.mapCatching { responseText ->
            // Strip markdown code fences if they exist
            var cleanedText = responseText.trim()
            if (cleanedText.startsWith("```json")) {
                cleanedText = cleanedText.removePrefix("```json")
            }
            if (cleanedText.startsWith("```")) {
                cleanedText = cleanedText.removePrefix("```")
            }
            if (cleanedText.endsWith("```")) {
                cleanedText = cleanedText.removeSuffix("```")
            }
            cleanedText = cleanedText.trim()

            val jsonArray = JSONArray(cleanedText)
            val candidates = mutableListOf<AiFoodCandidate>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                candidates.add(
                    AiFoodCandidate(
                        name = obj.getString("name"),
                        servingDescription = obj.getString("servingDescription"),
                        calories = obj.getDouble("calories").toFloat(),
                        protein = obj.getDouble("protein").toFloat(),
                        carbs = obj.getDouble("carbs").toFloat(),
                        fat = obj.getDouble("fat").toFloat()
                    )
                )
            }
            candidates
        }
    }

    override suspend fun summarizeWorkout(input: AiWorkoutSummaryInput, apiKey: String): Result<String> {
        val prompt = """
            Provide a short, encouraging post-workout summary. Use bullet points.
            Do not provide medical or injury diagnosis. Keep it concise.
            Format:
            - What went well: ...
            - Difficulty/Fatigue: ...
            - Next focus: ...
            
            Workout Data:
            Day: \${input.dayName}
            Exercises: \${input.totalExercises}
            Completed Sets: \${input.completedSets}
            Skipped Sets: \${input.skippedSets}
            Notes: \${input.notes}
        """.trimIndent()

        return callGeminiApi(prompt, apiKey)
    }

    override suspend fun explainProgression(input: AiProgressionExplanationInput, apiKey: String): Result<String> {
        val prompt = """
            Explain the following deterministic progression calculation in 1-2 friendly, conversational sentences.
            Do NOT calculate a new suggestion. Only explain this one. Do not provide medical advice.
            
            Exercise: \${input.exerciseName}
            Suggestion: \${input.suggestion}
            Reasoning: \${input.reasoning}
            History: \${input.history.joinToString("; ")}
        """.trimIndent()

        return callGeminiApi(prompt, apiKey)
    }

    override suspend fun generateWeeklyReview(input: AiWeeklyReviewInput, apiKey: String): Result<String> {
        val prompt = """
            Provide a concise weekly review based on the following local analytics. Use bullet points.
            Do not provide medical or nutrition advice. Keep wording neutral and focused on consistency.
            
            Workout Consistency: \${input.workoutConsistency}
            Nutrition Consistency: \${input.nutritionConsistency}
            Body Trends: \${input.bodyTrends}
        """.trimIndent()

        return callGeminiApi(prompt, apiKey)
    }
}
