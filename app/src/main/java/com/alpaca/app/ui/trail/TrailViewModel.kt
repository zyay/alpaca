package com.alpaca.app.ui.trail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alpaca.app.data.content.CourseUnit
import com.alpaca.app.data.db.entities.LessonStatus
import com.alpaca.app.data.db.entities.UserEntity
import com.alpaca.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class TrailViewModel(private val container: AppContainer) : ViewModel() {

    data class NodeUi(
        val lessonId: String,
        val title: String,
        val status: LessonStatus
    )

    data class UiState(
        val unit: CourseUnit? = null,
        val nodes: List<NodeUi> = emptyList(),
        val user: UserEntity? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            container.progressRepository.seedIfNeeded()
            val unit = container.contentRepository.loadUnit()
            combine(
                container.progressRepository.observeProgress(),
                container.gamificationRepository.observeUser()
            ) { progress, user ->
                UiState(
                    unit = unit,
                    nodes = unit.lessons.map { lesson ->
                        NodeUi(
                            lessonId = lesson.lessonId,
                            title = lesson.title,
                            status = container.progressRepository.effectiveStatus(
                                unit, progress, lesson.lessonId
                            )
                        )
                    },
                    user = user
                )
            }.collect { _state.value = it }
        }
    }
}
