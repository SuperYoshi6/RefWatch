package com.databelay.refwatch.screens

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.Image
import androidx.compose.ui.tooling.preview.Preview
import com.databelay.refwatch.BuildConfig
import com.databelay.refwatch.R
import com.databelay.refwatch.common.LegalLinks
import com.databelay.refwatch.common.getAppVersionCode
import com.databelay.refwatch.common.getAppVersionName
import com.databelay.refwatch.common.theme.RefWatchMobileTheme

/**
 * Überarbeiteter SettingsScreen mit besserer Struktur und Performance
 * - LazyColumn statt Column für besseres Scrolling
 * - Strukturierte Sektionen
 * - Wiederverwendbare UI-Komponenten
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenNew(
    onNavigateBack: () -> Unit,
    onDeleteAccountConfirmed: () -> Unit,
    onDeleteAllCompletedGames: () -> Unit = {}
) {
    val context = LocalContext.current
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showDeleteAllCompletedConfirmationDialog by remember { mutableStateOf(false) }
    var appVersionName by remember { mutableStateOf("Loading...") }
    var appVersionNumber by remember { mutableLongStateOf(0L) }
    val buildDateString = BuildConfig.BUILD_TIME

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ============== INFORMATIONEN SEKTION ==============
            item {
                SettingsSectionHeader("Informationen")
            }

            item {
                SettingsInfoCard(
                    title = "Version",
                    subtitle = "$appVersionName (Build $appVersionNumber)",
                    icon = Icons.Filled.Info
                )
            }

            item {
                SettingsInfoCard(
                    title = "Build Datum",
                    subtitle = buildDateString,
                    icon = Icons.Filled.Info
                )
            }

            item { Divider(modifier = Modifier.padding(horizontal = 16.dp)) }

            // ============== LINKS SEKTION ==============
            item {
                SettingsSectionHeader("Links & Dokumentation")
            }

            item {
                SettingsLinkItem(
                    title = "Datenschutzerklärung",
                    icon = Icons.Filled.Link,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(LegalLinks.PRIVACY_POLICY_URL))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("SettingsScreen", "Failed to open Privacy Policy", e)
                        }
                    }
                )
            }

            item {
                SettingsLinkItem(
                    title = "Nutzungsbedingungen",
                    icon = Icons.Filled.Link,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(LegalLinks.TERMS_OF_USE_URL))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("SettingsScreen", "Failed to open Terms of Use", e)
                        }
                    }
                )
            }

            item {
                SettingsLinkItem(
                    title = "Website",
                    icon = Icons.Filled.Link,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(LegalLinks.WEBSITE_URL))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("SettingsScreen", "Failed to open Website", e)
                        }
                    }
                )
            }

            item {
                SettingsLinkItem(
                    title = "GitHub Repository",
                    icon = Icons.Filled.Link,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(LegalLinks.GITHUB_URL))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("SettingsScreen", "Failed to open GitHub", e)
                        }
                    }
                )
            }

            item { Divider(modifier = Modifier.padding(horizontal = 16.dp)) }

            // ============== APP LOGO SEKTION ==============
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "RefWatch Logo",
                        modifier = Modifier
                            .height(80.dp)
                    )
                }
            }

            item { Divider(modifier = Modifier.padding(horizontal = 16.dp)) }

            // ============== DATENVERWALTUNG SEKTION ==============
            item {
                SettingsSectionHeader("Datenverwaltung")
            }

            item {
                SettingsDangerButton(
                    title = "Alle vergangenen Spiele löschen",
                    icon = Icons.Filled.Delete,
                    onClick = { showDeleteAllCompletedConfirmationDialog = true }
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                SettingsDangerButton(
                    title = "Account löschen",
                    icon = Icons.Filled.Delete,
                    onClick = { showDeleteConfirmationDialog = true }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    // Delete Confirmation Dialogs
    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text("Account wirklich löschen?") },
            text = { 
                Text(
                    "Diese Aktion ist permanent und kann nicht rückgängig gemacht werden. " +
                    "Alle deine Daten werden gelöscht.",
                    style = MaterialTheme.typography.bodyMedium
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmationDialog = false
                        onDeleteAccountConfirmed()
                        Log.d("SettingsScreen", "Account deletion confirmed")
                    }
                ) {
                    Text("Löschen", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    if (showDeleteAllCompletedConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllCompletedConfirmationDialog = false },
            title = { Text("Alle vergangenen Spiele löschen?") },
            text = { 
                Text(
                    "Diese Aktion löscht alle abgeschlossenen Spiele unwiderruflich. " +
                    "Anstehende Spiele bleiben erhalten.",
                    style = MaterialTheme.typography.bodyMedium
                ) 
            },
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
                TextButton(onClick = { showDeleteAllCompletedConfirmationDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

// ============== UI KOMPONENTEN ==============

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    )
}

@Composable
private fun SettingsInfoCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.material.icons.Icons? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(8.dp)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SettingsLinkItem(
    title: String,
    icon: androidx.compose.material.icons.Icons? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            modifier = Modifier.padding(8.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsDangerButton(
    title: String,
    icon: androidx.compose.material.icons.Icons? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.error
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Text(title)
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun SettingsScreenNewPreview() {
    RefWatchMobileTheme {
        SettingsScreenNew(
            onNavigateBack = {},
            onDeleteAccountConfirmed = {},
            onDeleteAllCompletedGames = {}
        )
    }
}
