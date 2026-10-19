package com.doruk.fitprogress

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationCrashTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testNavigationCrash() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "start") {
                composable("start") {}
                composable("active_workout/{templateId}?recId={recId}&planId={planId}") {}
            }
            
            navController.navigate("active_workout/tpl_a_day?recId=")
        }
        
        composeTestRule.waitForIdle()
    }
}