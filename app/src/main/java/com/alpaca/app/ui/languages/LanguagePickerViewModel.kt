package com.alpaca.app.ui.languages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alpaca.app.data.content.CourseLanguage
import com.alpaca.app.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LanguagePickerViewModel(private val container: AppContainer) : ViewModel() {

    val available = CourseLanguage.available
    val comingSoon = CourseLanguage.comingSoon

    val currentLanguageId: StateFlow<String> = container.prefs.prefs
        .map { it.currentLanguage }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "es")

    fun select(languageId: String) {
        viewModelScope.launch { container.prefs.setCurrentLanguage(languageId) }
    }
}
