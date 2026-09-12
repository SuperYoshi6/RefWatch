package com.databelay.refwatch.wear.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.databelay.refwatch.wear.auth.WatchAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class WatchLoginViewModel @Inject constructor(
    private val authManager: WatchAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(newEmail: String) {
        _uiState.value = _uiState.value.copy(email = newEmail, errorMessage = null)
    }

    fun onLogin(password: String) {
        val email = _uiState.value.email
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "E-Mail/Passwort leer")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authManager.signInWithEmail(email, password)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, 
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Fehler"
                )
            }
        }
    }

    fun onGoogleLogin(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authManager.signInWithGoogle(idToken)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Google Login fehlgeschlagen"
                )
            }
        }
    }
}
