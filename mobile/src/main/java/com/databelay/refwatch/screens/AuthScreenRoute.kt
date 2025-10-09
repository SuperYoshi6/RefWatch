package com.databelay.refwatch.screens

import AuthScreen
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.databelay.refwatch.auth.AuthState
import com.databelay.refwatch.auth.AuthViewModel

// In your navigation graph setup or a dedicated screen route composable
// For example, AuthScreenRoute.kt

@Composable
fun AuthScreenRoute(
    authViewModel: AuthViewModel = hiltViewModel(), // Or however you get your ViewModel
    onSignInSuccess: () -> Unit // Navigate away on success
) {
    val authState by authViewModel.authState.collectAsState() // Observe actual auth success/failure
    val isLoading by authViewModel.isLoading.collectAsState()
    val authError by authViewModel.authError.collectAsState()

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoginMode by rememberSaveable { mutableStateOf(true) } // Default to login mode

    // Handle actual authentication success
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onSignInSuccess()
        }
    }

    AuthScreen(
        emailValue = email,
        passwordValue = password,
        isLoginMode = isLoginMode,
        isLoading = isLoading,
        errorMessage = authError, // Pass the error message from ViewModel
        onEmailChange = { email = it },
        onPasswordChange = { password = it },
        onToggleLoginMode = { isLoginMode = !isLoginMode },
        onAuthAction = {
            if (isLoginMode) {
                Log.d("AuthScreenRoute", "Attempting Login with Email: $email")
                authViewModel.signInWithEmailPassword(email, password)
            } else {
                Log.d("AuthScreenRoute", "Attempting Sign Up with Email: $email")
                authViewModel.signUpWithEmailPassword(email, password)
            }
        },
        onClearError = { authViewModel.clearAuthError() }
    )
}
