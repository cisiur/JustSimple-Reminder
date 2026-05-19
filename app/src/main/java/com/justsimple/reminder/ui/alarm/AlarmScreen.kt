package com.justsimple.reminder.ui.alarm

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.justsimple.reminder.R
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// Fixed colours for the alarm screen — always dark regardless of app theme.
private val AlarmBg         = Color.Black
private val AlarmOnBg       = Color.White
private val AlarmSubtle     = Color(0xFF9E9E9E)   // muted text (scheduled time)
private val AlarmAccent     = Color(0xFFFFA726)   // warm amber — alarm icon
private val AlarmDismiss    = Color(0xFFE53935)   // red dismiss button
private val AlarmSheetBg    = Color(0xFF1C1C1E)   // dark bottom sheet

private data class SnoozeOption(val labelRes: Int, val minutes: Int)

private val snoozeOptions = listOf(
    SnoozeOption(R.string.snooze_5_min, 5),
    SnoozeOption(R.string.snooze_10_min, 10),
    SnoozeOption(R.string.snooze_15_min, 15),
    SnoozeOption(R.string.snooze_1_hour, 60),
    SnoozeOption(R.string.snooze_3_hours, 180),
    SnoozeOption(R.string.snooze_1_day, 1440),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    onFinish: () -> Unit,
    viewModel: AlarmViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isDismissed) {
        if (uiState.isDismissed) onFinish()
    }

    // Live wall-clock time, updated every second
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(1_000)
        }
    }

    var showSnoozeSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showSnoozeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSnoozeSheet = false },
            sheetState = sheetState,
            containerColor = AlarmSheetBg,
        ) {
            Text(
                text = stringResource(R.string.title_snooze_until),
                color = AlarmOnBg,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
            HorizontalDivider(color = AlarmSubtle.copy(alpha = 0.3f))
            snoozeOptions.forEach { option ->
                TextButton(
                    onClick = {
                        showSnoozeSheet = false
                        viewModel.snooze(option.minutes)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = stringResource(option.labelRes),
                        color = AlarmOnBg,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AlarmBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = AlarmAccent,
            )

            Spacer(Modifier.height(24.dp))

            // Live wall-clock time
            Text(
                text = currentTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                fontSize = 72.sp,
                fontWeight = FontWeight.Light,
                color = AlarmOnBg,
            )

            Spacer(Modifier.height(16.dp))

            if (!uiState.isLoading) {
                Text(
                    text = uiState.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = AlarmOnBg,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.label_scheduled_time, uiState.scheduledTimeLabel),
                    fontSize = 14.sp,
                    color = AlarmSubtle,
                )
            }

            Spacer(Modifier.weight(1f))

            // Snooze
            OutlinedButton(
                onClick = { showSnoozeSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AlarmOnBg,
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, AlarmSubtle),
            ) {
                Text(
                    text = stringResource(R.string.action_snooze),
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                )
            }

            Spacer(Modifier.height(12.dp))

            // Dismiss
            Button(
                onClick = viewModel::dismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlarmDismiss,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = stringResource(R.string.action_dismiss),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
