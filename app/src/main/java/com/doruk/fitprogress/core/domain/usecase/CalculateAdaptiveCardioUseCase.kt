package com.doruk.fitprogress.core.domain.usecase

import com.doruk.fitprogress.core.database.entity.AdaptiveCardioRecommendationEntity
import com.doruk.fitprogress.core.database.entity.ReadinessCheckInEntity
import com.doruk.fitprogress.core.database.entity.SmartDayRecommendationEntity
import com.doruk.fitprogress.core.database.entity.WorkoutSessionEntity
import java.util.UUID
import javax.inject.Inject

class CalculateAdaptiveCardioUseCase @Inject constructor() {

    operator fun invoke(
        session: WorkoutSessionEntity,
        checkIn: ReadinessCheckInEntity?,
        smartRecommendation: SmartDayRecommendationEntity?,
        averageRpe: Float,
        skippedSetsCount: Int
    ): AdaptiveCardioRecommendationEntity {
        
        val mode = smartRecommendation?.selectedMode ?: smartRecommendation?.recommendedMode ?: "NORMAL"
        val dayCode = checkIn?.plannedDayCode ?: "A" // fallback to A or get from session
        
        var recommendedCardio: String? = null
        var reason: String? = null
        
        val highFatigue = averageRpe >= 8.5f || skippedSetsCount > 3 || (checkIn?.energyLevel ?: 3) <= 2
        val highKneeSensitivity = checkIn?.kneeSensitivity == "high"

        if (dayCode == "B") {
            recommendedCardio = null
            reason = "No cardio planned for lower body strength day."
        } else if (mode == "LIGHT") {
            recommendedCardio = null
            reason = "Light mode selected. Cardio is skipped to prioritize recovery."
        } else if (highFatigue) {
             recommendedCardio = "Optional 10 min very light walk"
             reason = "High fatigue detected during the workout. Shorten cardio or skip entirely."
        } else if (dayCode == "A" && highKneeSensitivity) {
             recommendedCardio = "15 min stationary bike or elliptical"
             reason = "High knee sensitivity detected. Avoiding incline walk."
        } else if (mode == "CONTROLLED") {
             recommendedCardio = if (dayCode == "C") "15 min elliptical (Optional)" else "10 min bike (Optional)"
             reason = "Controlled mode. Cardio is optional and reduced."
        } else {
             // Normal mode, no high fatigue
             if (dayCode == "A") {
                 recommendedCardio = "15 min incline walk"
                 reason = "Keep planned cardio."
             } else if (dayCode == "C") {
                 recommendedCardio = "20 min elliptical"
                 reason = "Keep planned cardio."
             } else if (dayCode == "D") {
                 recommendedCardio = "15 min stationary bike"
                 reason = "Keep planned cardio."
             }
        }

        return AdaptiveCardioRecommendationEntity(
            id = UUID.randomUUID().toString(),
            workoutSessionId = session.id,
            recommendedCardio = recommendedCardio,
            originalCardio = null,
            reason = reason,
            accepted = false
        )
    }
}
