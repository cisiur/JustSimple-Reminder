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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.justsimple.reminder.R
import com.justsimple.reminder.diagnostics.OemBrand
import com.justsimple.reminder.diagnostics.OemDetector

/** Each step: what the user should do + optional clarifying subtitle + optional deep-link button. */
private data class ReliabilityStep(
    val description: String,
    val subtitle: String? = null,
    val buttonLabel: String? = null,
    val onButtonClick: (() -> Unit)? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReliabilityScreen(
    onBack: () -> Unit,
    onOpenAutostart: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenLockScreenSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    val oemBrand = OemDetector.detect()
    val deviceName = when (oemBrand) {
        OemBrand.XIAOMI  -> "Xiaomi / Redmi / POCO"
        OemBrand.SAMSUNG -> "Samsung"
        OemBrand.ONEPLUS -> "OnePlus"
        OemBrand.HUAWEI  -> "Huawei"
        OemBrand.OPPO    -> "OPPO"
        OemBrand.VIVO    -> "Vivo"
        OemBrand.REALME  -> "Realme"
        OemBrand.OTHER   -> "this device"
    }

    // Build the step list based on OEM
    val steps = buildList<ReliabilityStep> {
        // Autostart — relevant for Xiaomi, Huawei, OPPO, Vivo, Realme, OnePlus
        if (oemBrand in setOf(
                OemBrand.XIAOMI, OemBrand.HUAWEI, OemBrand.OPPO,
                OemBrand.VIVO, OemBrand.REALME, OemBrand.ONEPLUS
            )
        ) {
            add(
                ReliabilityStep(
                    description = stringResource(R.string.reliability_step_autostart),
                    buttonLabel = stringResource(R.string.action_open_autostart),
                    onButtonClick = onOpenAutostart,
                )
            )
        }

        // Battery — relevant for all OEMs
        add(
            ReliabilityStep(
                description = stringResource(R.string.reliability_step_battery),
                buttonLabel = stringResource(R.string.action_open_battery_settings),
                onButtonClick = onOpenBatterySettings,
            )
        )

        // Lock screen display — Xiaomi and Samsung
        if (oemBrand in setOf(OemBrand.XIAOMI, OemBrand.SAMSUNG)) {
            add(
                ReliabilityStep(
                    description = stringResource(R.string.reliability_step_lock_screen),
                    subtitle = stringResource(R.string.reliability_step_lock_screen_subtitle),
                    buttonLabel = stringResource(R.string.action_open_lock_screen_settings),
                    onButtonClick = onOpenLockScreenSettings,
                )
            )
        }

        // Lock screen notifications — Xiaomi and Samsung
        if (oemBrand in setOf(OemBrand.XIAOMI, OemBrand.SAMSUNG)) {
            add(
                ReliabilityStep(
                    description = stringResource(R.string.reliability_step_lock_notifications),
                    subtitle = stringResource(R.string.reliability_step_lock_notifications_subtitle),
                    buttonLabel = stringResource(R.string.action_open_app_settings),
                    onButtonClick = onOpenAppSettings,
                )
            )
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.screen_title_reliability)) },
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
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.reliability_intro, deviceName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
            }

            itemsIndexed(steps) { index, step ->
                ReliabilityStepCard(
                    number = index + 1,
                    description = step.description,
                    subtitle = step.subtitle,
                    buttonLabel = step.buttonLabel,
                    onButtonClick = step.onButtonClick,
                )
                Spacer(Modifier.height(12.dp))
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ── Step card ─────────────────────────────────────────────────────────────────

@Composable
private fun ReliabilityStepCard(
    number: Int,
    description: String,
    subtitle: String?,
    buttonLabel: String?,
    onButtonClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        // Step number badge
        Text(
            text = "$number.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .width(28.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (buttonLabel != null && onButtonClick != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onButtonClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(buttonLabel)
                }
            }
        }
    }
}
