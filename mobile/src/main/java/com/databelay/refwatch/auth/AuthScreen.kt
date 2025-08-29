package com.databelay.refwatch.auth

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.databelay.refwatch.common.LegalLinks

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onSignInSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(true) }

    val authState by authViewModel.authState.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val currentErrorMessage by authViewModel.authError.collectAsState() // Use 'by'

    val context = LocalContext.current // For opening URLs

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            Log.d("AuthScreen", "Authentication successful. Calling onSignInSuccess.")
            onSignInSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (isLoginMode) "Login" else "Sign Up",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp)) // Reduced space a bit

        // Terms and Privacy Text - only shown in Sign Up mode
        if (!isLoginMode) {
            val annotatedString = buildAnnotatedString {
                append("By signing up, you agree to our\n") // Added newline for better spacing

                pushStringAnnotation(tag = "TERMS", annotation = LegalLinks.TERMS_OF_USE_URL)
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append("Terms of Use")
                }
                pop()

                append(" and ")

                pushStringAnnotation(tag = "PRIVACY", annotation = LegalLinks.PRIVACY_POLICY_URL)
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append("Privacy Policy")
                }
                pop()
                append(".")
            }

            ClickableText(
                text = annotatedString,
                style = MaterialTheme.typography.bodySmall.copy(
                    textAlign = TextAlign.Center, // Center align the text
                    color = MaterialTheme.colorScheme.onBackground // Ensure good contrast
                ),
                onClick = { offset ->
                    annotatedString.getStringAnnotations(tag = "TERMS", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("AuthScreen", "Could not open Terms of Use URL", e)
                            }
                        }

                    annotatedString.getStringAnnotations(tag = "PRIVACY", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("AuthScreen", "Could not open Privacy Policy URL", e)
                            }
                        }
                },
                modifier = Modifier.padding(horizontal = 16.dp) // Add some horizontal padding
            )
            Spacer(modifier = Modifier.height(24.dp)) // Space before email field
        } else {
            // Add some space if login mode and the text isn't shown, to keep fields consistent
            Spacer(modifier = Modifier.height(24.dp + (MaterialTheme.typography.bodySmall.fontSize.value * 2).dp)) // Approximate height of the ClickableText + Spacer
        }


        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                if (currentErrorMessage != null) {
                    authViewModel.clearAuthError()
                }
            },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = !isLoading,
            isError = currentErrorMessage != null && (
                    currentErrorMessage!!.contains("email", ignoreCase = true) ||
                            currentErrorMessage!!.contains("credentials", ignoreCase = true) ||
                            currentErrorMessage!!.contains("empty", ignoreCase = true) && email.isEmpty()
                    )
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                if (currentErrorMessage != null) {
                    authViewModel.clearAuthError()
                }
            },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = !isLoading,
            isError = currentErrorMessage != null && (
                    currentErrorMessage!!.contains("password", ignoreCase = true) ||
                            currentErrorMessage!!.contains("credentials", ignoreCase = true) ||
                            currentErrorMessage!!.contains("empty", ignoreCase = true) && password.isEmpty()
                    )
        )
        Spacer(modifier = Modifier.height(8.dp))

        currentErrorMessage?.let { errMessage ->
            Text(
                text = errMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isLoginMode) {
                    Log.d("AuthScreen", "Attempting Login with Email: $email, Password: $password")
                    authViewModel.signInWithEmailPassword(email, password)
                } else {
                    Log.d("AuthScreen", "Attempting Sign Up with Email: $email, Password: $password")
                    authViewModel.signUpWithEmailPassword(email, password)
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(if (isLoginMode) "Login" else "Sign Up")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            onClick = {
                isLoginMode = !isLoginMode
                if (currentErrorMessage != null) {
                    authViewModel.clearAuthError()
                }
            },
            enabled = !isLoading
        ) {
            Text(if (isLoginMode) "Need an account? Sign Up" else "Have an account? Login")
        }
    }
}
