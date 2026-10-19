package com.doruk.fitprogress.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doruk.fitprogress.core.database.dao.AnalyticsDao
import com.doruk.fitprogress.core.database.entity.BodyMetricEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

data class BodyMetricsUiState(
    val metricsList: List<BodyMetricEntity> = emptyList(),
    val latestWeight: Float? = null,
    val latestWaist: Float? = null,
    val latestBodyFat: Float? = null,
    val weightChange: Float? = null,
    val waistChange: Float? = null
)

@HiltViewModel
class BodyMetricsViewModel @Inject constructor(
    private val analyticsDao: AnalyticsDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(BodyMetricsUiState())
    val uiState: StateFlow<BodyMetricsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            analyticsDao.getAllBodyMetrics().collect { metrics ->
                val sorted = metrics.sortedByDescending { it.dateIso }
                val latest = sorted.firstOrNull()
                val previous = if (sorted.size > 1) sorted[1] else null
                
                val weightChange = if (latest?.bodyWeight != null && previous?.bodyWeight != null) {
                    latest.bodyWeight - previous.bodyWeight
                } else null
                
                val waistChange = if (latest?.waist != null && previous?.waist != null) {
                    latest.waist - previous.waist
                } else null
                
                _uiState.value = BodyMetricsUiState(
                    metricsList = sorted,
                    latestWeight = latest?.bodyWeight,
                    latestWaist = latest?.waist,
                    latestBodyFat = latest?.bodyFatPercentage,
                    weightChange = weightChange,
                    waistChange = waistChange
                )
            }
        }
    }

    fun saveBodyMetric(
        dateIso: String,
        weight: Float?,
        waist: Float?,
        chest: Float?,
        arm: Float?,
        leg: Float?,
        bodyFat: Float?,
        notes: String?
    ) {
        viewModelScope.launch {
            val existing = analyticsDao.getBodyMetricByDate(dateIso)
            val now = System.currentTimeMillis()
            
            if (existing != null) {
                val updated = existing.copy(
                    bodyWeight = weight,
                    waist = waist,
                    chest = chest,
                    arm = arm,
                    leg = leg,
                    bodyFatPercentage = bodyFat,
                    notes = notes,
                    updatedAt = now
                )
                analyticsDao.updateBodyMetric(updated)
            } else {
                val newMetric = BodyMetricEntity(
                    id = UUID.randomUUID().toString(),
                    dateIso = dateIso,
                    bodyWeight = weight,
                    weightUnit = "kg", // hardcode kg for MVP
                    waist = waist,
                    chest = chest,
                    arm = arm,
                    leg = leg,
                    bodyFatPercentage = bodyFat,
                    notes = notes,
                    createdAt = now,
                    updatedAt = now
                )
                analyticsDao.insertBodyMetric(newMetric)
            }
        }
    }
    
    fun deleteMetric(metric: BodyMetricEntity) {
        viewModelScope.launch {
            analyticsDao.deleteBodyMetric(metric)
        }
    }
}
