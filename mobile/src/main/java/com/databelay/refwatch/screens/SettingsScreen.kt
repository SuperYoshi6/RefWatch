package com.databelay.refwatch.screens

import android.content.Intent
import android.content.res.Configuration
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.databelay.refwatch.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.databelay.refwatch.BuildConfig
import com.databelay.refwatch.common.LegalLinks
import com.databelay.refwatch.common.getAppVersionCode
import com.databelay.refwatch.common.getAppVersionName
import com.databelay.refwatch.common.theme.RefWatchMobileTheme

// Assuming you have an AuthViewModel or similar to handle account deletion
// import com.databelay.refwatch.auth.AuthViewModel
// import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    // Inject your AuthViewModel or a specific ViewModel responsible for account deletion
    // authViewModel: AuthViewModel = hiltViewModel() // Example
    onDeleteAccountConfirmed: () -> Unit, // Callback when deletion is confirmed
    onDeleteAllCompletedGames: () -> Unit = {} // Added default value to fix compilation error
) {
    val context = LocalContext.current
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showDeleteAllCompletedConfirmationDialog by remember { mutableStateOf(false) }
    var appVersionName by remember { mutableStateOf("Loading...") } // State for version name
    var appVersionNumber by remember { mutableLongStateOf(0L) } // State for version name
    val buildDateString = BuildConfig.BUILD_TIME

    // LaunchedEffect to get version name (it's a synchronous call but good practice
    // if it were asynchronous, and keeps UI responsive during initial composition)
    LaunchedEffect(Unit) {
        appVersionName = getAppVersionName(context)
        appVersionNumber = getAppVersionCode(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
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
                "Rechtliches",
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
                    append("Datenschutzerklärung")
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
                    append("Nutzungsbedingungen")
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

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Projekt",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // App Logo
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "RefWatch Logo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(vertical = 8.dp)
            )

            Text(
                "Website",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(LegalLinks.WEBSITE_URL))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("SettingsScreen", "Failed to open Website URL", e)
                        }
                    }
            )

            Text(
                "GitHub Repository",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(LegalLinks.GITHUB_URL))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("SettingsScreen", "Failed to open GitHub URL", e)
                        }
                    }
            )

            Text(
                "Superior Zex",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(LegalLinks.SUPERIOR_ZEX_URL))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("SettingsScreen", "Failed to open Superior Zex URL", e)
                        }
                    }
            )

            // --- END BUILD INFO TEXT ---
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Datenverwaltung",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Button(
                onClick = { showDeleteAllCompletedConfirmationDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Alle vergangenen Spiele löschen")
            }

            Spacer(modifier = Modifier.weight(1f)) // Pushes delete account to bottom

            Button(
                onClick = { showDeleteConfirmationDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Account löschen")
            }
            
            // Build Info
            Text(
                text = "Version: $appVersionName ($appVersionNumber)\nBuild: $buildDateString",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text("Account löschen?") },
            text = { Text("Diese Aktion ist endgültig und kann nicht rückgängig gemacht werden. Bist du sicher, dass du deinen Account löschen möchtest?") },
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
                    Text("Abbrechen")
                }
            }
        )
    }

    if (showDeleteAllCompletedConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllCompletedConfirmationDialog = false },
            title = { Text("Alle vergangenen Spiele löschen?") },
            text = { Text("Diese Aktion löscht alle abgeschlossenen Spiele unwiderruflich aus der Datenbank. Anstehende Spiele bleiben erhalten.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAllCompletedConfirmationDialog = false
                        onDeleteAllCompletedGames()
                    }
                ) {
                    Text("Löschen", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAllCompletedConfirmationDialog = false }
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun SettingsScreenMobilePreview() {
    RefWatchMobileTheme {
        SettingsScreen(
            onNavigateBack = {},
            onDeleteAccountConfirmed = {},
            onDeleteAllCompletedGames = {}
        )
    }
}
