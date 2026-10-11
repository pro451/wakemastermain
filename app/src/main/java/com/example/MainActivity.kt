package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.data.health.HealthConnectManager
import com.example.data.model.Alarm
import com.example.data.model.MissionType
import com.example.ui.screens.SleepTrackerScreen
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WakeMasterTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val healthConnectManager = remember { HealthConnectManager(context) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var alarms by remember {
        mutableStateOf(
            listOf(
                Alarm(hour = 6, minute = 30, label = "Work Morning", missionType = MissionType.MATH),
                Alarm(hour = 7, minute = 15, label = "Gym & Workout", missionType = MissionType.SHAKE),
                Alarm(hour = 8, minute = 0, label = "Weekend Routine", repeatDays = setOf(6, 7), missionType = MissionType.MEMORY)
            )
        )
    }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                contentColor = TextPrimary
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Alarms") },
                    label = { Text("Alarms") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Missions") },
                    label = { Text("Missions") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Sleep") },
                    label = { Text("Sleep") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = AccentPurple,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_alarm_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Alarm")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> AlarmsTab(
                    alarms = alarms,
                    onToggle = { id, enabled ->
                        alarms = alarms.map { if (it.id == id) it.copy(isEnabled = enabled) else it }
                    },
                    onDelete = { id ->
                        alarms = alarms.filter { it.id != id }
                    }
                )
                1 -> MissionsTab()
                2 -> SleepTrackerScreen(healthConnectManager = healthConnectManager)
                3 -> SettingsTab()
            }
        }
    }

    if (showAddDialog) {
        AddAlarmDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { newAlarm ->
                alarms = alarms + newAlarm
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AlarmsTab(
    alarms: List<Alarm>,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "WakeMaster AI",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Smart missions guarantee you wake up on time",
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(AccentPurple, AccentBlue))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Streak",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Wake Streak: 5 Days", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Top 5% of early risers this week", fontSize = 12.sp, color = TextSecondary)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(alarms, key = { it.id }) { alarm ->
                AlarmItem(
                    alarm = alarm,
                    onToggle = { onToggle(alarm.id, it) },
                    onDelete = { onDelete(alarm.id) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun AlarmItem(
    alarm: Alarm,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = alarm.formattedTime,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (alarm.isEnabled) TextPrimary else TextSecondary
                )
                Text(
                    text = "${alarm.label} • ${alarm.repeatSummary}",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = AccentTeal,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = alarm.missionType.displayName,
                        fontSize = 12.sp,
                        color = AccentTeal
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = TextSecondary
                    )
                }
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AccentPurple
                    ),
                    modifier = Modifier.testTag("alarm_switch_${alarm.id}")
                )
            }
        }
    }
}

@Composable
fun MissionsTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Smart Missions", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Active challenges required to silence alarms", fontSize = 14.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 16.dp))

        MissionType.values().forEach { mission ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp)
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
                            .background(AccentPurple.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (mission) {
                                MissionType.MATH -> Icons.Default.Edit
                                MissionType.SHAKE -> Icons.Default.Notifications
                                MissionType.MEMORY -> Icons.Default.Info
                                MissionType.STANDARD -> Icons.Default.Check
                            },
                            contentDescription = null,
                            tint = AccentPurple
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(mission.displayName, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(mission.description, fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}



@Composable
fun SettingsTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Snooze Prevention", color = TextPrimary)
                    Switch(checked = true, onCheckedChange = {})
                }
                HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vibration", color = TextPrimary)
                    Switch(checked = true, onCheckedChange = {})
                }
                HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("App Version", color = TextPrimary)
                    Text("1.0 (Build 1)", color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun AddAlarmDialog(
    onDismiss: () -> Unit,
    onAdd: (Alarm) -> Unit
) {
    var hour by remember { mutableIntStateOf(7) }
    var minute by remember { mutableIntStateOf(0) }
    var label by remember { mutableStateOf("Wake Up") }
    var selectedMission by remember { mutableStateOf(MissionType.MATH) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Alarm", color = TextPrimary) },
        containerColor = SurfaceDark,
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Alarm Label") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Time: $hour:${if (minute < 10) "0$minute" else "$minute"}", color = TextPrimary)
                Slider(
                    value = hour.toFloat(),
                    onValueChange = { hour = it.toInt() },
                    valueRange = 0f..23f,
                    steps = 22
                )
                Slider(
                    value = minute.toFloat(),
                    onValueChange = { minute = it.toInt() },
                    valueRange = 0f..59f,
                    steps = 58
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(Alarm(hour = hour, minute = minute, label = label, missionType = selectedMission))
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
