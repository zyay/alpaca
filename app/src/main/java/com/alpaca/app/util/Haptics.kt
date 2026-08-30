package com.alpaca.app.util

import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Crisp Duolingo-style haptics through View.performHapticFeedback, which
 * respects the user's system touch-feedback setting.
 */
class HapticPlayer(private val view: View) {
    fun light() = view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    fun correctThud() = view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    fun wrongBuzz() = view.performHapticFeedback(HapticFeedbackConstants.REJECT)
    fun celebrate() {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        view.postDelayed({ view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }, 90)
        view.postDelayed({ view.performHapticFeedback(HapticFeedbackConstants.CONFIRM) }, 200)
    }
}
