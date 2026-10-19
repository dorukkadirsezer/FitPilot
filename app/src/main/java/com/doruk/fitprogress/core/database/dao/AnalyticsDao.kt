package com.doruk.fitprogress.core.database.dao

import androidx.room.*
import com.doruk.fitprogress.core.database.entity.BodyMetricEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalyticsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyMetric(metric: BodyMetricEntity)

    @Update
    suspend fun updateBodyMetric(metric: BodyMetricEntity)

    @Delete
    suspend fun deleteBodyMetric(metric: BodyMetricEntity)

    @Query("SELECT * FROM body_metrics ORDER BY dateIso DESC")
    fun getAllBodyMetrics(): Flow<List<BodyMetricEntity>>
    
    @Query("SELECT * FROM body_metrics ORDER BY dateIso DESC")
    suspend fun getAllBodyMetricsList(): List<BodyMetricEntity>

    @Query("SELECT * FROM body_metrics WHERE dateIso = :dateIso LIMIT 1")
    suspend fun getBodyMetricByDate(dateIso: String): BodyMetricEntity?

    @Query("SELECT * FROM body_metrics ORDER BY dateIso DESC LIMIT 1")
    fun getLatestBodyMetricFlow(): Flow<BodyMetricEntity?>
    
    @Query("SELECT * FROM body_metrics ORDER BY dateIso DESC LIMIT 1")
    suspend fun getLatestBodyMetric(): BodyMetricEntity?

    @Query("SELECT * FROM body_metrics WHERE dateIso BETWEEN :startDate AND :endDate ORDER BY dateIso ASC")
    suspend fun getBodyMetricsBetweenDates(startDate: String, endDate: String): List<BodyMetricEntity>
}
