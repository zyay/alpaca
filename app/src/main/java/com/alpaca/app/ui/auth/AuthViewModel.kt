package com.alpaca.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alpaca.app.BuildConfig
import com.alpaca.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AuthViewModel(private val container: AppContainer) : ViewModel() {

    enum class Mode { SIGN_IN, SIGN_UP }

    data class UiState(
        val mode: Mode = Mode.SIGN_IN,
        val email: String = "",
        val password: String = "",
        val name: String = "",
        val busy: Boolean = false,
        val error: String? = null,
        val signedIn: Boolean = false
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            val prefs = container.prefs.prefs.first()
            _state.value = _state.value.copy(
                signedIn = prefs.signedIn,
                name = if (prefs.displayName == "Explorer") "" else prefs.displayName
            )
        }
    }

    fun setMode(mode: Mode) {
        _state.value = _state.value.copy(mode = mode, error = null)
    }

    fun onEmailChange(value: String) {
        _state.value = _state.value.copy(email = value, error = null)
    }

    fun onPasswordChange(value: String) {
        _state.value = _state.value.copy(password = value, error = null)
    }

    fun onNameChange(value: String) {
        _state.value = _state.value.copy(name = value, error = null)
    }

    fun submit() {
        val s = _state.value
        if (s.busy) return
        val email = s.email.trim()
        val password = s.password
        val name = s.name.trim().ifEmpty { "Learner" }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.value = s.copy(error = "Enter a valid email address")
            return
        }
        if (password.length < 8) {
            _state.value = s.copy(error = "Password needs at least 8 characters")
            return
        }

        val baseUrl = BuildConfig.VERCEL_BASE_URL
        if (baseUrl.isBlank()) {
            _state.value = s.copy(error = "No backend configured in this build")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null)
            val result = if (s.mode == Mode.SIGN_UP) {
                container.authClient.signup(baseUrl, email, password, name)
            } else {
                container.authClient.login(baseUrl, email, password)
            }
            val response = result.getOrNull()
            when {
                result.isFailure -> _state.value = _state.value.copy(
                    busy = false,
                    error = result.exceptionOrNull()?.message ?: "Network error — try again"
                )
                response == null -> _state.value = _state.value.copy(
                    busy = false, error = "Network error — try again"
                )
                !response.available -> _state.value = _state.value.copy(
                    busy = false,
                    error = "Account storage isn't attached on the backend yet"
                )
                response.ok && response.token.isNotBlank() -> {
                    val prefs = container.prefs.prefs.first()
                    container.prefs.setSession(
                        response.token, response.userId, response.email, response.name
                    )
                    if (prefs.displayName == "Explorer" && response.name.isNotBlank()) {
                        container.prefs.setDisplayName(response.name)
                    }
                    _state.value = _state.value.copy(busy = false, signedIn = true)
                }
                else -> _state.value = _state.value.copy(
                    busy = false,
                    error = response.error ?: "Something went wrong"
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            val baseUrl = BuildConfig.VERCEL_BASE_URL
            val prefs = container.prefs.prefs.first()
            if (baseUrl.isNotBlank() && prefs.authToken.isNotBlank()) {
                runCatching { container.authClient.logout(baseUrl, prefs.authToken) }
            }
            container.prefs.clearSession()
            _state.value = _state.value.copy(signedIn = false, password = "", error = null)
        }
    }
}
