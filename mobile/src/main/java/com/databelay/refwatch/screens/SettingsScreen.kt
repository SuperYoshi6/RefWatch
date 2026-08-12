package com.databelay.refwatch.screens

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextFieldColors
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Add
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.databelay.refwatch.R
import com.databelay.refwatch.auth.AuthViewModel
import com.databelay.refwatch.common.LegalLinks
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.databelay.refwatch.common.getAppVersionName
import com.databelay.refwatch.common.theme.AccentGreen
import com.databelay.refwatch.common.theme.Border
import com.databelay.refwatch.common.theme.PitchBackground
import com.databelay.refwatch.common.theme.Surface
import com.databelay.refwatch.common.theme.Surface2
import com.databelay.refwatch.common.theme.TextMuted
import com.databelay.refwatch.common.theme.TextPrimary
import com.databelay.refwatch.common.theme.RefWatchMobileTheme
import com.databelay.refwatch.data.PreferencesManager
import com.databelay.refwatch.data.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStatistics: () -> Unit = {},
    onDeleteAccountConfirmed: () -> Unit,
    onDeleteAllCompletedGames: () -> Unit = {},
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val authMessage by authViewModel.authMessage.collectAsState()
    val authError by authViewModel.authError.collectAsState()
    
    val pairingMessage by settingsViewModel.pairingMessage.collectAsState()
    val isPairingLoading by settingsViewModel.isPairingLoading.collectAsState()
    
    var isDfbNetEnabled by remember { mutableStateOf(settingsViewModel.isDfbNetEnabled) }
    var pairingCode by remember { mutableStateOf("") }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showDeleteAllCompletedConfirmationDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showEmailChangeDialog by remember { mutableStateOf(false) }
    var appVersionName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        appVersionName = getAppVersionName(context)
        authViewModel.reloadUser() // Refresh verification status on entry
    }

    LaunchedEffect(authMessage) {
        authMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            authViewModel.clearAuthError() // Reusing this to clear message as well if needed
        }
    }

    LaunchedEffect(authError) {
        authError?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(pairingMessage) {
        pairingMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            settingsViewModel.clearPairingMessage()
            if (it.contains("erfolgreich", ignoreCase = true)) {
                pairingCode = ""
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        }
    ) { paddingValues ->
        PitchBackground(modifier = Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(Modifier.height(8.dp))

                // Hero
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = stringResource(R.string.settings_hero_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_hero_subtitle),
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(Modifier.height(8.dp))

                SectionHeader(stringResource(R.string.settings_section_account), Icons.Filled.Person)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                ) {
                    Column {
                        val isVerified = currentUser?.isEmailVerified == true
                        val isPasswordUser = currentUser?.providerData?.any { it.providerId == "password" } == true
                        
                        SettingsRow(
                            icon = if (isVerified) Icons.Filled.MarkEmailRead else Icons.Filled.MarkEmailUnread,
                            title = currentUser?.email ?: "Account",
                            supportingText = if (isVerified) stringResource(R.string.email_verified) else stringResource(R.string.email_unverified),
                            onClick = { 
                                if (isPasswordUser) {
                                    showEmailChangeDialog = true
                                }
                            }
                        )
                        if (!isVerified && currentUser?.email != null) {
                            HorizontalDivider(color = Border)
                            SettingsRow(
                                icon = Icons.Filled.MarkEmailUnread,
                                title = stringResource(R.string.verify_now),
                                onClick = { authViewModel.sendEmailVerification() }
                            )
                        }
                        
                        val linkedDevices by settingsViewModel.linkedDevices.collectAsState()
                        if (linkedDevices.isNotEmpty()) {
                            HorizontalDivider(color = Border, modifier = Modifier.padding(vertical = 12.dp))
                            Text(
                                "Verknüpfte Geräte",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            linkedDevices.forEach { device ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Watch ID: ${device.id.take(8)}...", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                        Text(
                                            "Verknüpft am: ${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(device.linkedAt))}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                    IconButton(onClick = { settingsViewModel.removeDevice(device.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Entfernen", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                SectionHeader(stringResource(R.string.settings_section_project), Icons.Filled.SportsSoccer)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Filled.BarChart,
                            title = stringResource(R.string.view_statistics),
                            onClick = onNavigateToStatistics
                        )
                        HorizontalDivider(color = Border)
                        SettingsRow(
                            icon = Icons.Filled.Language,
                            title = stringResource(R.string.language),
                            supportingText = when (AppCompatDelegate.getApplicationLocales()[0]?.language) {
                                "de" -> stringResource(R.string.language_german)
                                else -> stringResource(R.string.language_english)
                            },
                            onClick = { showLanguageDialog = true }
                        )
                        HorizontalDivider(color = Border)
                        SwitchRow(
                            icon = Icons.Filled.Storage, // Or a better icon
                            title = stringResource(R.string.enable_dfbnet),
                            supportingText = stringResource(R.string.enable_dfbnet_desc),
                            checked = isDfbNetEnabled,
                            onCheckedChange = {
                                isDfbNetEnabled = it
                                settingsViewModel.isDfbNetEnabled = it
                            }
                        )
                        HorizontalDivider(color = Border)
                        ExternalLinkRow(
                            icon = Icons.Filled.Language,
                            title = stringResource(R.string.website),
                            onClick = {
                                val lang = AppCompatDelegate.getApplicationLocales()[0]?.language ?: "en"
                                openUrl(context, LegalLinks.getLocalizedWebsiteUrl(lang), "Website")
                            }
                        )
                        HorizontalDivider(color = Border)
                        ExternalLinkRow(
                            icon = Icons.Default.Code,
                            title = stringResource(R.string.web_manager),
                            supportingText = stringResource(R.string.web_manager_desc),
                            onClick = {
                                openUrl(context, LegalLinks.WEB_MANAGER_URL, "Web Manager")
                            }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Developer cards now stack vertically to ensure handles like
                // "SuperYoshi6 / RefWatch" have enough room on narrow screens.
                DeveloperCardsRow(
                    onDeveloperClick = { openUrl(context, LegalLinks.GITHUB_URL, "GitHub Repository") },
                    onOriginalClick = { openUrl(context, LegalLinks.GITHUB_ORIGINAL_URL, "Original Project") }
                )

                Spacer(Modifier.height(24.dp))

                SectionHeader(stringResource(R.string.settings_section_legal), Icons.Filled.Policy)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                ) {
                    Column {
                        ExternalLinkRow(
                            icon = Icons.Filled.Shield,
                            title = stringResource(R.string.auth_privacy_policy),
                            onClick = {
                                val lang = AppCompatDelegate.getApplicationLocales()[0]?.language ?: "en"
                                openUrl(context, LegalLinks.getLocalizedPrivacyUrl(lang), "Datenschutzerklärung")
                            }
                        )
                        HorizontalDivider(color = Border)
                        ExternalLinkRow(
                            icon = Icons.Filled.Description,
                            title = stringResource(R.string.auth_terms_of_service),
                            onClick = {
                                val lang = AppCompatDelegate.getApplicationLocales()[0]?.language ?: "en"
                                openUrl(context, LegalLinks.getLocalizedTermsUrl(lang), "Nutzungsbedingungen")
                            }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                SectionHeader(stringResource(R.string.settings_section_data), Icons.Filled.Storage)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                ) {
                    Column {
                        DestructiveActionRow(
                            icon = Icons.Filled.Delete,
                            title = stringResource(R.string.delete_all_past_games),
                            onClick = { showDeleteAllCompletedConfirmationDialog = true }
                        )
                        HorizontalDivider(color = Border)
                        DestructiveActionRow(
                            icon = Icons.Filled.DeleteForever,
                            title = stringResource(R.string.delete),
                            contentColor = MaterialTheme.colorScheme.error,
                            onClick = { showDeleteConfirmationDialog = true }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                val versionLabel = stringResource(R.string.version_label, appVersionName ?: stringResource(R.string.loading_dots))
                Text(
                    text = versionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 24.dp)
                        .align(Alignment.CenterHorizontally),
                    color = TextMuted
                )
            }
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            onDismissRequest = { showLanguageDialog = false },
            onLanguageSelected = { languageCode ->
                val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
                AppCompatDelegate.setApplicationLocales(appLocale)
                showLanguageDialog = false
            }
        )
    }

    if (showEmailChangeDialog) {
        var newEmail by remember { mutableStateOf(currentUser?.email ?: "") }
        var password by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEmailChangeDialog = false },
            title = { Text(stringResource(R.string.change_email)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.reauth_required), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text(stringResource(R.string.new_email_address)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.reauth_password_label)) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = null,
                                    tint = TextMuted
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (password.isNotBlank() && newEmail.isNotBlank()) {
                            authViewModel.reauthenticateAndChangeEmail(password, newEmail)
                            showEmailChangeDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmailChangeDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text(stringResource(R.string.delete_account_title)) },
            text = { Text(stringResource(R.string.delete_account_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmationDialog = false
                        onDeleteAccountConfirmed()
                        Log.d("SettingsScreen", "Account deletion confirmed by user.")
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDeleteAllCompletedConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllCompletedConfirmationDialog = false },
            title = { Text(stringResource(R.string.delete_all_past_games_title)) },
            text = { Text(stringResource(R.string.delete_all_past_games_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAllCompletedConfirmationDialog = false
                        onDeleteAllCompletedGames()
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllCompletedConfirmationDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AccentGreen,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = AccentGreen,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ProjectCard(
    onWebsiteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = stringResource(R.string.logo_refwatch),
                    modifier = Modifier.size(72.dp)
                )
            }
            HorizontalDivider(color = Border)
            ExternalLinkRow(
                icon = Icons.Filled.Language,
                title = "Website",
                onClick = onWebsiteClick
            )
        }
    }
}

/**
 * Developer cards now stack vertically on all devices to ensure that long
 * handles (like "SuperYoshi6 / RefWatch") have ample room and don't clip.
 */
@Composable
private fun DeveloperCardsRow(
    onDeveloperClick: () -> Unit,
    onOriginalClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DeveloperCard(
            roleLabel = stringResource(R.string.developer_role_label),
            handle = stringResource(R.string.developer_super_yoshi_handle),
            onClick = onDeveloperClick
        )
        DeveloperCard(
            roleLabel = stringResource(R.string.developer_original_role_label),
            handle = stringResource(R.string.developer_githubbar_handle),
            onClick = onOriginalClick
        )
    }
}

@Composable
private fun DeveloperCard(
    roleLabel: String,
    handle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colored circular badge to echo the gradient circles used on the
            // website (the small accent in the gh-row block).
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AccentGreen.copy(alpha = 0.18f))
                    .border(
                        width = 1.dp,
                        color = AccentGreen.copy(alpha = 0.35f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Code,
                    contentDescription = stringResource(R.string.developer_avatar_cd),
                    tint = AccentGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = roleLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = handle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun LanguageSelectionDialog(
    onDismissRequest: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val currentLanguage = AppCompatDelegate.getApplicationLocales()[0]?.language ?: "en"
    val languages = listOf(
        "en" to stringResource(R.string.language_english),
        "de" to stringResource(R.string.language_german)
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.select_language)) },
        text = {
            Column(Modifier.selectableGroup()) {
                languages.forEach { (code, name) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (code == currentLanguage),
                                onClick = { onLanguageSelected(code) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (code == currentLanguage),
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AccentGreen,
                                unselectedColor = TextMuted
                            )
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp),
                            color = TextPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    supportingText: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        modifier = Modifier.clickable { onCheckedChange(!checked) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentGreen
            )
        },
        headlineContent = { Text(title, color = TextPrimary) },
        supportingContent = supportingText?.let { { Text(it, color = TextMuted) } },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextPrimary,
                    checkedTrackColor = AccentGreen,
                    checkedBorderColor = AccentGreen,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = Surface,
                    uncheckedBorderColor = Border
                )
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    supportingText: String? = null,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentGreen
            )
        },
        headlineContent = { Text(title, color = TextPrimary) },
        supportingContent = supportingText?.let { { Text(it, color = TextMuted) } },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun DestructiveActionRow(
    icon: ImageVector,
    title: String,
    contentColor: Color = TextPrimary,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (contentColor == TextPrimary) AccentGreen else contentColor
            )
        },
        headlineContent = { Text(title, color = contentColor) },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun ExternalLinkRow(
    icon: ImageVector,
    title: String,
    supportingText: String? = null,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentGreen
            )
        },
        headlineContent = { Text(title, color = TextPrimary) },
        supportingContent = supportingText?.let { { Text(it, color = TextMuted) } },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = TextMuted
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

private fun openUrl(context: Context, url: String, label: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("SettingsScreen", "Failed to open $label URL: $url", e)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    focusedContainerColor = Surface, unfocusedContainerColor = Surface,
    focusedBorderColor = AccentGreen, unfocusedBorderColor = Border,
    focusedLabelColor = AccentGreen, unfocusedLabelColor = TextMuted,
    focusedPlaceholderColor = TextMuted, unfocusedPlaceholderColor = TextMuted,
    cursorColor = AccentGreen, disabledTextColor = TextMuted,
    disabledContainerColor = Surface, disabledBorderColor = Border,
    disabledLabelColor = TextMuted, disabledPlaceholderColor = TextMuted
)
