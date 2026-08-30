package com.alpaca.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.alpaca.app.AlpacaApp
import com.alpaca.app.di.LocalViewModelFactory
import com.alpaca.app.ui.leaderboard.LeaderboardScreen
import com.alpaca.app.ui.leaderboard.LeaderboardViewModel
import com.alpaca.app.ui.lesson.LessonScreen
import com.alpaca.app.ui.lesson.LessonViewModel
import com.alpaca.app.ui.settings.SettingsScreen
import com.alpaca.app.ui.settings.SettingsViewModel
import com.alpaca.app.ui.summary.SummaryScreen
import com.alpaca.app.ui.trail.TrailScreen
import com.alpaca.app.ui.trail.TrailViewModel
import com.alpaca.app.ui.voice.VoiceCallScreen
import com.alpaca.app.ui.voice.VoiceCallViewModel
import com.alpaca.app.util.HapticPlayer
import kotlinx.serialization.Serializable

@Serializable
data object TrailRoute

@Serializable
data class LessonRoute(val lessonId: String)

@Serializable
data object SummaryRoute

@Serializable
data object VoiceRoute

@Serializable
data object LeaderboardRoute

@Serializable
data object SettingsRoute

@Composable
fun AppNavHost(
    app: AlpacaApp,
    haptics: HapticPlayer?,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val factory = LocalViewModelFactory.current

    NavHost(
        navController = navController,
        startDestination = TrailRoute,
        modifier = modifier
    ) {
        composable<TrailRoute> {
            val viewModel: TrailViewModel = viewModel(factory = factory)
            TrailScreen(
                viewModel = viewModel,
                onOpenLesson = { lessonId -> navController.navigate(LessonRoute(lessonId)) },
                onOpenVoice = { navController.navigate(VoiceRoute) },
                onOpenLeaderboard = { navController.navigate(LeaderboardRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                haptics = haptics
            )
        }

        composable<LessonRoute> { entry ->
            val route = entry.toRoute<LessonRoute>()
            val viewModel: LessonViewModel = viewModel(factory = factory)
            LessonScreen(
                lessonId = route.lessonId,
                viewModel = viewModel,
                haptics = haptics,
                onFinished = {
                    navController.navigate(SummaryRoute) {
                        popUpTo(TrailRoute)
                    }
                },
                onQuit = { navController.popBackStack() }
            )
        }

        composable<SummaryRoute> {
            SummaryScreen(
                app = app,
                haptics = haptics,
                onContinue = { navController.popBackStack(TrailRoute, inclusive = false) }
            )
        }

        composable<VoiceRoute> {
            val viewModel: VoiceCallViewModel = viewModel(factory = factory)
            VoiceCallScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable<LeaderboardRoute> {
            val viewModel: LeaderboardViewModel = viewModel(factory = factory)
            LeaderboardScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable<SettingsRoute> {
            val viewModel: SettingsViewModel = viewModel(factory = factory)
            SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}
