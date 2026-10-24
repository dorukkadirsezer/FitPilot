package com.doruk.fitprogress

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.doruk.fitprogress.feature.dashboard.DashboardScreen
import com.doruk.fitprogress.theme.GymFlowAITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Prevent black flash during language recreation
        if (savedInstanceState != null) {
            overridePendingTransition(0, 0)
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()

            GymFlowAITheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GymFlowApp()
                }
            }
        }
    }
}

@Composable
fun GymFlowApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "dashboard",
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
    ) {
        composable("dashboard") {
            DashboardScreen(
                onTemplateClick = { templateId ->
                    navController.navigate("workout_preview/$templateId")
                },
                onNavigateToCoaching = { templateId ->
                    navController.navigate("coaching/$templateId")
                },
                onNavigateToNutrition = {
                    navController.navigate("nutrition_dashboard")
                },
                onNavigateToAnalytics = {
                    navController.navigate("analytics_dashboard")
                },
                onNavigateToWorkoutHistory = {
                    navController.navigate("workout_history")
                },
                onNavigateToBodyMetrics = {
                    navController.navigate("body_metrics")
                },
                onNavigateToAiSettings = {
                    navController.navigate("ai_settings")
                },
                onNavigateToActiveWorkout = { routeParams ->
                    navController.navigate("active_workout/AI_COACH?$routeParams") {
                        popUpTo("dashboard") { inclusive = false }
                    }
                }
            )
        }

        composable("workout_preview/{templateId}") { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId") ?: return@composable
            com.doruk.fitprogress.feature.workout.WorkoutPreviewScreen(
                templateId = templateId,
                onBackClick = { navController.popBackStack() },
                onStartWorkoutClick = { navController.navigate("coaching/$templateId") }
            )
        }

        composable("coaching/{templateId}") { backStackEntry ->
            com.doruk.fitprogress.feature.coaching.CoachingScreenHost(
                onNavigateToActiveWorkout = { recId, planId ->
                    val route = "active_workout/${backStackEntry.arguments?.getString("templateId")}?recId=$recId" +
                        (if (planId != null) "&planId=$planId" else "")
                    navController.navigate(route) {
                        popUpTo("dashboard") { inclusive = false }
                    }
                },
                onNavigateToDashboard = {
                    navController.popBackStack("dashboard", false)
                }
            )
        }

        composable(
            route = "active_workout/{templateId}?recId={recId}&planId={planId}",
            arguments = listOf(
                navArgument("recId") { nullable = true; defaultValue = null; type = NavType.StringType },
                navArgument("planId") { nullable = true; defaultValue = null; type = NavType.StringType }
            )
        ) {
            com.doruk.fitprogress.feature.workout.ActiveWorkoutScreen(
                onNavigateBack = { navController.popBackStack("dashboard", false) }
            )
        }

        composable("nutrition_dashboard") {
            com.doruk.fitprogress.feature.nutrition.NutritionDashboardScreen(
                onNavigateUp = { navController.popBackStack() },
                onNavigateToAddFood = { dateIso, mealType ->
                    navController.navigate("nutrition_add_food?dateIso=$dateIso&mealType=$mealType")
                },
                onNavigateToSavedFoods = { dateIso, mealType ->
                    navController.navigate("nutrition_saved_foods?dateIso=$dateIso&mealType=$mealType")
                },
                onNavigateToAiReview = { rawInput, nutritionDayId ->
                    navController.navigate("nutrition_ai_review?rawInput=$rawInput&nutritionDayId=$nutritionDayId")
                }
            )
        }

        composable("nutrition_setup") {
            com.doruk.fitprogress.feature.nutrition.NutritionSetupScreen(
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable(
            route = "nutrition_add_food?dateIso={dateIso}&mealType={mealType}&foodItemId={foodItemId}",
            arguments = listOf(
                navArgument("dateIso") { nullable = true; defaultValue = null; type = NavType.StringType },
                navArgument("mealType") { nullable = true; defaultValue = null; type = NavType.StringType },
                navArgument("foodItemId") { nullable = true; defaultValue = null; type = NavType.StringType }
            )
        ) {
            com.doruk.fitprogress.feature.nutrition.AddFoodScreen(
                onNavigateUp = { navController.popBackStack() },
                onNavigateToSavedFoods = { date, meal ->
                    navController.navigate("nutrition_saved_foods?dateIso=$date&mealType=$meal")
                }
            )
        }

        composable(
            route = "nutrition_saved_foods?dateIso={dateIso}&mealType={mealType}",
            arguments = listOf(
                navArgument("dateIso") { nullable = true; defaultValue = null; type = NavType.StringType },
                navArgument("mealType") { nullable = true; defaultValue = null; type = NavType.StringType }
            )
        ) {
            com.doruk.fitprogress.feature.nutrition.SavedFoodsScreen(
                onNavigateUp = { navController.popBackStack() },
                onNavigateToEditFood = { foodItemId ->
                    navController.navigate("nutrition_add_food?dateIso=&mealType=&foodItemId=$foodItemId")
                }
            )
        }

        composable("analytics_dashboard") {
            com.doruk.fitprogress.feature.analytics.AnalyticsDashboardScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("body_metrics") {
            com.doruk.fitprogress.feature.analytics.BodyMetricsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("ai_settings") {
            com.doruk.fitprogress.feature.settings.AiSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "nutrition_ai_review?rawInput={rawInput}&nutritionDayId={nutritionDayId}",
            arguments = listOf(
                navArgument("rawInput") { nullable = true; defaultValue = null; type = NavType.StringType },
                navArgument("nutritionDayId") { nullable = true; defaultValue = null; type = NavType.StringType }
            )
        ) {
            com.doruk.fitprogress.feature.nutrition.ai.AiFoodReviewScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaveComplete = { navController.popBackStack() }
            )
        }

        composable("workout_history") {
            com.doruk.fitprogress.feature.history.WorkoutHistoryScreen(
                onNavigateToDetail = { sessionId ->
                    navController.navigate("workout_detail/$sessionId")
                }
            )
        }

        composable(
            route = "workout_detail/{sessionId}",
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType }
            )
        ) {
            com.doruk.fitprogress.feature.history.WorkoutDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
