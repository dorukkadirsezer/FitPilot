package com.doruk.fitprogress.core.domain.usecase

import android.content.ClipboardManager
import android.content.Context
import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.datastore.ProfileDataStore
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncCoachPlanUseCaseTest {

    private lateinit var workoutDao: WorkoutDao
    private lateinit var profileDataStore: ProfileDataStore
    private lateinit var context: Context
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var useCase: SyncCoachPlanUseCase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        workoutDao = mockk(relaxed = true)
        profileDataStore = mockk(relaxed = true)
        context = mockk(relaxed = true)
        clipboardManager = mockk(relaxed = true)

        every { context.getSystemService(Context.CLIPBOARD_SERVICE) } returns clipboardManager

        useCase = SyncCoachPlanUseCase(workoutDao, profileDataStore, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `invoke with empty clipboard returns failure`() = runTest {
        // Arrange
        every { clipboardManager.primaryClip } returns null

        // Act
        val result = useCase()

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("empty") == true)
    }

    @Test
    fun `invoke with malformed json returns failure`() = runTest {
        // Arrange
        val mockClip = mockk<android.content.ClipData>()
        val mockItem = mockk<android.content.ClipData.Item>()
        every { clipboardManager.primaryClip } returns mockClip
        every { mockClip.getItemAt(0) } returns mockItem
        every { mockItem.text } returns "{ not valid json"

        // Act
        val result = useCase()

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Failed to parse program") == true)
    }

    @Test
    fun `invoke with missing dailyNutritionTarget returns failure`() = runTest {
        // Arrange
        val mockClip = mockk<android.content.ClipData>()
        val mockItem = mockk<android.content.ClipData.Item>()
        every { clipboardManager.primaryClip } returns mockClip
        every { mockClip.getItemAt(0) } returns mockItem

        val jsonMissingTarget = """
            {
              "programVersion": "2.1",
              "athlete": "John Doe",
              "workouts": []
            }
        """.trimIndent()

        every { mockItem.text } returns jsonMissingTarget

        // Act
        val result = useCase()

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Failed to parse program") == true)
    }
}
