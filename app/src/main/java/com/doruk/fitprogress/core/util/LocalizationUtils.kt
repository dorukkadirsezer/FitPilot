package com.doruk.fitprogress.core.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.doruk.fitprogress.R

object LocalizationUtils {

    @Composable
    fun getEnumString(value: String): String {
        return when (value) {
            "PRE_WORKOUT" -> stringResource(R.string.enum_pre_workout)
            "POST_WORKOUT" -> stringResource(R.string.enum_post_workout)
            "START_ORIGINAL" -> stringResource(R.string.enum_start_original)
            "START_RECOMMENDED" -> stringResource(R.string.enum_start_recommended)
            "START_TIME_LIMITED" -> stringResource(R.string.enum_start_time_limited)
            "REST_DAY" -> stringResource(R.string.enum_rest_day)
            "CONTROLLED" -> stringResource(R.string.enum_controlled)
            "LIGHT" -> stringResource(R.string.enum_light)
            "REST_OR_WALK" -> stringResource(R.string.enum_rest_or_walk)
            "NORMAL" -> stringResource(R.string.enum_normal)
            "OVERRIDDEN" -> stringResource(R.string.enum_overridden)
            else -> value // Fallback
        }
    }

    @Composable
    fun getRecommendationReasonString(reasonCode: String): String {
        return when (reasonCode) {
            "REASON_SEVERE_FATIGUE" -> stringResource(R.string.reason_severe_fatigue)
            "REASON_SORENESS_MOTIVATION" -> stringResource(R.string.reason_soreness_motivation)
            "REASON_KNEE_FATIGUE_LOWER" -> stringResource(R.string.reason_knee_fatigue_lower)
            "REASON_BACK_FATIGUE_LOWER" -> stringResource(R.string.reason_back_fatigue_lower)
            "REASON_JOINT_DROP_10" -> stringResource(R.string.reason_joint_drop_10)
            "REASON_FATIGUE_DROP_10" -> stringResource(R.string.reason_fatigue_drop_10)
            "REASON_CONTROLLED_FATIGUE" -> stringResource(R.string.reason_controlled_fatigue)
            "REASON_NORMAL_GOOD_ENERGY" -> stringResource(R.string.reason_normal_good_energy)
            else -> reasonCode
        }
    }

    @Composable
    fun getReminderString(reminderCode: String): String {
        return when (reminderCode) {
            "REMINDER_STRAPS" -> stringResource(R.string.reminder_straps)
            "REMINDER_SHOULDER_CONTRACTION" -> stringResource(R.string.reminder_shoulder_contraction)
            "REMINDER_CONTROLLED_SQUAT" -> stringResource(R.string.reminder_controlled_squat)
            "REMINDER_RDL_HINGE" -> stringResource(R.string.reminder_rdl_hinge)
            "REMINDER_HACK_SQUAT" -> stringResource(R.string.reminder_hack_squat)
            "REMINDER_CONTROLLED_MODE" -> stringResource(R.string.reminder_controlled_mode)
            else -> reminderCode
        }
    }

    @Composable
    fun getCoachNoteString(coachNote: String): String {
        return when {
            coachNote == "COACH_LOCK_SHOULDER_BLADES" || coachNote.contains("Lock shoulder blades", ignoreCase = true) -> stringResource(R.string.coach_lock_shoulder_blades)
            coachNote == "COACH_STRAPS_REQUIRED" || coachNote.contains("Lifting straps must be used", ignoreCase = true) || coachNote.contains("Use lifting straps", ignoreCase = true) || coachNote.contains("Lifting straps are mandatory", ignoreCase = true) -> stringResource(R.string.coach_straps_required)
            coachNote == "COACH_CONTROLLED_DESCENT" || coachNote.contains("Controlled descent", ignoreCase = true) || coachNote.contains("Because of previous knee sensitivity", ignoreCase = true) -> stringResource(R.string.coach_controlled_descent)
            coachNote == "COACH_PUSH_HIPS_BACK" || coachNote.contains("Push hips back, do not lift with lower back", ignoreCase = true) -> stringResource(R.string.coach_push_hips_back)
            coachNote == "COACH_FOCUS_CHEST_CONTRACTION" || coachNote.contains("Focus on chest contraction at the top", ignoreCase = true) -> stringResource(R.string.coach_focus_chest_contraction)
            coachNote == "COACH_CONSTANT_TENSION" || coachNote.contains("Constant tension on shoulders", ignoreCase = true) || coachNote.contains("As you pull the rope", ignoreCase = true) -> stringResource(R.string.coach_constant_tension)
            coachNote == "COACH_FULL_STRETCH" || coachNote.contains("Full stretch and squeeze", ignoreCase = true) || coachNote.contains("Place your feet slightly forward", ignoreCase = true) -> stringResource(R.string.coach_full_stretch)
            else -> coachNote // Fallback for custom user notes
        }
    }

    @Composable
    fun getTemplateDescription(desc: String): String {
        return when {
            desc == "DESC_TPL_A" || desc.contains("Strength and basic push-pull foundation", ignoreCase = true) -> stringResource(R.string.desc_tpl_a)
            desc == "DESC_TPL_B" || desc.contains("Strength and Core", ignoreCase = true) -> stringResource(R.string.desc_tpl_b)
            desc == "DESC_TPL_C" || desc.contains("Volume and Detail", ignoreCase = true) -> stringResource(R.string.desc_tpl_c)
            desc == "DESC_TPL_D" || desc.contains("Machine-Focused Volume", ignoreCase = true) -> stringResource(R.string.desc_tpl_d)
            desc.equals("AI Coach Program", ignoreCase = true) -> stringResource(R.string.ai_coach_program_focus)
            else -> desc
        }
    }

    @Composable
    fun getWarmupInstruction(instruction: String): String {
        return when {
            instruction == "WARMUP_LIGHT_WALKING" || instruction.contains("5 minutes light walking", ignoreCase = true) -> stringResource(R.string.warmup_light_walking)
            instruction == "WARMUP_ELLIPTICAL_LIGHT" || instruction.contains("5 minutes elliptical", ignoreCase = true) -> stringResource(R.string.warmup_elliptical_light)
            instruction == "WARMUP_BIKE_LIGHT" || instruction.contains("5 minutes stationary bike", ignoreCase = true) -> stringResource(R.string.warmup_bike_light)
            instruction == "WARMUP_SHOULDER_CHEST_MOBILITY" || instruction.contains("Dynamic shoulder and chest mobility", ignoreCase = true) -> stringResource(R.string.warmup_shoulder_chest_mobility)
            instruction == "WARMUP_HIP_ANKLE_MOBILITY" || instruction.contains("Hip-opening stretches", ignoreCase = true) -> stringResource(R.string.warmup_hip_ankle_mobility)
            instruction == "WARMUP_UPPER_BODY_DYNAMIC" || instruction.contains("Shoulder rotations", ignoreCase = true) -> stringResource(R.string.warmup_upper_body_dynamic)
            instruction == "WARMUP_LOWER_BODY_DYNAMIC" || instruction.contains("Dynamic leg swings", ignoreCase = true) -> stringResource(R.string.warmup_lower_body_dynamic)
            else -> instruction
        }
    }

    @Composable
    fun getMealTypeName(mealType: String): String {
        return when (mealType) {
            "BREAKFAST" -> stringResource(R.string.meal_breakfast)
            "LUNCH" -> stringResource(R.string.meal_lunch)
            "DINNER" -> stringResource(R.string.meal_dinner)
            "SNACK" -> stringResource(R.string.meal_snack)
            "PRE_WORKOUT" -> stringResource(R.string.meal_pre_workout)
            "POST_WORKOUT" -> stringResource(R.string.meal_post_workout)
            else -> mealType
        }
    }
}
