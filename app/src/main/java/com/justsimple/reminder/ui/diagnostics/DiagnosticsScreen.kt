package com.justsimple.reminder.ui.diagnostics

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.justsimple.reminder.R
import com.justsimple.reminder.diagnostics.OemBrand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onBack: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAlarmSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onReliabilityClick: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.screen_title_diagnostics)) },
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
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            if (uiState.isLoading) return@LazyColumn

            // ── Permission checks ─────────────────────────────────────────
            item {
                SectionLabel(stringResource(R.string.settings_section_permissions))
            }
            item {
                DiagCheckRow(
                    label = stringResource(R.string.diag_notifications),
                    ok = uiState.notificationsGranted,
                    fixLabel = stringResource(R.string.action_open_app_settings),
                    onFix = onOpenNotificationSettings,
                )
            }
            item {
                DiagCheckRow(
                    label = stringResource(R.string.diag_exact_alarms),
                    ok = uiState.exactAlarmsGranted,
                    fixLabel = stringResource(R.string.action_open_app_settings),
                    onFix = onOpenAlarmSettings,
                )
            }
            item {
                DiagCheckRow(
                    label = stringResource(R.string.diag_battery_optimization),
                    ok = uiState.batteryOptimizationOff,
                    fixLabel = stringResource(R.string.action_open_battery_settings),
                    onFix = onOpenBatterySettings,
                )
            }
            if (uiState.showFullScreenCheck) {
                item {
                    DiagCheckRow(
                        label = stringResource(R.string.diag_fullscreen_intent),
                        ok = uiState.fullScreenIntentGranted,
                        fixLabel = stringResource(R.string.action_open_app_settings),
                        onFix = onOpenAppSettings,
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
            item { HorizontalDivider() }
            item { Spacer(Modifier.height(8.dp)) }

            // ── Device info ───────────────────────────────────────────────
            item {
                SectionLabel(stringResource(R.string.diag_device_info))
            }
            item {
                InfoLine(stringResource(R.string.diag_manufacturer, uiState.manufacturer))
            }
            item {
                InfoLine(stringResource(R.string.diag_model, uiState.model))
            }
            item {
                InfoLine(
                    stringResource(R.string.diag_android_version, uiState.androidVersion, uiState.sdkInt)
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
            item { HorizontalDivider() }
            item { Spacer(Modifier.height(8.dp)) }

            // ── Next scheduled alarm ──────────────────────────────────────
            item {
                SectionLabel(stringResource(R.string.diag_next_scheduled, uiState.nextAlarmText))
            }

            // ── OEM reliability guide link ────────────────────────────────
            if (uiState.oemBrand != OemBrand.OTHER) {
                item { Spacer(Modifier.height(16.dp)) }
                item { HorizontalDivider() }
                item { Spacer(Modifier.height(16.dp)) }
                item {
                    Button(
                        onClick = onReliabilityClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.screen_title_reliability))
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ── Subcomponents ─────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 10.dp),
    )
}

@Composable
private fun InfoLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun DiagCheckRow(
    label: String,
    ok: Boolean,
    fixLabel: String,
    onFix: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (ok) stringResource(R.string.diag_status_ok)
                       else stringResource(R.string.diag_status_needs_action),
                style = MaterialTheme.typography.bodySmall,
                color = if (ok) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium,
            )
        }
        if (!ok) {
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onFix) {
                Text(fixLabel, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
