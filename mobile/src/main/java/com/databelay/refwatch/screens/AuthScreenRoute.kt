package com.databelay.refwatch.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.databelay.refwatch.R
import com.databelay.refwatch.auth.AuthState
import com.databelay.refwatch.auth.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

// In your navigation graph setup or a dedicated screen route composable
// For example, AuthScreenRoute.kt

@Composable
fun AuthScreenRoute(
    authViewModel: AuthViewModel = hiltViewModel(), // Or however you get your ViewModel
    onSignInSuccess: () -> Unit // Navigate away on success
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState() // Observe actual auth success/failure
    val isLoading by authViewModel.isLoading.collectAsState()
    val authError by authViewModel.authError.collectAsState()
    val authMessage by authViewModel.authMessage.collectAsState()

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoginMode by rememberSaveable { mutableStateOf(true) } // Default to login mode

    // Google Sign-In setup
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { idToken ->
                authViewModel.signInWithGoogle(idToken)
            }
        } catch (e: ApiException) {
            Log.e("AuthScreenRoute", "Google Sign-In failed", e)
            // Error handled by AuthViewModel if we pass it, or just log here.
        }
    }

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
        infoMessage = authMessage,
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
        onGoogleSignIn = {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        },
        onForgotPassword = {
            val languageCode = java.util.Locale.getDefault().language
            authViewModel.sendPasswordResetEmail(email, languageCode)
        },
        onClearError = { authViewModel.clearAuthError() }
    )
}
