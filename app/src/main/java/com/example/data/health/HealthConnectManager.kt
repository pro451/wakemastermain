package com.example.data.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.data.model.SleepSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class HealthConnectStatus {
    AVAILABLE,
    PROVIDER_UPDATE_REQUIRED,
    NOT_INSTALLED,
    NOT_SUPPORTED
}

class HealthConnectManager(private val context: Context) {

    private val healthConnectClient by lazy {
        try {
            HealthConnectClient.getOrCreate(context)
        } catch (e: Exception) {
            null
        }
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(SleepSessionRecord::class)
    )

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun checkAvailability(): HealthConnectStatus {
        return try {
            val status = HealthConnectClient.getSdkStatus(context)
            when (status) {
                HealthConnectClient.SDK_AVAILABLE -> HealthConnectStatus.AVAILABLE
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectStatus.PROVIDER_UPDATE_REQUIRED
                else -> HealthConnectStatus.NOT_SUPPORTED
            }
        } catch (e: Exception) {
            HealthConnectStatus.NOT_SUPPORTED
        }
    }

    suspend fun hasPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            val hasAll = granted.containsAll(permissions)
            _isConnected.value = hasAll
            hasAll
        } catch (e: Exception) {
            false
        }
    }

    suspend fun readSleepSessions(
        start: Instant = Instant.now().minus(7, ChronoUnit.DAYS),
        end: Instant = Instant.now()
    ): List<SleepSession> {
        val client = healthConnectClient ?: return emptyList()
        return try {
            val request = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            val response = client.readRecords(request)
            val dateFormatter = DateTimeFormatter.ofPattern("MMM dd").withZone(ZoneId.systemDefault())

            response.records.map { record ->
                val duration = ChronoUnit.MINUTES.between(record.startTime, record.endTime).toInt()
                
                var deepMinutes = 0
                for (stage in record.stages) {
                    if (stage.stage == SleepSessionRecord.STAGE_TYPE_DEEP) {
                        deepMinutes += ChronoUnit.MINUTES.between(stage.startTime, stage.endTime).toInt()
                    }
                }
                if (deepMinutes == 0 && duration > 0) {
                    deepMinutes = (duration * 0.22).toInt() // estimated deep sleep
                }

                val score = calculateSleepScore(duration, deepMinutes)

                SleepSession(
                    id = record.metadata.id,
                    date = dateFormatter.format(record.startTime),
                    durationMinutes = duration,
                    deepSleepMinutes = deepMinutes,
                    efficiencyScore = score
                )
            }.sortedByDescending { it.date }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun writeSleepSession(
        startTime: Instant,
        endTime: Instant,
        title: String = "Night Sleep",
        notes: String = "Recorded by WakeMaster AI"
    ): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            val zoneOffset = ZoneId.systemDefault().rules.getOffset(startTime)
            val durationMinutes = ChronoUnit.MINUTES.between(startTime, endTime)

            // Construct sleep stages (Light, Deep, REM)
            val lightEnd = startTime.plus((durationMinutes * 0.4).toLong(), ChronoUnit.MINUTES)
            val deepEnd = lightEnd.plus((durationMinutes * 0.25).toLong(), ChronoUnit.MINUTES)
            val remEnd = deepEnd.plus((durationMinutes * 0.25).toLong(), ChronoUnit.MINUTES)

            val stages = listOf(
                SleepSessionRecord.Stage(
                    startTime = startTime,
                    endTime = lightEnd,
                    stage = SleepSessionRecord.STAGE_TYPE_LIGHT
                ),
                SleepSessionRecord.Stage(
                    startTime = lightEnd,
                    endTime = deepEnd,
                    stage = SleepSessionRecord.STAGE_TYPE_DEEP
                ),
                SleepSessionRecord.Stage(
                    startTime = deepEnd,
                    endTime = remEnd,
                    stage = SleepSessionRecord.STAGE_TYPE_REM
                ),
                SleepSessionRecord.Stage(
                    startTime = remEnd,
                    endTime = endTime,
                    stage = SleepSessionRecord.STAGE_TYPE_LIGHT
                )
            )

            val session = SleepSessionRecord(
                startTime = startTime,
                startZoneOffset = zoneOffset,
                endTime = endTime,
                endZoneOffset = zoneOffset,
                metadata = Metadata.manualEntry(),
                title = title,
                notes = notes,
                stages = stages
            )

            client.insertRecords(listOf(session))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openHealthConnectSettings(context: Context) {
        val intent = Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to Health Connect Play Store page
            val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(playStoreIntent)
            } catch (_: Exception) {}
        }
    }

    private fun calculateSleepScore(durationMinutes: Int, deepSleepMinutes: Int): Int {
        val targetMinutes = 480.0 // 8 hours
        val durationScore = ((durationMinutes / targetMinutes) * 70).coerceIn(0.0, 70.0)
        val deepRatio = if (durationMinutes > 0) deepSleepMinutes.toDouble() / durationMinutes else 0.0
        val qualityScore = ((deepRatio / 0.20) * 30).coerceIn(0.0, 30.0)
        return (durationScore + qualityScore).toInt().coerceIn(10, 100)
    }
}
