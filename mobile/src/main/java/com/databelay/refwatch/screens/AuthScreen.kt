package com.databelay.refwatch.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.databelay.refwatch.R
import com.databelay.refwatch.common.theme.AccentGreen
import com.databelay.refwatch.common.theme.Border
import com.databelay.refwatch.common.theme.PitchBackground
import com.databelay.refwatch.common.theme.RefWatchMobileTheme
import com.databelay.refwatch.common.theme.RefWatchWordmark
import com.databelay.refwatch.common.theme.Surface
import com.databelay.refwatch.common.theme.TextMuted
import com.databelay.refwatch.common.theme.TextPrimary

// In AuthScreen.kt

@Composable
fun AuthScreen(
    emailValue: String,
    passwordValue: String,
    isLoginMode: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    infoMessage: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleLoginMode: () -> Unit,
    onAuthAction: () -> Unit, // Renamed from onSignInSuccess for clarity, handles both login/signup
    onGoogleSignIn: () -> Unit,
    onForgotPassword: () -> Unit,
    onClearError: () -> Unit, // To clear error when user types
    modifier: Modifier = Modifier // Keep the modifier
) {
    // No more internal remember states for email, password, isLoginMode
    // No more direct viewModel access for isLoading and errorMessage

    val context = LocalContext.current
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    PitchBackground(modifier = modifier.imePadding()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(24.dp)
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Hero block: wordmark + bilingual tagline (mirrors site)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RefWatchWordmark(
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = androidx.compose.ui.unit.TextUnit.Unspecified
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (isLoginMode)
                        stringResource(R.string.login_welcome_back)
                    else
                        stringResource(R.string.create_account),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isLoginMode)
                        stringResource(R.string.login_sign_in)
                    else
                        stringResource(R.string.login_register),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AccentGreen,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.auth_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            Spacer(Modifier.height(20.dp))

            // Card containing the form (matches the .feature-card style on the site)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = emailValue,
                        onValueChange = { newValue ->
                            onEmailChange(newValue)
                            if (errorMessage != null) onClearError()
                        },
                        label = { Text(stringResource(R.string.email)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        enabled = !isLoading,
                        colors = darkAuthFieldColors(),
                        isError = errorMessage != null && (
                                errorMessage.contains("email", ignoreCase = true) ||
                                        errorMessage.contains("credentials", ignoreCase = true) ||
                                        errorMessage.contains("empty", ignoreCase = true) && emailValue.isEmpty()
                                )
                    )
                    OutlinedTextField(
                        value = passwordValue,
                        onValueChange = { newValue ->
                            onPasswordChange(newValue)
                            if (errorMessage != null) onClearError()
                        },
                        label = { Text(stringResource(R.string.password)) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (passwordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password),
                                    tint = TextMuted
                                )
                            }
                        },
                        enabled = !isLoading,
                        colors = darkAuthFieldColors(),
                        isError = errorMessage != null && (
                                errorMessage.contains("password", ignoreCase = true) ||
                                        errorMessage.contains("credentials", ignoreCase = true) ||
                                        errorMessage.contains("empty", ignoreCase = true) && passwordValue.isEmpty()
                                )
                    )

                    errorMessage?.let { errMessage ->
                        Text(
                            text = errMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    infoMessage?.let { message ->
                        Text(
                            text = message,
                            color = AccentGreen,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    // Conditional terms / privacy copy
                    if (!isLoginMode) {
                        val termsLabel = stringResource(R.string.auth_terms_of_service)
                        val privacyLabel = stringResource(R.string.auth_privacy_policy)
                        val annotatedText = buildAnnotatedString {
                            append(stringResource(R.string.auth_terms_prefix))
                            pushStringAnnotation(tag = "TERMS", annotation = "https://example.com/terms")
                            withStyle(style = SpanStyle(color = AccentGreen, textDecoration = TextDecoration.Underline)) {
                                append(termsLabel)
                            }
                            pop()
                            append(stringResource(R.string.auth_terms_and))
                            pushStringAnnotation(tag = "PRIVACY", annotation = "https://example.com/privacy")
                            withStyle(style = SpanStyle(color = AccentGreen, textDecoration = TextDecoration.Underline)) {
                                append(privacyLabel)
                            }
                            pop()
                            append(stringResource(R.string.auth_terms_suffix))
                        }
                        Text(
                            text = annotatedText,
                            style = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
                            modifier = Modifier.fillMaxWidth(),
                            color = TextMuted
                        )
                    }

                    Button(
                        onClick = { onAuthAction() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGreen,
                            contentColor = Color(0xFF0A1A0A)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF0A1A0A)
                            )
                        } else {
                            Text(
                                text = if (isLoginMode) stringResource(R.string.login_sign_in) else stringResource(R.string.login_register),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (isLoginMode) {
                        // Visual Separator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Border,
                                thickness = 1.dp
                            )
                            Text(
                                text = stringResource(R.string.or_separator),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Border,
                                thickness = 1.dp
                            )
                        }

                        // Google Sign-In Button
                        OutlinedButton(
                            onClick = onGoogleSignIn,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // Local Google Icon
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_google_logo),
                                    contentDescription = null,
                                    tint = Color.Unspecified, // Keep original colors
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.login_with_google),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (isLoginMode) {
                        TextButton(
                            onClick = onForgotPassword,
                            enabled = !isLoading && emailValue.isNotBlank(),
                            colors = ButtonDefaults.textButtonColors(contentColor = AccentGreen)
                        ) {
                            Text(stringResource(R.string.login_forgot_password))
                        }
                    }
                    TextButton(
                        onClick = {
                            onToggleLoginMode()
                            if (errorMessage != null) onClearError()
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.textButtonColors(contentColor = TextMuted)
                    ) {
                        Text(if (isLoginMode) stringResource(R.string.login_no_account_register) else stringResource(R.string.login_have_account_sign_in))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            // Footer line — same vibe as "Mit ❤️ für Schiedsrichter gemacht"
            Text(
                text = stringResource(R.string.auth_footer),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun darkAuthFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = Surface,
    unfocusedContainerColor = Surface,
    focusedBorderColor = AccentGreen,
    unfocusedBorderColor = Border,
    focusedLabelColor = AccentGreen,
    unfocusedLabelColor = TextMuted,
    cursorColor = AccentGreen,
    disabledTextColor = TextMuted,
    disabledContainerColor = Surface,
    disabledBorderColor = Border,
    disabledLabelColor = TextMuted,
    errorBorderColor = MaterialTheme.colorScheme.error,
    errorLabelColor = MaterialTheme.colorScheme.error,
    errorCursorColor = MaterialTheme.colorScheme.error
)

// In your AuthScreen.kt or a preview file

// (Keep your PreviewAuthViewModel and FakeAuthRepository as before, they don't change for this refactor)

@Preview(name = "Stateless Login Mode - Light", showBackground = true,uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun StatelessAuthScreenPreview_LoginMode() {
    var email by remember { mutableStateOf("test@example.com") }
    var password by remember { mutableStateOf("password") }
    var isLoginMode by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    RefWatchMobileTheme {
        AuthScreen(
            emailValue = email,
            passwordValue = password,
            isLoginMode = isLoginMode,
            isLoading = isLoading,
            errorMessage = errorMessage,
            infoMessage = null,
            onEmailChange = { email = it },
            onPasswordChange = { password = it },
            onToggleLoginMode = { isLoginMode = !isLoginMode },
            onAuthAction = { isLoading = true /* Simulate action */ },
            onGoogleSignIn = {},
            onForgotPassword = {},
            onClearError = { errorMessage = null }
        )
    }}

@Preview(name = "Stateless Sign Up - Error", showBackground = true,uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun StatelessAuthScreenPreview_SignUpError() {
    var email by remember { mutableStateOf("newuser@example.com") }
    var password by remember { mutableStateOf("123") } // Short password might be an error
    var isLoginMode by remember { mutableStateOf(false) } // Sign Up Mode
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>("Password should be at least 6 characters.") }

    RefWatchMobileTheme {
        AuthScreen(
            emailValue = email,
            passwordValue = password,
            isLoginMode = isLoginMode,
            isLoading = isLoading,
            errorMessage = errorMessage,
            infoMessage = null,
            onEmailChange = { email = it; errorMessage = null }, // Clear error on change
            onPasswordChange = { password = it; errorMessage = null }, // Clear error on change
            onToggleLoginMode = { isLoginMode = !isLoginMode },
            onAuthAction = { /* Simulate action */ },
            onGoogleSignIn = {},
            onForgotPassword = {},
            onClearError = { errorMessage = null }
        )
    }
}
