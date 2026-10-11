package com.example.ui.screens

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import com.example.data.health.HealthConnectManager
import com.example.data.health.HealthConnectStatus
import com.example.data.model.SleepSession
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTrackerScreen(
    healthConnectManager: HealthConnectManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isConnected by remember { mutableStateOf(false) }
    var sdkStatus by remember { mutableStateOf(HealthConnectStatus.AVAILABLE) }
    var isSyncing by remember { mutableStateOf(false) }
    var showRecordDialog by remember { mutableStateOf(false) }

    // Sleep records
    var sleepSessions by remember {
        mutableStateOf<List<SleepSession>>(
            listOf(
                SleepSession(
                    id = "demo-1",
                    date = "Today",
                    durationMinutes = 465, // 7h 45m
                    deepSleepMinutes = 115,
                    efficiencyScore = 91
                ),
                SleepSession(
                    id = "demo-2",
                    date = "Yesterday",
                    durationMinutes = 410, // 6h 50m
                    deepSleepMinutes = 85,
                    efficiencyScore = 78
                ),
                SleepSession(
                    id = "demo-3",
                    date = "2 days ago",
                    durationMinutes = 480, // 8h
                    deepSleepMinutes = 120,
                    efficiencyScore = 95
                )
            )
        )
    }

    // Health Connect Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        coroutineScope.launch {
            val hasAll = grantedPermissions.containsAll(healthConnectManager.permissions)
            isConnected = hasAll
            if (hasAll) {
                Toast.makeText(context, "Health Connect connected!", Toast.LENGTH_SHORT).show()
                // Fetch real sessions
                val realSessions = healthConnectManager.readSleepSessions()
                if (realSessions.isNotEmpty()) {
                    sleepSessions = realSessions
                }
            } else {
                Toast.makeText(context, "Health permissions not granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Check availability and permissions on launch
    LaunchedEffect(Unit) {
        sdkStatus = healthConnectManager.checkAvailability()
        isConnected = healthConnectManager.hasPermissions()
        if (isConnected) {
            val realSessions = healthConnectManager.readSleepSessions()
            if (realSessions.isNotEmpty()) {
                sleepSessions = realSessions
            }
        }
    }

    val latestSession = sleepSessions.firstOrNull()
    val totalHours = (latestSession?.durationMinutes ?: 465) / 60
    val totalMins = (latestSession?.durationMinutes ?: 465) % 60
    val deepMins = latestSession?.deepSleepMinutes ?: 110
    val deepPercent = if ((latestSession?.durationMinutes ?: 1) > 0) {
        ((deepMins.toFloat() / (latestSession?.durationMinutes ?: 465)) * 100).toInt()
    } else 22
    val efficiencyScore = latestSession?.efficiencyScore ?: 88

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Sleep Tracking",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Health Connect integration & recovery",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            isSyncing = true
                            if (isConnected) {
                                val fetched = healthConnectManager.readSleepSessions()
                                if (fetched.isNotEmpty()) sleepSessions = fetched
                            }
                            kotlinx.coroutines.delay(600)
                            isSyncing = false
                            Toast.makeText(context, "Sleep data synced", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("sync_health_button")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AccentTeal,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = AccentTeal
                        )
                    }
                }
            }
        }

        // Health Connect Connection Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("health_connect_status_card"),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isConnected) SuccessGreen.copy(alpha = 0.5f) else SurfaceBorder
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isConnected) SuccessGreen.copy(alpha = 0.2f)
                                        else AccentAmber.copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isConnected) Icons.Default.Check else Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = if (isConnected) SuccessGreen else AccentAmber,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Health Connect",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isConnected) "Connected • Auto-Syncing"
                                    else if (sdkStatus == HealthConnectStatus.AVAILABLE) "Not connected"
                                    else "Local Mode (Health App Optional)",
                                    fontSize = 12.sp,
                                    color = if (isConnected) SuccessGreen else TextSecondary
                                )
                            }
                        }

                        // Status Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isConnected) SuccessGreen.copy(alpha = 0.15f)
                            else SurfaceBorder.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = if (isConnected) "ACTIVE" else "OFFLINE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) SuccessGreen else TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!isConnected) {
                            Button(
                                onClick = {
                                    if (sdkStatus == HealthConnectStatus.AVAILABLE) {
                                        permissionLauncher.launch(healthConnectManager.permissions)
                                    } else {
                                        healthConnectManager.openHealthConnectSettings(context)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("connect_health_app_button")
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connect Health App", fontSize = 13.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    healthConnectManager.openHealthConnectSettings(context)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Manage Permissions", fontSize = 13.sp, color = TextPrimary)
                            }
                        }

                        FilledTonalButton(
                            onClick = { showRecordDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = SurfaceBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("record_sleep_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log Sleep", fontSize = 13.sp, color = AccentTeal)
                        }
                    }
                }
            }
        }

        // Latest Sleep Score & Duration Hero
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sleep_hero_card"),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "Last Night's Sleep",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${totalHours}h ${totalMins}m",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        }

                        // Circular Score Badge
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            AccentTeal.copy(alpha = 0.25f),
                                            AccentPurple.copy(alpha = 0.25f)
                                        )
                                    )
                                )
                                .border(2.dp, AccentTeal, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$efficiencyScore",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentTeal
                                )
                                Text(
                                    text = "SCORE",
                                    fontSize = 9.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stages Breakdown Progress Bar
                    Text(
                        text = "Sleep Stages",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(0.24f)
                                .background(AccentPurple) // Deep
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(0.22f)
                                .background(AccentBlue) // REM
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(0.54f)
                                .background(AccentTeal) // Light
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StageLegendItem(color = AccentPurple, label = "Deep", value = "${deepMins}m ($deepPercent%)")
                        StageLegendItem(color = AccentBlue, label = "REM", value = "1h 40m (22%)")
                        StageLegendItem(color = AccentTeal, label = "Light", value = "4h 15m (54%)")
                    }
                }
            }
        }

        // AI Sleep & Morning Wake Insight
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(AccentTeal.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = AccentTeal
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "AI Recovery Insight",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = if (efficiencyScore >= 85)
                                "Great deep sleep ratio! Your body is fully recovered for morning cognitive missions."
                            else
                                "You reached 78% of your target sleep. Try a gentle math mission to wake up smoothly.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // History Section Header
        item {
            Text(
                text = "Recent Sleep Sessions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Sleep History Items
        items(sleepSessions, key = { it.id }) { session ->
            val sHours = session.durationMinutes / 60
            val sMins = session.durationMinutes % 60

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sleep_item_${session.id}"),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = session.date,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Deep: ${session.deepSleepMinutes}m • Efficiency ${session.efficiencyScore}%",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${sHours}h ${sMins}m",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (session.efficiencyScore >= 85) SuccessGreen.copy(alpha = 0.2f)
                            else AccentAmber.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (session.efficiencyScore >= 85) "Optimal" else "Fair",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (session.efficiencyScore >= 85) SuccessGreen else AccentAmber,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Record Sleep Dialog
    if (showRecordDialog) {
        RecordSleepDialog(
            onDismiss = { showRecordDialog = false },
            onSave = { bedtimeHour, bedtimeMinute, wakeHour, wakeMinute, notes ->
                coroutineScope.launch {
                    val now = LocalDate.now()
                    val startInstant = now.minusDays(1)
                        .atTime(bedtimeHour, bedtimeMinute)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                    val endInstant = now
                        .atTime(wakeHour, wakeMinute)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()

                    val duration = ChronoUnit.MINUTES.between(startInstant, endInstant).toInt()
                    val deep = (duration * 0.24).toInt()
                    val score = ((duration / 480.0) * 100).toInt().coerceIn(30, 98)

                    // Write to Health Connect if available
                    if (isConnected) {
                        healthConnectManager.writeSleepSession(
                            startTime = startInstant,
                            endTime = endInstant,
                            title = "Night Sleep",
                            notes = notes
                        )
                    }

                    // Add to UI list
                    val newSession = SleepSession(
                        id = "user-${System.currentTimeMillis()}",
                        date = "Last Night",
                        durationMinutes = duration,
                        deepSleepMinutes = deep,
                        efficiencyScore = score
                    )
                    sleepSessions = listOf(newSession) + sleepSessions
                    showRecordDialog = false
                    Toast.makeText(context, "Sleep recorded successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun StageLegendItem(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(label, fontSize = 11.sp, color = TextSecondary)
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
fun RecordSleepDialog(
    onDismiss: () -> Unit,
    onSave: (bedtimeHour: Int, bedtimeMinute: Int, wakeHour: Int, wakeMinute: Int, notes: String) -> Unit
) {
    var bedtimeHour by remember { mutableIntStateOf(23) }
    var bedtimeMinute by remember { mutableIntStateOf(15) }
    var wakeHour by remember { mutableIntStateOf(7) }
    var wakeMinute by remember { mutableIntStateOf(0) }
    var notes by remember { mutableStateOf("Felt well rested") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Record Sleep Session", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        containerColor = SurfaceDark,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Bedtime: ${if (bedtimeHour < 10) "0$bedtimeHour" else "$bedtimeHour"}:${if (bedtimeMinute < 10) "0$bedtimeMinute" else "$bedtimeMinute"}",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = bedtimeHour.toFloat(),
                    onValueChange = { bedtimeHour = it.toInt() },
                    valueRange = 18f..23f,
                    steps = 4
                )

                Text(
                    "Wake time: ${if (wakeHour < 10) "0$wakeHour" else "$wakeHour"}:${if (wakeMinute < 10) "0$wakeMinute" else "$wakeMinute"}",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = wakeHour.toFloat(),
                    onValueChange = { wakeHour = it.toInt() },
                    valueRange = 5f..11f,
                    steps = 5
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Quality Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(bedtimeHour, bedtimeMinute, wakeHour, wakeMinute, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
            ) {
                Text("Save to Health Connect", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
