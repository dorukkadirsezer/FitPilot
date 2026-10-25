package com.doruk.fitprogress.core.ai

import androidx.room.withTransaction
import com.doruk.fitprogress.core.database.GymFlowDatabase
import com.doruk.fitprogress.core.database.dao.AiDao
import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.database.entity.AiInteractionLogEntity
import com.doruk.fitprogress.core.database.entity.ExerciseEntity
import com.doruk.fitprogress.core.database.entity.WorkoutTemplateEntity
import com.doruk.fitprogress.core.database.entity.WorkoutTemplateExerciseEntity
import com.doruk.fitprogress.core.datastore.AiSettingsDataStore
import com.doruk.fitprogress.core.datastore.ProfileDataStore
import com.doruk.fitprogress.core.di.AiCoachJson
import com.doruk.fitprogress.core.di.AiCoachModule
import com.doruk.fitprogress.core.di.GenerativeModelFactory
import com.doruk.fitprogress.core.domain.model.AiSyncResult
import com.doruk.fitprogress.core.domain.model.ProgramPlanDto
import com.doruk.fitprogress.core.domain.model.ValidatedCoachPlanDto
import com.doruk.fitprogress.core.domain.model.ValidatedWorkoutDayDto
import com.doruk.fitprogress.core.domain.usecase.ExportUserProgressionStatsUseCase
import com.google.ai.client.generativeai.type.GoogleGenerativeAIException
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiCoachingRepositoryImpl @Inject constructor(
    private val aiSettingsDataStore: AiSettingsDataStore,
    private val profileDataStore: ProfileDataStore,
    private val database: GymFlowDatabase,
    private val workoutDao: WorkoutDao,
    private val aiDao: AiDao,
    private val exportUserProgressionStatsUseCase: ExportUserProgressionStatsUseCase,
    private val generativeModelFactory: GenerativeModelFactory,
    private val ktorHttpClient: HttpClient,
    @AiCoachJson private val json: Json
) : AiCoachingRepository {

    companion object {
        /**
         * Builds the official Google Generative Language v1beta REST endpoint URL:
         * `https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent`
         */
        internal fun buildGenerateContentUrl(modelName: String = AiCoachModule.DEFAULT_GEMINI_MODEL): String {
            val cleanModelName = modelName.removePrefix("models/").trim()
            return "https://generativelanguage.googleapis.com/v1beta/models/$cleanModelName:generateContent"
        }

        private val MARKDOWN_FENCE_REGEX = Regex("(?i)```(?:json)?|```")

        val SYSTEM_PROMPT = """
            You are an elite NSCA-CSCS Strength & Conditioning Head Coach and Sports Scientist for the FitPilot application.
            Your task is to analyze the user's workout progression telemetry JSON (recent completed sessions, working volume, average RPE, set completion adherence, and exercise catalog) and generate an updated, periodized hypertrophy & strength macrocycle.

            COLD START / ZERO-TELEMETRY RULE (CRITICAL):
            - If `recentCompletedSessions` is empty (`[]`) or `totalCompletedWorkouts` is `0`, DO NOT output any prose, explanation, greeting, apology, or markdown text.
            - Instead, immediately generate a complete, balanced 4-day baseline hypertrophy & strength starter macrocycle (Day 1: Upper Body A, Day 2: Lower Body A, Day 3: Upper Body B, Day 4: Lower Body B) using exercises from `catalogExercises` (or standard compound/isolation movements if `catalogExercises` is empty) with sensible beginner-intermediate starting loads (`targetLoad` between 15.0 and 60.0 kg), `targetSets = 3`, `targetRepsMin = 8`, `targetRepsMax = 10`, and `targetRpe = 7.5`, strictly formatted as the required `ValidatedCoachPlanDto` JSON object.

            PROGRESSIVE OVERLOAD & AUTOREGULATION RULES:
            1. If an exercise has completed working sets with average RPE <= 7.5 and 100% set completion, apply progressive overload: increase `targetLoad` by 2.5% to 5.0% (or +2.5 kg for barbell compounds) OR increase `targetRepsMax` by 1-2 reps.
            2. If an exercise has average RPE >= 9.0 or skipped working sets, maintain or slightly reduce `targetLoad` (-2.5% to -5%) and optimize `restTimerSeconds` (120-180s for compounds, 60-90s for isolations) to manage fatigue.
            3. Reuse existing `id` values from `catalogExercises` / `currentProgramOverview` whenever prescribing those movements so historical exercise analytics remain linked.
            4. Design between 3 and 5 structured workout days (`workouts`) with 4 to 7 exercises per day.

            STRICT OUTPUT SCHEMA (RAW JSON ONLY, NO MARKDOWN FENCES, NO PROSE):
            You MUST return ONLY a raw JSON object matching `ValidatedCoachPlanDto` starting with `{` and ending with `}`:
            {
              "programVersion": "2.2-gemini",
              "athlete": "FitPilot_Athlete",
              "coachRationale": "Concise 1-2 sentence summary of progression adjustments or baseline starter plan.",
              "dailyNutritionTarget": {
                "calories": 2500,
                "proteinGrams": 165,
                "fatGrams": 75,
                "carbsGrams": 270,
                "dailyStepsTarget": 8500,
                "sleepHoursTarget": 8.0
              },
              "workouts": [
                {
                  "dayNumber": 1,
                  "workoutName": "Upper Body Strength & Hypertrophy",
                  "exercises": [
                    {
                      "id": "bench_press",
                      "name": "Barbell Bench Press",
                      "targetSets": 4,
                      "targetReps": 8,
                      "targetRepsMin": 6,
                      "targetRepsMax": 8,
                      "targetLoad": 60.0,
                      "targetRpe": 7.5,
                      "restTimerSeconds": 150,
                      "notes": "Baseline or progressive overload prescription."
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
    }

    override suspend fun syncMacrocycleWithGemini(): AiSyncResult = withContext(Dispatchers.IO) {
        // 1. H2 Consent Check: Respect user's explicit AI opt-in toggle before reading telemetry or API key
        val isAiEnabled = runCatching {
            aiSettingsDataStore.isAiEnabledFlow.firstOrNull() ?: false
        }.getOrDefault(false)

        if (!isAiEnabled) {
            return@withContext AiSyncResult.AiDisabled
        }

        // 1.b M4 Explicit Data Sharing Consent Check before transmitting workout telemetry to Gemini API
        val hasAcceptedDataSharing = runCatching {
            aiSettingsDataStore.hasAcceptedDataSharingFlow.firstOrNull() ?: false
        }.getOrDefault(false)

        if (!hasAcceptedDataSharing) {
            return@withContext AiSyncResult.DataSharingConsentRequired
        }

        // 2. Dynamically and securely resolve Gemini API Key from EncryptedSharedPreferences via AiSettingsDataStore
        val rawApiKey = runCatching {
            aiSettingsDataStore.geminiApiKeyFlow.firstOrNull()?.trim()
        }.getOrNull()

        if (rawApiKey.isNullOrBlank()) {
            return@withContext AiSyncResult.MissingApiKey
        }

        // 3. Aggregate lightweight user progression telemetry from Room
        val telemetryJson = runCatching {
            exportUserProgressionStatsUseCase.exportAsJsonString(maxRecentSessions = 10)
        }.getOrElse { e ->
            return@withContext AiSyncResult.SchemaValidationError(
                reason = "Telemetri verisi hazÄ±rlanamadÄ±: ${e.localizedMessage ?: "Bilinmeyen hata"}"
            )
        }

        val userPrompt = buildString {
            appendLine("Analyze the following athlete progression telemetry JSON and return the next macrocycle strictly adhering to the ValidatedCoachPlanDto JSON schema.")
            appendLine("CRITICAL: Output ONLY raw valid JSON starting with '{' and ending with '}'. Do NOT use ```json markdown fences and do NOT write any conversational text. If recentCompletedSessions is empty ([]), immediately generate a complete 4-day baseline starter macrocycle in the exact same ValidatedCoachPlanDto JSON schema:")
            appendLine(telemetryJson)
        }

        // 4. Call Gemini SDK first; if SDK throws Unexpected Response / SerializationException on gemini-1.5-flash envelope metadata,
        // seamlessly fall back to direct Ktor HTTPS REST call with x-goog-api-key header.
        val rawResponseText = try {
            val sdkText = runCatching {
                val generativeModel = generativeModelFactory.create(
                    apiKey = rawApiKey,
                    systemPrompt = SYSTEM_PROMPT
                )
                generativeModel.generateContent(userPrompt).text?.trim()
            }.getOrElse { sdkException ->
                val msg = sdkException.localizedMessage ?: ""
                // Only fallback for serialization errors or unexpected response formats
                val isSerializationError = sdkException is SerializationException ||
                        msg.contains("Unexpected Response", ignoreCase = true) ||
                        msg.contains("Serialization", ignoreCase = true)

                if (!isSerializationError) {
                    throw sdkException
                }

                // Fallback to Ktor REST client to bypass deprecated SDK's envelope deserialization bug ("Unexpected Response")
                callGeminiRestViaKtor(
                    apiKey = rawApiKey,
                    systemPrompt = SYSTEM_PROMPT,
                    userPrompt = userPrompt
                )
            }

            if (sdkText.isNullOrBlank()) {
                callGeminiRestViaKtor(
                    apiKey = rawApiKey,
                    systemPrompt = SYSTEM_PROMPT,
                    userPrompt = userPrompt
                )
            } else {
                sdkText
            }
        } catch (e: GeminiRestApiException) {
            return@withContext AiSyncResult.NetworkError(
                userMessage = e.userMessage,
                isQuotaOrAuthError = e.isAuthOrQuota
            )
        } catch (e: GoogleGenerativeAIException) {
            val msg = e.localizedMessage ?: "Gemini API hatasÄ±"
            val isAuthOrQuota = msg.contains("API_KEY", ignoreCase = true) ||
                msg.contains("API key", ignoreCase = true) ||
                msg.contains("quota", ignoreCase = true) ||
                msg.contains("403") ||
                msg.contains("429")
            return@withContext AiSyncResult.NetworkError(
                userMessage = if (isAuthOrQuota) {
                    "Gemini API anahtarÄ± geÃ§ersiz veya kota sÄ±nÄ±rÄ±na ulaÅŸÄ±ldÄ±. LÃ¼tfen AI AyarlarÄ±nÄ± kontrol edin."
                } else {
                    "Gemini servis hatasÄ±: $msg"
                },
                isQuotaOrAuthError = isAuthOrQuota
            )
        } catch (e: UnknownHostException) {
            return@withContext AiSyncResult.NetworkError(
                userMessage = "Ä°nternet baÄŸlantÄ±sÄ± bulunamadÄ±. LÃ¼tfen aÄŸ baÄŸlantÄ±nÄ±zÄ± kontrol edin."
            )
        } catch (e: SocketTimeoutException) {
            return@withContext AiSyncResult.NetworkError(
                userMessage = "Gemini API yanÄ±t sÃ¼resi zaman aÅŸÄ±mÄ±na uÄŸradÄ±. LÃ¼tfen tekrar deneyin."
            )
        } catch (e: IOException) {
            return@withContext AiSyncResult.NetworkError(
                userMessage = "AÄŸ iletiÅŸim hatasÄ±: ${e.localizedMessage ?: "BaÄŸlantÄ± kesildi"}"
            )
        } catch (e: Exception) {
            return@withContext AiSyncResult.NetworkError(
                userMessage = "Beklenmeyen baÄŸlantÄ± hatasÄ±: ${e.localizedMessage ?: "Bilinmeyen hata"}"
            )
        }

        if (rawResponseText.isNullOrBlank()) {
            return@withContext AiSyncResult.SchemaValidationError(
                reason = "Gemini boÅŸ bir yanÄ±t dÃ¶ndÃ¼rdÃ¼."
            )
        }

        // 5. Sanitize markdown fences & prose, then strictly parse and validate the JSON response
        val cleanJson = sanitizeAndExtractJson(rawResponseText)
        val rawFirst100 = rawResponseText.replace(Regex("\\s+"), " ").trim().take(100)

        val validatedDto = try {
            val parsed = decodeCoachPlanCompat(cleanJson)
            parsed.validate()
        } catch (e: SerializationException) {
            return@withContext AiSyncResult.SchemaValidationError(
                reason = "JSON ayrÄ±ÅŸtÄ±rma hatasÄ±: ${e.localizedMessage?.take(80) ?: "GeÃ§ersiz format"} | Ä°lk 100 krkt: [$rawFirst100]",
                rawSnippet = rawFirst100
            )
        } catch (e: IllegalArgumentException) {
            return@withContext AiSyncResult.SchemaValidationError(
                reason = "Åema doÄŸrulama hatasÄ±: ${e.localizedMessage?.take(80) ?: "Eksik alan"} | Ä°lk 100 krkt: [$rawFirst100]",
                rawSnippet = rawFirst100
            )
        } catch (e: Exception) {
            return@withContext AiSyncResult.SchemaValidationError(
                reason = "YanÄ±t doÄŸrulama hatasÄ± (${e.javaClass.simpleName}): ${e.localizedMessage?.take(80) ?: "Bilinmeyen hata"} | Ä°lk 100 krkt: [$rawFirst100]",
                rawSnippet = rawFirst100
            )
        }

        // 6. Atomically persist new macrocycle into Room & update ProfileDataStore
        return@withContext runCatching {
            val planDto = validatedDto.toProgramPlanDto()
            persistProgramPlanAtomically(planDto)

            // Optionally log interaction if saveAiLogs is enabled
            val shouldLog = aiSettingsDataStore.saveAiLogsFlow.firstOrNull() ?: true
            if (shouldLog) {
                aiDao.insertInteractionLog(
                    AiInteractionLogEntity(
                        id = UUID.randomUUID().toString(),
                        interactionType = "GEMINI_MACROCYCLE_SYNC",
                        promptSummary = "Synced ${planDto.workouts.size} workout days from progression telemetry.",
                        response = validatedDto.coachRationale ?: "Imported ${planDto.workouts.size} AI Coach workouts.",
                        createdAt = System.currentTimeMillis(),
                        savedByUser = true
                    )
                )
            }

            val totalExercises = planDto.workouts.sumOf { it.exercises.size }
            AiSyncResult.Success(
                workoutsImported = planDto.workouts.size,
                exercisesUpdated = totalExercises,
                coachRationale = validatedDto.coachRationale
            )
        }.getOrElse { dbError ->
            AiSyncResult.SchemaValidationError(
                reason = "VeritabanÄ± kayÄ±t hatasÄ±: ${dbError.localizedMessage ?: "KayÄ±t baÅŸarÄ±sÄ±z"} | Ä°lk 100 krkt: [$rawFirst100]",
                rawSnippet = rawFirst100
            )
        }
    }

    /**
     * Direct Ktor HTTPS fallback for Gemini v1beta generateContent.
     * Uses `https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent`,
     * sends the API key strictly in the `x-goog-api-key` header, enforces `responseMimeType = "application/json"`,
     * uses only the static gemini-1.5-flash model, and includes `error.message` on failure.
     */
    private suspend fun callGeminiRestViaKtor(
        apiKey: String,
        systemPrompt: String,
        userPrompt: String
    ): String {
        val requestBodyJson = buildJsonObject {
            put("systemInstruction", buildJsonObject {
                put("parts", buildJsonArray {
                    add(buildJsonObject {
                        put("text", systemPrompt)
                    })
                })
            })
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("parts", buildJsonArray {
                        add(buildJsonObject {
                            put("text", userPrompt)
                        })
                    })
                })
            })
            put("generationConfig", buildJsonObject {
                put("responseMimeType", "application/json")
                put("temperature", 0.25)
                put("topK", 32)
                put("topP", 0.95)
            })
        }.toString()

        val endpointUrl = buildGenerateContentUrl(AiCoachModule.DEFAULT_GEMINI_MODEL)
        val httpResponse = ktorHttpClient.post(endpointUrl) {
            header("x-goog-api-key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(requestBodyJson)
        }

        val responseBody = httpResponse.bodyAsText()
        if (httpResponse.status.isSuccess()) {
            return extractTextFromGenerateContentResponse(responseBody)
        }

        val lastStatusCode = httpResponse.status.value
        val errorMessage = extractApiErrorMessage(responseBody)
        val isAuthOrQuota = lastStatusCode == 401 || lastStatusCode == 403 || lastStatusCode == 429 ||
            responseBody.contains("API_KEY", ignoreCase = true) ||
            responseBody.contains("API key not valid", ignoreCase = true) ||
            responseBody.contains("quota", ignoreCase = true)

        throw GeminiRestApiException(
            userMessage = if (isAuthOrQuota) {
                "Gemini API anahtarÄ± geÃ§ersiz veya kota sÄ±nÄ±rÄ±na ulaÅŸÄ±ldÄ± (HTTP $lastStatusCode): $errorMessage"
            } else {
                "Gemini HTTP $lastStatusCode hatasÄ±: $errorMessage"
            },
            isAuthOrQuota = isAuthOrQuota
        )
    }

    private fun extractTextFromGenerateContentResponse(responseBody: String): String {
        val rootObj = json.parseToJsonElement(responseBody).jsonObject
        val candidates = rootObj["candidates"]?.jsonArray
        if (candidates.isNullOrEmpty()) {
            val snippet = responseBody.replace(Regex("\\s+"), " ").take(100)
            throw IllegalArgumentException("Model aday yanÄ±t (candidates) Ã¼retmedi. Ham yanÄ±t: $snippet")
        }

        val firstCandidate = candidates[0].jsonObject
        val parts = firstCandidate["content"]?.jsonObject?.get("parts")?.jsonArray
        if (parts.isNullOrEmpty()) {
            val snippet = responseBody.replace(Regex("\\s+"), " ").take(100)
            throw IllegalArgumentException("Model iÃ§erik parÃ§asÄ± (parts) Ã¼retmedi. Ham yanÄ±t: $snippet")
        }

        val nonThoughtText = parts
            .map { it.jsonObject }
            .firstOrNull { partObj ->
                val isThought = partObj["thought"]?.jsonPrimitive?.booleanOrNull ?: false
                !isThought && !partObj["text"]?.jsonPrimitive?.content.isNullOrBlank()
            }
            ?.get("text")?.jsonPrimitive?.content

        return nonThoughtText
            ?: parts.last().jsonObject["text"]?.jsonPrimitive?.content
            ?: responseBody
    }

    /**
     * Extracts `error.message` (and `error.status`) from the standard Google REST API error JSON payload,
     * falling back to a single-line snippet of the raw error body if JSON parsing fails.
     */
    private fun extractApiErrorMessage(responseBody: String): String {
        val parsedErrorMessage = runCatching {
            val rootObj = json.parseToJsonElement(responseBody).jsonObject
            val errorObj = rootObj["error"]?.jsonObject
            val message = errorObj?.get("message")?.jsonPrimitive?.content?.trim()
            val status = errorObj?.get("status")?.jsonPrimitive?.content?.trim()
            if (!message.isNullOrBlank()) {
                if (!status.isNullOrBlank()) "$message ($status)" else message
            } else {
                null
            }
        }.getOrNull()

        return (parsedErrorMessage ?: responseBody.replace(Regex("\\s+"), " ").trim())
            .take(180)
            .ifBlank { "Sunucu hata detayÄ± dÃ¶ndÃ¼rmedi" }
    }

    /**
     * Multi-stage Markdown & prose sanitizer.
     * Tolerates missing/malformed ```json tags and extracts only the substring
     * between the outermost `{` or `[` and the matching last `}` or `]`.
     */
    internal fun sanitizeAndExtractJson(raw: String): String {
        // Step 1: Strip any ```json or ``` fences (even if unclosed or malformed)
        val withoutFences = MARKDOWN_FENCE_REGEX.replace(raw, "").trim()

        // Step 2: Locate the first opening brace/bracket and last closing brace/bracket
        val firstBrace = withoutFences.indexOf('{')
        val firstBracket = withoutFences.indexOf('[')

        val isObjectRoot = when {
            firstBrace == -1 && firstBracket == -1 -> return withoutFences
            firstBrace != -1 && firstBracket == -1 -> true
            firstBrace == -1 && firstBracket != -1 -> false
            else -> firstBrace < firstBracket
        }

        if (isObjectRoot) {
            val lastBrace = withoutFences.lastIndexOf('}')
            if (firstBrace != -1 && lastBrace > firstBrace) {
                return withoutFences.substring(firstBrace, lastBrace + 1).trim()
            }
        } else {
            val lastBracket = withoutFences.lastIndexOf(']')
            if (firstBracket != -1 && lastBracket > firstBracket) {
                return withoutFences.substring(firstBracket, lastBracket + 1).trim()
            }
        }

        return withoutFences
    }

    /**
     * Supports both a root `ValidatedCoachPlanDto` JSON object and a root JSON array
     * of `ValidatedWorkoutDayDto` items in case the model outputs a direct array.
     */
    private fun decodeCoachPlanCompat(cleanJson: String): ValidatedCoachPlanDto {
        val trimmed = cleanJson.trim()
        return if (trimmed.startsWith("[")) {
            val days = json.decodeFromString<List<ValidatedWorkoutDayDto>>(trimmed)
            ValidatedCoachPlanDto(workouts = days)
        } else {
            json.decodeFromString<ValidatedCoachPlanDto>(trimmed)
        }
    }

    private suspend fun persistProgramPlanAtomically(plan: ProgramPlanDto) {
        // M4 Privacy: Do not overwrite local athlete name with model output; only update bounded nutrition targets
        profileDataStore.setNutritionTargets(
            calories = plan.dailyNutritionTarget.calories,
            protein = plan.dailyNutritionTarget.proteinGrams,
            fat = plan.dailyNutritionTarget.fatGrams,
            carbs = plan.dailyNutritionTarget.carbsGrams,
            steps = plan.dailyNutritionTarget.dailyStepsTarget,
            sleep = plan.dailyNutritionTarget.sleepHoursTarget
        )

        database.withTransaction {
            workoutDao.deleteAiProgramTemplates()

            val templatesToInsert = mutableListOf<WorkoutTemplateEntity>()
            val templateExercisesToInsert = mutableListOf<WorkoutTemplateExerciseEntity>()
            val newExercisesToInsert = mutableMapOf<String, ExerciseEntity>()

            val allExercises = workoutDao.getAllExercisesSync()
            val exercisesMap = allExercises.associateBy { it.id }

            for (workout in plan.workouts) {
                val templateId = UUID.randomUUID().toString()
                val template = WorkoutTemplateEntity(
                    id = templateId,
                    name = workout.workoutName,
                    dayCode = "PROGRAM_DAY_${workout.dayNumber}",
                    dayFocus = "AI Coach Program",
                    defaultCardioType = null,
                    defaultCardioMinutes = null,
                    isAiProgram = true
                )
                templatesToInsert.add(template)

                workout.exercises.forEachIndexed { index, planEx ->
                    if (!exercisesMap.containsKey(planEx.id) && !newExercisesToInsert.containsKey(planEx.id)) {
                        newExercisesToInsert[planEx.id] = ExerciseEntity(
                            id = planEx.id,
                            name = planEx.name,
                            muscleGroup = "Other",
                            equipment = "Other",
                            movementType = "Other",
                            defaultRestSeconds = planEx.restTimerSeconds ?: 90,
                            defaultUnit = "kg",
                            isCompound = false,
                            requiresStraps = false,
                            coachNote = planEx.notes,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    }

                    templateExercisesToInsert.add(
                        WorkoutTemplateExerciseEntity(
                            id = UUID.randomUUID().toString(),
                            templateId = templateId,
                            exerciseId = planEx.id,
                            orderIndex = index + 1,
                            isSuperset = false,
                            supersetGroupId = null,
                            supersetRoundCount = null,
                            targetSets = planEx.targetSets,
                            targetRepsMin = planEx.targetReps ?: (planEx.targetRepsMin ?: 10),
                            targetRepsMax = planEx.targetReps ?: (planEx.targetRepsMax ?: 10),
                            targetWeightMin = planEx.targetLoad,
                            targetWeightMax = planEx.targetLoad,
                            weightUnit = "kg",
                            restSeconds = planEx.restTimerSeconds ?: 90,
                            rpeMin = planEx.targetRpe,
                            rpeMax = planEx.targetRpe
                        )
                    )
                }
            }

            if (newExercisesToInsert.isNotEmpty()) {
                workoutDao.insertExercises(newExercisesToInsert.values.toList())
            }
            if (templatesToInsert.isNotEmpty()) {
                workoutDao.insertTemplates(templatesToInsert)
            }
            if (templateExercisesToInsert.isNotEmpty()) {
                workoutDao.insertTemplateExercises(templateExercisesToInsert)
            }
        }
    }

    private class GeminiRestApiException(
        val userMessage: String,
        val isAuthOrQuota: Boolean
    ) : Exception(userMessage)
}

