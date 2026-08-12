package com.databelay.refwatch.wear.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*
import com.databelay.refwatch.R

@Composable
fun WatchLoginScreen(
    onLoginSuccess: () -> Unit,
    onCancel: () -> Unit,
    viewModel: WatchLoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var password by remember { mutableStateOf("") }
    val listState = rememberScalingLazyListState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onLoginSuccess()
        }
    }

    ScreenScaffold(
        scrollIndicator = { ScrollIndicator(state = listState) }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 32.dp, bottom = 32.dp, start = 8.dp, end = 8.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.login_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                TextInputField(
                    value = uiState.email,
                    onValueChange = { viewModel.onEmailChange(it) },
                    label = stringResource(R.string.email_label),
                    keyboardType = KeyboardType.Email
                )
            }

            item {
                TextInputField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.password_label),
                    keyboardType = KeyboardType.Password,
                    isPassword = true
                )
            }

            if (uiState.errorMessage != null) {
                item {
                    Text(
                        text = uiState.errorMessage!!,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            item {
                Button(
                    onClick = { viewModel.onLogin(password) },
                    enabled = !uiState.isLoading && uiState.email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text(stringResource(R.string.login_button))
                    }
                }
            }

            item {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun TextInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    isPassword: Boolean = false
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = androidx.compose.material3.TextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.LightGray,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}
