package com.alpaca.app.data.repository

data class LessonResult(
    val lessonId: String,
    val lessonTitle: String,
    val xpGained: Int,
    val coinsGained: Int,
    val correctCount: Int,
    val totalCount: Int,
    val streakIncreased: Boolean,
    val newStreak: Int,
    val outOfEnergy: Boolean,
    val mistaken: List<String>
) {
    val perfect: Boolean get() = !outOfEnergy && mistaken.isEmpty() && totalCount > 0
}
