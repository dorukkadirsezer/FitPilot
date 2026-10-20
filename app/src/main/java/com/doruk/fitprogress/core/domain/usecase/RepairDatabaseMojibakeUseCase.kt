package com.doruk.fitprogress.core.domain.usecase

import com.doruk.fitprogress.core.database.MojibakeMigrationUtils
import com.doruk.fitprogress.core.database.dao.WorkoutDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RepairDatabaseMojibakeUseCase @Inject constructor(
    private val workoutDao: WorkoutDao
) {
    suspend operator fun invoke() {
        withContext(Dispatchers.IO) {
            MojibakeMigrationUtils.repairDatabaseMojibake(workoutDao)
        }
    }
}
