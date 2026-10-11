package com.example.data.model

enum class MissionType(val displayName: String, val description: String) {
    MATH("Math Challenge", "Solve 3 quick math equations to turn off the alarm"),
    SHAKE("Shake Device", "Vigorously shake your phone 25 times"),
    MEMORY("Memory Sequence", "Memorize and repeat a pattern sequence"),
    STANDARD("Gentle Wake", "Simple dismiss button with smart affirmation")
}

data class Alarm(
    val id: String = java.util.UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    val label: String = "Morning Alarm",
    val isEnabled: Boolean = true,
    val repeatDays: Set<Int> = setOf(1, 2, 3, 4, 5),
    val missionType: MissionType = MissionType.MATH,
    val vibrate: Boolean = true,
    val soundUri: String = "default"
) {
    val formattedTime: String
        get() {
            val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            val m = if (minute < 10) "0$minute" else "$minute"
            val amPm = if (hour < 12) "AM" else "PM"
            return "$h:$m $amPm"
        }

    val repeatSummary: String
        get() = when {
            repeatDays.isEmpty() -> "Once"
            repeatDays.size == 7 -> "Every day"
            repeatDays == setOf(1, 2, 3, 4, 5) -> "Weekdays"
            repeatDays == setOf(6, 7) -> "Weekends"
            else -> {
                val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
                repeatDays.sorted().joinToString(", ") { dayNames[(it - 1) % 7] }
            }
        }
}
