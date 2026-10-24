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
import com.doruk.fitprogress.core.di.GenerativeModelFactory
import com.doruk.fitprogress.core.domain.model.AiSyncResult
import com.doruk.fitprogress.core.domain.model.ProgramPlanDto
import com.doruk.fitprogress.core.domain.model.ValidatedCoachPlanDto
import com.doruk.fitprogress.core.domain.usecase.ExportUserProgressionStatsUseCase
import com.google.ai.client.generativeai.type.GoogleGenerativeAIException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
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
    @AiCoachJson private val json: Json
) : AiCoachingRepository {

    companion object {
        val SYSTEM_PROMPT = """
            You are an elite NSCA-CSCS Strength & Conditioning Head Coach and Sports Scientist for the FitPilot application.
            Your task is to analyze the user's workout progression telemetry JSON (recent completed sessions, working volume, average RPE, set completion adherence, and exercise catalog) and generate an updated, periodized hypertrophy & strength macrocycle.

            PROGRESSIVE OVERLOAD & AUTOREGULATION RULES:
            1. If an exercise has completed working sets with average RPE <= 7.5 and 100% set completion, apply progressive overload: increase `targetLoad` by 2.5% to 5.0% (or +2.5 kg for barbell compounds) OR increase `targetRepsMax` by 1-2 reps.
            2. If an exercise has average RPE >= 9.0 or skipped working sets, maintain or slightly reduce `targetLoad` (-2.5% to -5%) and optimize `restTimerSeconds` (120-180s for compounds, 60-90s for isolations) to manage fatigue.
            3. Reuse existing `id` values from `catalogExercises` / `currentProgramOverview` whenever prescribing those movements so historical exercise analytics remain linked.
            4. Design between 3 and 5 structured workout days (`workouts`) with 4 to 7 exercises per day.

            STRICT OUTPUT SCHEMA (JSON ONLY, NO MARKDOWN FENCES, NO EXTRA TEXT):
            You MUST return a single JSON object matching `ValidatedCoachPlanDto`:
            {
              "programVersion": "2.2-gemini",
              "athlete": "Athlete Name",
              "coachRationale": "Concise 1-2 sentence summary of progression adjustments.",
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
                      "targetLoad": 82.5,
                      "targetRpe": 8.0,
                      "restTimerSeconds": 150,
                      "notes": "Progressive overload applied based on recent RPE."
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

        // 2. Aggregate lightweight user progression telemetry from Room
        val telemetryJson = runCatching {
            exportUserProgressionStatsUseCase.exportAsJsonString(maxRecentSessions = 10)
        }.getOrElse { e ->
            return@withContext AiSyncResult.SchemaValidationError(
                reason = "Telemetri verisi hazırlanamadı: ${e.localizedMessage ?: "Bilinmeyen hata"}"
            )
        }

        val userPrompt = buildString {
            appendLine("Analyze the following athlete progression telemetry JSON and return the next macrocycle strictly adhering to the ValidatedCoachPlanDto JSON schema:")
            appendLine(telemetryJson)
        }

        // 3. Call Gemini SDK with responseMimeType = "application/json"
        val rawResponseText = try {
            val generativeModel = generativeModelFactory.create(
                apiKey = rawApiKey,
                systemPrompt = SYSTEM_PROMPT
            )
            val response = generativeModel.generateContent(userPrompt)
            response.text?.trim()
        } catch (e: GoogleGenerativeAIException) {
            val msg = e.localizedMessage ?: "Gemini API hatası"
            val isAuthOrQuota = msg.contains("API_KEY", ignoreCase = true) ||
                msg.contains("API key", ignoreCase = true) ||
                msg.contains("quota", ignoreCase = true) ||
                msg.contains("403") ||
                msg.contains("429")
            return@withContext AiSyncResult.NetworkError(
                userMessage = if (isAuthOrQuota) {
                    "Gemini API anahtarı geçersiz veya kota sınırına ulaşıldı. Lütfen AI Ayarlarını kontrol edin."
                } else {
                    "Gemini servis hatası: $msg"
                },
                isQuotaOrAuthError = isAuthOrQuota
            )
        } catch (e: UnknownHostException) {
            return@withContext AiSyncResult.NetworkError(
                userMessage = "İnternet bağlantısı bulunamadı. Lütfen ağ bağlantınızı kontrol edin."
            )
        } catch (e: SocketTimeoutException) {
            return@withContext AiSyncResult.NetworkError(
                userMessage = "Gemini API yanıt süresi zaman aşımına uğradı. Lütfen tekrar deneyin."
            )
        } catch (e: IOException) {
            return@withContext AiSyncResult.NetworkError(
                userMessage = "Ağ iletişim hatası: ${e.localizedMessage ?: "Bağlantı kesildi"}"
            )
        } catch (e: Exception) {
            return@withContext AiSyncResult.NetworkError(
                userMessage = "Beklenmeyen bağlantı hatası: ${e.localizedMessage ?: "Bilinmeyen hata"}"
            )
        }

        if (rawResponseText.isNullOrBlank()) {
            return@withContext AiSyncResult.SchemaValidationError(
                reason = "Gemini boş bir yanıt döndürdü."
            )
        }

        // 4. Strictly parse & validate the JSON response
        val cleanJson = stripMarkdownJsonFence(rawResponseText)
        val validatedDto = try {
            val parsed = decodeCoachPlanCompat(cleanJson)
            parsed.validate()
        } catch (e: SerializationException) {
            return@withContext AiSyncResult.SchemaValidationError(
                reason = "JSON şema ayrıştırma hatası: ${e.localizedMessage ?: "Geçersiz format"}",
                rawSnippet = cleanJson.take(180)
            )
        } catch (e: IllegalArgumentException) {
            return@withContext AiSyncResult.SchemaValidationError(
                reason = "Antrenman şeması doğrulama hatası: ${e.localizedMessage ?: "Eksik alan"}",
                rawSnippet = cleanJson.take(180)
            )
        } catch (e: Exception) {
            return@withContext AiSyncResult.SchemaValidationError(
                reason = "Şema doğrulama hatası: ${e.localizedMessage ?: "Bilinmeyen şema hatası"}",
                rawSnippet = cleanJson.take(180)
            )
        }

        // 5. Atomically persist new macrocycle into Room & update ProfileDataStore
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
                reason = "Veritabanı kayıt hatası: ${dbError.localizedMessage ?: "Kayıt başarısız"}"
            )
        }
    }

    /**
     * Supports both a root `ValidatedCoachPlanDto` JSON object and a root JSON array
     * of `ValidatedWorkoutDayDto` items in case the model outputs a direct array.
     */
    private fun decodeCoachPlanCompat(cleanJson: String): ValidatedCoachPlanDto {
        val trimmed = cleanJson.trim()
        return if (trimmed.startsWith("[")) {
            val days = json.decodeFromString<List<com.doruk.fitprogress.core.domain.model.ValidatedWorkoutDayDto>>(trimmed)
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

    private fun stripMarkdownJsonFence(raw: String): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("```json", ignoreCase = true)) {
            cleaned = cleaned.substring(7).trimStart()
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trimStart()
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.dropLast(3).trimEnd()
        }
        return cleaned.trim()
    }
}
