package com.justsimple.reminder.ui.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.justsimple.reminder.R
import com.justsimple.reminder.domain.recurrence.RecurrenceType
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReminderScreen(
    reminderId: Long?,
    onBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    viewModel: AddEditReminderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBack()
    }
    // ── Past date error ───────────────────────────────────────────────────────
    if (uiState.showPastDateError) {
        AlertDialog(
            onDismissRequest = viewModel::dismissPastDateError,
            title = { Text(stringResource(R.string.dialog_past_date_title)) },
            text = { Text(stringResource(R.string.error_date_past)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissPastDateError) {
                    Text(stringResource(R.string.action_ok))
                }
            },
        )
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

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (reminderId != null) R.string.screen_title_edit_reminder
                            else R.string.screen_title_add_reminder,
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        // Box + imePadding so the FAB floats above the keyboard
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text(stringResource(R.string.label_title)) },
                    placeholder = { Text(stringResource(R.string.hint_title_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { viewModel.save() }),
                )

                FormSectionLabel(stringResource(R.string.label_date))
                PickerTriggerCard(
                    label = uiState.scheduledDate.format(
                        DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault()),
                    ),
                    icon = Icons.Default.CalendarMonth,
                    onClick = { showDatePicker = true },
                )

                FormSectionLabel(stringResource(R.string.label_time))
                PickerTriggerCard(
                    label = uiState.scheduledTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    icon = Icons.Default.Schedule,
                    onClick = { showTimePicker = true },
                )

                FormSectionLabel(stringResource(R.string.label_recurrence))
                RecurrenceSelector(
                    selected = uiState.recurrenceType,
                    onSelect = viewModel::onRecurrenceChange,
                )

                Spacer(Modifier.height(88.dp)) // clearance for FAB
            }

            FloatingActionButton(
                onClick = viewModel::save,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(Icons.Default.Check, contentDescription = stringResource(R.string.action_save))
            }
        }
    }

    // ── Date picker dialog ────────────────────────────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.scheduledDate
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.onDateChange(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(),
                        )
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ── Wheel time picker dialog ──────────────────────────────────────────────
    if (showTimePicker) {
        var selectedHour by remember { mutableIntStateOf(uiState.scheduledTime.hour) }
        var selectedMinute by remember { mutableIntStateOf(uiState.scheduledTime.minute) }
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onTimeChange(LocalTime.of(selectedHour, selectedMinute))
                    showTimePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            text = {
                WheelTimePicker(
                    initialHour = selectedHour,
                    initialMinute = selectedMinute,
                    onHourChange = { selectedHour = it },
                    onMinuteChange = { selectedMinute = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )
    }
}

// ── Private helpers ───────────────────────────────────────────────────────────

@Composable
private fun FormSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

@Composable
private fun PickerTriggerCard(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurrenceSelector(
    selected: RecurrenceType,
    onSelect: (RecurrenceType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = RecurrenceType.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, type ->
            SegmentedButton(
                selected = type == selected,
                onClick = { onSelect(type) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(recurrenceChipLabel(type)) },
            )
        }
    }
}

@Composable
private fun recurrenceChipLabel(type: RecurrenceType): String = when (type) {
    RecurrenceType.ONCE -> stringResource(R.string.recurrence_chip_once)
    RecurrenceType.DAILY -> stringResource(R.string.recurrence_chip_daily)
    RecurrenceType.WEEKLY -> stringResource(R.string.recurrence_chip_weekly)
    RecurrenceType.MONTHLY -> stringResource(R.string.recurrence_chip_monthly)
}
