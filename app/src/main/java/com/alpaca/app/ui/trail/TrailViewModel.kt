package com.alpaca.app.ui.trail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alpaca.app.data.content.CourseLanguage
import com.alpaca.app.data.db.entities.LessonStatus
import com.alpaca.app.data.db.entities.UserEntity
import com.alpaca.app.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TrailViewModel(private val container: AppContainer) : ViewModel() {

    data class NodeUi(
        val lessonId: String,
        val title: String,
        val status: LessonStatus
    )

    data class UnitTab(
        val unitId: String,
        val title: String,
        val region: String,
        val unlocked: Boolean,
        val completed: Int,
        val total: Int
    )

    data class UiState(
        val language: CourseLanguage = CourseLanguage.Spanish,
        val units: List<UnitTab> = emptyList(),
        val selectedUnitId: String = "es_u1",
        val nodes: List<NodeUi> = emptyList(),
        val user: UserEntity? = null,
        val mistakeCount: Int = 0
    ) {
        val selectedUnit: UnitTab? get() = units.firstOrNull { it.unitId == selectedUnitId }
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            container.progressRepository.seedIfNeeded()
            combine(
                container.progressRepository.observeProgress(),
                container.gamificationRepository.observeUser(),
                container.mistakeRepository.observeCount()
            ) { progress, user, mistakes -> Triple(progress, user, mistakes) }
                .flatMapLatest { (progress, user, mistakes) ->
                    container.prefs.prefs.map { prefs ->
                        val language = CourseLanguage.byId(prefs.currentLanguage)
                        val units = container.contentRepository.loadUnits(language.id)
                        val tabs = units.map { unit ->
                            UnitTab(
                                unitId = unit.unitId,
                                title = unit.title,
                                region = unit.region,
                                unlocked = container.progressRepository.unitUnlocked(
                                    units, progress, unit.unitId
                                ),
                                completed = unit.lessons.count {
                                    progress[it.lessonId]?.status == LessonStatus.COMPLETE
                                },
                                total = unit.lessons.size
                            )
                        }
                        val selected = tabs.firstOrNull { it.unitId == prefs.currentUnitId }
                            ?: tabs.firstOrNull { it.unlocked }
                            ?: tabs.firstOrNull()
                        val nodeUi = if (selected == null) emptyList() else
                            units.first { it.unitId == selected.unitId }.lessons.map { lesson ->
                                NodeUi(
                                    lessonId = lesson.lessonId,
                                    title = lesson.title,
                                    status = container.progressRepository.effectiveStatus(
                                        units.first { it.unitId == selected.unitId },
                                        progress,
                                        lesson.lessonId
                                    )
                                )
                            }
                        UiState(
                            language = language,
                            units = tabs,
                            selectedUnitId = selected?.unitId ?: "${language.id}_u1",
                            nodes = nodeUi,
                            user = user,
                            mistakeCount = mistakes
                        )
                    }
                }
                .collect { _state.value = it }
        }
    }

    fun selectUnit(unitId: String) {
        viewModelScope.launch { container.prefs.setCurrentUnit(unitId) }
    }
}
