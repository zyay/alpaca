package com.alpaca.app

import android.app.Application
import com.alpaca.app.data.repository.LessonResult
import com.alpaca.app.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlpacaApp : Application() {
    lateinit var container: AppContainer
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Result of the most recent lesson, handed to the Summary screen. */
    var lastLessonResult: LessonResult? = null

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch { container.init() }
    }
}
