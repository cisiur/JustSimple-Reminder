package com.justsimple.reminder.ui.settings

import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.justsimple.reminder.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onDiagnosticsClick: () -> Unit,
    onPaywallClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val ctx = LocalContext.current

    // Language dialog — shown only on pre-API 33 (on 33+ we open system settings directly)
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }

    if (showLanguageDialog) {
        LanguagePickerDialog(
            onDismiss = { showLanguageDialog = false },
            onSelect = { tag ->
                showLanguageDialog = false
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(tag)
                )
            },
        )
    }

    // Restore-purchases result snackbar
    val restoreSuccessMsg = stringResource(R.string.settings_restore_success)
    val restoreFailureMsg = stringResource(R.string.settings_restore_failure)
    LaunchedEffect(uiState.showRestoreSuccess, uiState.showRestoreFailure) {
        when {
            uiState.showRestoreSuccess -> {
                snackbarHost.showSnackbar(restoreSuccessMsg)
                viewModel.dismissRestoreResult()
            }
            uiState.showRestoreFailure -> {
                snackbarHost.showSnackbar(restoreFailureMsg)
                viewModel.dismissRestoreResult()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.screen_title_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {

            // ── Language ──────────────────────────────────────────────────
            item { SectionHeader(stringResource(R.string.settings_section_language)) }
            item {
                ArrowRow(
                    label = stringResource(R.string.settings_language_label),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            // API 33+: open system per-app language picker
                            ContextCompat.startActivity(
                                ctx,
                                android.content.Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                                    data = android.net.Uri.fromParts("package", ctx.packageName, null)
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                },
                                null
                            )
                        } else {
                            showLanguageDialog = true
                        }
                    },
                )
            }
            item { SectionDivider() }

            // ── Time Format ───────────────────────────────────────────────
            item { SectionHeader(stringResource(R.string.settings_section_time_format)) }
            item {
                SwitchRow(
                    label = stringResource(R.string.settings_time_format_24h),
                    checked = uiState.use24hFormat,
                    onCheckedChange = viewModel::setUse24hFormat,
                )
            }
            item { SectionDivider() }

            // ── Subscription ──────────────────────────────────────────────
            item { SectionHeader(stringResource(R.string.settings_section_subscription)) }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = if (uiState.isPremium)
                            stringResource(R.string.settings_subscription_premium)
                        else
                            stringResource(R.string.settings_subscription_free),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (uiState.isPremium)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(12.dp))
                    if (!uiState.isPremium) {
                        OutlinedButton(
                            onClick = onPaywallClick,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.settings_action_upgrade))
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    TextButton(
                        onClick = viewModel::restorePurchases,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_action_restore))
                    }
                }
            }
            item { SectionDivider() }

            // ── Alarm Reliability ─────────────────────────────────────────
            item { SectionHeader(stringResource(R.string.settings_section_reliability)) }
            item {
                ArrowRow(
                    label = stringResource(R.string.settings_action_diagnostics),
                    subtitle = stringResource(R.string.settings_action_diagnostics_subtitle),
                    onClick = onDiagnosticsClick,
                )
            }
            item { SectionDivider() }

            // ── About ─────────────────────────────────────────────────────
            item { SectionHeader(stringResource(R.string.settings_section_about)) }
            item {
                ArrowRow(
                    label = stringResource(R.string.settings_privacy_policy),
                    onClick = onPrivacyPolicyClick,
                )
            }
            if (uiState.appVersion.isNotEmpty()) {
                item {
                    InfoRow(stringResource(R.string.settings_version, uiState.appVersion))
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ── Language picker (pre-API 33) ──────────────────────────────────────────────

private data class LangOption(val tag: String, val displayName: String)

private val LANGUAGES = listOf(
    LangOption("en", "English"),
    LangOption("pl", "Polski"),
)

@Composable
private fun LanguagePickerDialog(
    onDismiss: () -> Unit,
    onSelect: (tag: String) -> Unit,
) {
    // Determine currently active locale via AppCompat
    val currentTag = AppCompatDelegate.getApplicationLocales()
        .toLanguageTags()
        .takeIf { it.isNotBlank() }
        ?: java.util.Locale.getDefault().language

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_language_title)) },
        text = {
            Column {
                LANGUAGES.forEach { lang ->
                    val selected = currentTag.startsWith(lang.tag)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(lang.tag) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { onSelect(lang.tag) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(lang.displayName, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

// ── MIUI action row (no status, always opens settings) ───────────────────────

@Composable
private fun MiuiActionRow(label: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ── Section components ────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}


@Composable
private fun ArrowRow(label: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun InfoRow(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
    )
}
