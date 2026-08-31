package com.alpaca.app.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alpaca.app.data.db.entities.LessonStatus
import com.alpaca.app.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class Badge(
    val emoji: String,
    val title: String,
    val description: String,
    val unlocked: Boolean,
    val progress: String
)

class AchievementsViewModel(container: AppContainer) : ViewModel() {

    data class UiState(val badges: List<Badge> = emptyList(), val displayName: String = "Explorer")

    private val _state = kotlinx.coroutines.flow.MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            val units = container.contentRepository.loadUnits()
            val totalLessons = units.sumOf { it.lessons.size }
            combine(
                container.progressRepository.observeProgress(),
                container.gamificationRepository.observeUser(),
                container.prefs.prefs
            ) { progress, user, prefs ->
                val completed = progress.values.count { it.status == LessonStatus.COMPLETE }
                val perfect = progress.values.any { it.bestScore >= 100 }
                UiState(
                    displayName = prefs.displayName,
                    badges = listOf(
                        Badge("🐣", "Primer Paso", "Complete your first lesson",
                            completed >= 1, "$completed/$totalLessons lessons"),
                        Badge("🔥", "En Marcha", "Keep a 3-day streak",
                            user.streakDays >= 3, "streak ${user.streakDays}"),
                        Badge("🌋", "Modo Volcán", "Keep a 7-day streak",
                            user.streakDays >= 7, "streak ${user.streakDays}"),
                        Badge("💯", "Perfeccionista", "Finish a lesson with zero mistakes",
                            perfect, if (perfect) "done" else "keep trying"),
                        Badge("💰", "Coleccionista", "Collect 100 Coins",
                            user.coins >= 100, "${user.coins}/100 coins"),
                        Badge("⭐", "Superestrella", "Reach 500 XP",
                            user.xp >= 500, "${user.xp}/500 XP"),
                        Badge("🎤", "Hablante", "Start a live voice call",
                            prefs.callsMade >= 1, "${prefs.callsMade} calls"),
                        Badge("🦙", "Líder de la Manada", "Complete every lesson on the trail",
                            completed >= totalLessons, "$completed/$totalLessons lessons")
                    )
                )
            }.collect { _state.value = it }
        }
    }
}
