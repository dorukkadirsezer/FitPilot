package com.doruk.fitprogress.core.domain.usecase

import com.doruk.fitprogress.core.database.entity.ReadinessCheckInEntity
import javax.inject.Inject

enum class RecommendationMode {
    NORMAL, CONTROLLED, LIGHT, REST_OR_WALK
}

data class SmartRecommendationResult(
    val recommendedMode: String,
    val reason: String
)

class CalculateSmartRecommendationUseCase @Inject constructor() {

    operator fun invoke(checkIn: ReadinessCheckInEntity): SmartRecommendationResult {
        val isLowerBodyDay = checkIn.plannedDayCode == "B" || checkIn.plannedDayCode == "D"
        val isUpperBodyDay = checkIn.plannedDayCode == "A" || checkIn.plannedDayCode == "C"

        val severeFatigue = checkIn.energyLevel <= 2 && checkIn.sleepQuality <= 2 && checkIn.previousWorkoutFeltHard
        val highKneeSensitivity = checkIn.kneeSensitivity == "high"
        val highBackDiscomfort = checkIn.backDiscomfort == "high"
        val highSorenessLowMotivation = checkIn.muscleSoreness >= 4 && checkIn.motivationLevel <= 2

        // 1. REST / WALK evaluation
        if (severeFatigue) {
            return SmartRecommendationResult("REST_OR_WALK", "REASON_SEVERE_FATIGUE")
        }
        if (highSorenessLowMotivation) {
            return SmartRecommendationResult("REST_OR_WALK", "REASON_SORENESS_MOTIVATION")
        }
        if (isLowerBodyDay && highKneeSensitivity && (checkIn.energyLevel <= 3 || checkIn.muscleSoreness >= 3)) {
            return SmartRecommendationResult("REST_OR_WALK", "REASON_KNEE_FATIGUE_LOWER")
        }
        if (checkIn.plannedDayCode == "B" && highBackDiscomfort && (checkIn.energyLevel <= 3 || checkIn.muscleSoreness >= 3)) {
            return SmartRecommendationResult("REST_OR_WALK", "REASON_BACK_FATIGUE_LOWER")
        }

        // 2. LIGHT evaluation
        val isLowerBodyJointRisk = (isLowerBodyDay && highKneeSensitivity) || (checkIn.plannedDayCode == "B" && highBackDiscomfort)
        val moderateJointRisk = checkIn.kneeSensitivity == "moderate" || checkIn.backDiscomfort == "moderate"
        
        if (checkIn.energyLevel <= 2 || checkIn.muscleSoreness >= 4 || isLowerBodyJointRisk || moderateJointRisk) {
            val reason = if (isLowerBodyJointRisk || moderateJointRisk) "REASON_JOINT_DROP_10"
                         else "REASON_FATIGUE_DROP_10"
            return SmartRecommendationResult("LIGHT", reason)
        }

        // 3. CONTROLLED evaluation
        val mildSensitivity = checkIn.kneeSensitivity == "mild" || checkIn.backDiscomfort == "mild"
        val upperBodyJointRisk = isUpperBodyDay && (highKneeSensitivity || highBackDiscomfort)
        
        if (checkIn.energyLevel == 3 || checkIn.sleepQuality <= 3 || checkIn.previousWorkoutFeltHard || mildSensitivity || upperBodyJointRisk) {
            val reason = "REASON_CONTROLLED_FATIGUE"
            return SmartRecommendationResult("CONTROLLED", reason)
        }

        // 4. NORMAL evaluation
        return SmartRecommendationResult("NORMAL", "REASON_NORMAL_GOOD_ENERGY")
    }
}
