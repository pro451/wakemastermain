package com.example.data.model

data class SleepSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val date: String,
    val durationMinutes: Int,
    val deepSleepMinutes: Int,
    val efficiencyScore: Int
)
