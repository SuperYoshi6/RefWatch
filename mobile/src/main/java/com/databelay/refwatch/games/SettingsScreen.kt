package com.databelay.refwatch.games

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.databelay.refwatch.common.LegalLinks

// Assuming you have an AuthViewModel or similar to handle account deletion
// import com.databelay.refwatch.auth.AuthViewModel
// import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    // Inject your AuthViewModel or a specific ViewModel responsible for account deletion
    // authViewModel: AuthViewModel = hiltViewModel() // Example
    onDeleteAccountConfirmed: () -> Unit // Callback when deletion is confirmed
) {
    val context = LocalContext.current
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                "Legal",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Privacy Policy Link
            val privacyPolicyAnnotatedString = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append("Privacy Policy")
                }
            }

            Text(
                text = privacyPolicyAnnotatedString,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(LegalLinks.PRIVACY_POLICY_URL))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("SettingsScreen", "Failed to open Privacy Policy URL: ${LegalLinks.PRIVACY_POLICY_URL}", e)
                        }
                    }
            )

            // Terms of Use Link
            val termsOfUseAnnotatedString = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append("Terms of Use")
                }
            }

            Text(
                text = termsOfUseAnnotatedString,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(LegalLinks.TERMS_OF_USE_URL))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("SettingsScreen", "Failed to open Terms of Use URL: ${LegalLinks.TERMS_OF_USE_URL}", e)
                        }
                    }
            )

            Spacer(modifier = Modifier.height(24.dp)) // More space before other settings

            Text("Other Settings will go here...", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.weight(1f)) // Pushes delete account to bottom

            Button(
                onClick = { showDeleteConfirmationDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete Account")
            }
        }
    }

    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text("Delete Account?") },
            text = { Text("This action is permanent and cannot be undone. All your data will be erased. Are you sure you want to delete your account?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmationDialog = false
                        // Trigger the actual account deletion logic
                        // authViewModel.deleteUserAccount() // Example call to ViewModel
                        onDeleteAccountConfirmed()
                        Log.d("SettingsScreen", "Account deletion confirmed by user.")
                        // After deletion, you'll likely navigate the user away (e.g., to login screen)
                        // This navigation should be handled after the onDeleteAccountConfirmed callback completes,
                        // potentially observing a state from the ViewModel.
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmationDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenMobilePreview() {
    MaterialTheme {
        SettingsScreen(onNavigateBack = {}, onDeleteAccountConfirmed = {})
    }
}
