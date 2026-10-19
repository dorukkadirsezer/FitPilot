package com.doruk.fitprogress.core.domain.usecase

import com.doruk.fitprogress.core.database.entity.SetLogEntity
import javax.inject.Inject

enum class AdviceType {
    VERY_HIGH_RPE, LOW_REPS, PERFORMANCE_DROP, HIGH_RPE, GOOD_SET, NO_ADVICE
}

data class SetAdviceResult(
    val type: AdviceType,
    val text: String
)

class CalculateSetAdviceUseCase @Inject constructor() {

    operator fun invoke(
        currentSet: SetLogEntity,
        previousWorkingSet: SetLogEntity?, // The previous working set of the same exercise in this session
        targetRepsMin: Int?,
        targetRepsMax: Int?,
        targetRpe: Float?
    ): SetAdviceResult {
        // 1. Ignore cases
        if (currentSet.setType == "WARMUP" || currentSet.isSkipped || !currentSet.completed) {
            return SetAdviceResult(AdviceType.NO_ADVICE, "")
        }
        if (currentSet.actualReps == null || currentSet.rpe == null || targetRepsMin == null || targetRpe == null) {
            return SetAdviceResult(AdviceType.NO_ADVICE, "")
        }

        val actualReps = currentSet.actualReps
        val actualRpe = currentSet.rpe

        // 2. VERY_HIGH_RPE
        if (actualRpe > targetRpe + 1.5f) {
            return SetAdviceResult(
                AdviceType.VERY_HIGH_RPE,
                "Do not increase today. Consider reducing load for the next set."
            )
        }

        // 3. LOW_REPS
        if (actualReps < targetRepsMin) {
            return SetAdviceResult(
                AdviceType.LOW_REPS,
                "Reps fell below the target range. Rest longer and focus on technique."
            )
        }

        // 4. PERFORMANCE_DROP
        if (previousWorkingSet != null && previousWorkingSet.actualReps != null && previousWorkingSet.actualWeight != null && currentSet.actualWeight != null) {
            // Only compare if weight is the same
            if (previousWorkingSet.actualWeight == currentSet.actualWeight) {
                if (actualReps <= previousWorkingSet.actualReps - 2) {
                    return SetAdviceResult(
                        AdviceType.PERFORMANCE_DROP,
                        "Performance drop detected. Consider longer rest or reduce load."
                    )
                }
            }
        }

        // 5. HIGH_RPE
        if (actualReps >= targetRepsMin && actualRpe > targetRpe + 0.5f && actualRpe <= targetRpe + 1.5f) {
            return SetAdviceResult(
                AdviceType.HIGH_RPE,
                "Reps are good, but RPE is above target. Keep the same weight or reduce slightly."
            )
        }

        // 6. GOOD_SET
        if (actualReps >= targetRepsMin && actualRpe <= targetRpe + 0.5f) {
            return SetAdviceResult(
                AdviceType.GOOD_SET,
                "Good set. Repeat the same weight."
            )
        }

        return SetAdviceResult(AdviceType.NO_ADVICE, "")
    }

    fun calculateForSupersetRound(
        setsInRound: List<SetLogEntity>,
        targetRepsMinList: List<Int?>,
        targetRpeList: List<Float?>
    ): SetAdviceResult {
        var hasVeryHighRpe = false
        var hasLowReps = false
        var hasHighRpe = false

        for (index in setsInRound.indices) {
            val setLog = setsInRound[index]
            val targetRepsMin = targetRepsMinList.getOrNull(index)
            val targetRpe = targetRpeList.getOrNull(index)
            
            if (setLog.setType == "WARMUP" || setLog.isSkipped || !setLog.completed || 
                setLog.actualReps == null || setLog.rpe == null || targetRepsMin == null || targetRpe == null) {
                return SetAdviceResult(AdviceType.NO_ADVICE, "")
            }

            val actualReps = setLog.actualReps
            val actualRpe = setLog.rpe

            if (actualRpe > targetRpe + 1.5f) {
                hasVeryHighRpe = true
            } else if (actualReps < targetRepsMin) {
                hasLowReps = true
            } else if (actualRpe > targetRpe + 0.5f && actualRpe <= targetRpe + 1.5f) {
                hasHighRpe = true
            }
        }

        if (hasVeryHighRpe) {
             return SetAdviceResult(
                AdviceType.VERY_HIGH_RPE,
                "Superset Round: RPE was far above target. Consider reducing load for the next round."
            )
        }
        if (hasLowReps) {
            return SetAdviceResult(
                AdviceType.LOW_REPS,
                "Superset Round: Reps fell below the target. Rest longer before the next round."
            )
        }
        if (hasHighRpe) {
            return SetAdviceResult(
                AdviceType.HIGH_RPE,
                "Superset Round: Reps are good, but RPE is high. Do not increase weight."
            )
        }
        
        return SetAdviceResult(
            AdviceType.GOOD_SET,
            "Superset Round: Good work. Repeat the same weights."
        )
    }
}
