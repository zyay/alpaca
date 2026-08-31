package com.alpaca.app.ui.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alpaca.app.BuildConfig
import com.alpaca.app.data.content.ContentRepository
import com.alpaca.app.data.content.Exercise
import com.alpaca.app.data.content.Lesson
import com.alpaca.app.data.db.entities.MistakeEntity
import com.alpaca.app.data.db.entities.UserEntity
import com.alpaca.app.data.datastore.UserPrefs
import com.alpaca.app.data.repository.LessonResult
import com.alpaca.app.di.AppContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LessonViewModel(private val container: AppContainer) : ViewModel() {

    val prefs: StateFlow<UserPrefs> =
        container.prefs.prefs.stateIn(viewModelScope, SharingStarted.Eagerly, UserPrefs())

    data class Correction(val correctText: String, val explanation: String?)

    data class UiState(
        val loading: Boolean = true,
        val lessonTitle: String = "",
        val exercise: Exercise? = null,
        val index: Int = 0,
        val total: Int = 0,
        val energy: Int = UserEntity.MAX_ENERGY,
        val attempt: Int = 0,
        val correction: Correction? = null,
        val justCorrect: Boolean = false,
        val finished: Boolean = false,
        val outOfEnergy: Boolean = false,
        val languageTag: String = "es-ES"
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    /** Set by the screen: where to deliver the finished result. */
    var resultSink: (LessonResult) -> Unit = {}

    private var lesson: Lesson? = null
    private var isReview = false
    private var reviewMistakes: List<MistakeEntity> = emptyList()
    private var correctCount = 0
    private val mistaken = mutableListOf<String>()

    fun start(lessonId: String) {
        if (lesson != null) return
        viewModelScope.launch {
            val review = lessonId == ContentRepository.REVIEW_LESSON_ID
            val loaded = if (review) {
                reviewMistakes = container.mistakeRepository.recent(12)
                container.contentRepository.buildReviewLesson(reviewMistakes)
            } else {
                container.contentRepository.findLesson(lessonId)
            } ?: return@launch
            isReview = review
            lesson = loaded
            container.progressRepository.recordAttempt(lessonId)
            val user = container.gamificationRepository.currentUser()
            val langId = loaded.lessonId.substringBefore('_')
                .takeIf { it.length == 2 && it != loaded.lessonId }
                ?: prefs.value.currentLanguage
            _state.value = UiState(
                loading = false,
                lessonTitle = loaded.title,
                exercise = loaded.exercises.firstOrNull(),
                total = loaded.exercises.size,
                energy = user.fleeceEnergy,
                languageTag = com.alpaca.app.data.content.CourseLanguage.byId(langId).speechTag
            )
        }
    }

    fun onCorrect() {
        val s = _state.value
        if (s.justCorrect || s.finished) return
        correctCount++
        _state.value = s.copy(justCorrect = true, correction = null)
        viewModelScope.launch {
            delay(950)
            advance()
        }
    }

    fun onWrong(correctText: String, explanation: String?, mistakeLabel: String) {
        val s = _state.value
        if (s.correction != null || s.finished) return
        viewModelScope.launch {
            mistaken.add(mistakeLabel)
            if (!isReview) {
                val l = lesson
                val originalId = l?.lessonId?.takeIf { it != ContentRepository.REVIEW_LESSON_ID }
                if (originalId != null) {
                    container.mistakeRepository.log(originalId, s.index, mistakeLabel)
                }
            }
            val remaining = if (prefs.value.alpacaMax) s.energy
                else container.gamificationRepository.consumeEnergy()
            if (remaining <= 0) {
                finish(outOfEnergy = true)
            } else {
                _state.value = _state.value.copy(
                    energy = remaining,
                    correction = Correction(correctText, explanation)
                )
            }
        }
    }

    fun dismissCorrection() {
        val s = _state.value
        _state.value = s.copy(correction = null, attempt = s.attempt + 1)
    }

    fun quit() {
        if (_state.value.finished) return
        finish(outOfEnergy = true)
    }

    private fun advance() {
        val l = lesson ?: return
        val nextIndex = _state.value.index + 1
        val next = l.exercises.getOrNull(nextIndex)
        if (next == null) {
            finish(outOfEnergy = false)
        } else {
            _state.value = _state.value.copy(
                exercise = next,
                index = nextIndex,
                justCorrect = false,
                attempt = 0
            )
        }
    }

    private fun reportLeagueXp(xp: Int) {
        val baseUrl = BuildConfig.VERCEL_BASE_URL
        if (baseUrl.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val deviceId = container.prefs.ensureDeviceId()
                val name = prefs.value.displayName
                container.leagueClient.reportXp(
                    baseUrl, deviceId, name, xp, prefs.value.authToken
                ).getOrThrow()
            }
        }
    }

    private fun finish(outOfEnergy: Boolean) {
        val l = lesson ?: return
        val total = l.exercises.size
        viewModelScope.launch {
            if (!outOfEnergy) {
                val xp = 10 + correctCount
                val coins = if (mistaken.isEmpty()) 10 else 5
                val reward = container.gamificationRepository.awardLesson(xp, coins)
                container.questRepository.recordLesson(xp, coins)
                reportLeagueXp(xp)
                val score = if (total > 0) correctCount * 100 / total else 0
                container.progressRepository.completeLesson(l.lessonId, score)
                if (isReview) {
                    container.mistakeRepository.consume(reviewMistakes)
                }
                resultSink(
                    LessonResult(
                        lessonId = l.lessonId,
                        lessonTitle = l.title,
                        xpGained = reward.xpGained,
                        coinsGained = reward.coinsGained,
                        correctCount = correctCount,
                        totalCount = total,
                        streakIncreased = reward.streakIncreased,
                        newStreak = reward.newStreak,
                        outOfEnergy = false,
                        mistaken = mistaken.distinct()
                    )
                )
            } else {
                resultSink(
                    LessonResult(
                        lessonId = l.lessonId,
                        lessonTitle = l.title,
                        xpGained = 0,
                        coinsGained = 0,
                        correctCount = correctCount,
                        totalCount = total,
                        streakIncreased = false,
                        newStreak = container.gamificationRepository.currentUser().streakDays,
                        outOfEnergy = true,
                        mistaken = mistaken.distinct()
                    )
                )
            }
            _state.value = _state.value.copy(finished = true, outOfEnergy = outOfEnergy)
        }
    }
}
