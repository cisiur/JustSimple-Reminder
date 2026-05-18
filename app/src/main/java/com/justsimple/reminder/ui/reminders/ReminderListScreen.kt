package com.justsimple.reminder.ui.reminders

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.TextButton
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.justsimple.reminder.R
import com.justsimple.reminder.ads.AdBannerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAlarmSettings: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    viewModel: ReminderListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh warning banners every time the screen comes back to foreground
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshWarnings()
        }
    }

    if (uiState.showFreeTierDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissFreeTierDialog,
            title = { Text(stringResource(R.string.dialog_free_limit_title)) },
            text = { Text(stringResource(R.string.dialog_free_limit_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissFreeTierDialog()
                    onNavigateToPaywall()
                }) { Text(stringResource(R.string.action_upgrade)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissFreeTierDialog) {
                    Text(stringResource(R.string.action_not_now))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.screen_title_reminders)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.screen_title_settings),
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_reminder))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Warning banners ───────────────────────────────────────────────
            if (uiState.showExactAlarmWarning) {
                WarningBanner(
                    text = stringResource(R.string.warning_exact_alarm),
                    onClick = onOpenAlarmSettings,
                )
            }
            if (uiState.showBatteryWarning) {
                WarningBanner(
                    text = stringResource(R.string.warning_battery_optimization),
                    onClick = onOpenBatterySettings,
                )
            }

            // ── Content ───────────────────────────────────────────────────────
            if (uiState.reminders.isEmpty()) {
                EmptyState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item { Spacer(Modifier.height(4.dp)) }

                    items(
                        items = uiState.reminders,
                        key = { it.id },
                    ) { reminder ->
                        SwipeToDeleteReminderItem(
                            reminder = reminder,
                            onEdit = { onEditClick(reminder.id) },
                            onToggle = { viewModel.setEnabled(reminder.id, it) },
                            onDelete = { viewModel.delete(reminder.id) },
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )

                    }

                    item { Spacer(Modifier.height(80.dp)) } // FAB clearance
                }
            }

            // ── AdMob banner (free tier only) ─────────────────────────────────
            if (!uiState.isPremium) {
                AdBannerView()
            }
        }
    }
}

// ── Warning banner ────────────────────────────────────────────────────────────

@Composable
private fun WarningBanner(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

// ── Swipe-to-delete wrapper ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteReminderItem(
    reminder: ReminderUiModel,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        },
        positionalThreshold = { it * 0.4f },
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.errorContainer,
                label = "swipe_bg",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, shape = MaterialTheme.shapes.medium)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = Color.White,
                )
            }
        },
    ) {
        ReminderCard(
            reminder = reminder,
            onEdit = onEdit,
            onToggle = onToggle,
            onDelete = onDelete,
        )
    }
}

// ── Reminder card ─────────────────────────────────────────────────────────────

@Composable
private fun ReminderCard(
    reminder: ReminderUiModel,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${reminder.dateLabel} · ${reminder.timeLabel} · ${reminder.recurrenceLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (reminder.timeRemaining.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = reminder.timeRemaining,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_delete_reminder),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = reminder.enabled,
                onCheckedChange = onToggle,
                enabled = !reminder.isExpired,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Alarm,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_state_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.empty_state_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}
