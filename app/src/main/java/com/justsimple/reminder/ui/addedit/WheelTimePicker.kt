package com.justsimple.reminder.ui.addedit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val WHEEL_ITEMS = 10_000
private const val VISIBLE = 5
private val ITEM_H = 52.dp
private val WHEEL_H = ITEM_H * VISIBLE

@Composable
fun WheelTimePicker(
    initialHour: Int,
    initialMinute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NumberWheel(
            count = 24,
            initialValue = initialHour,
            onValueChange = onHourChange,
            modifier = Modifier.width(80.dp),
        )
        Text(
            text = ":",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        NumberWheel(
            count = 60,
            initialValue = initialMinute,
            onValueChange = onMinuteChange,
            modifier = Modifier.width(80.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NumberWheel(
    count: Int,
    initialValue: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Center initialValue in the middle of the item pool
    val midStart = WHEEL_ITEMS / 2 - (WHEEL_ITEMS / 2 % count) + initialValue
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = midStart - VISIBLE / 2,
    )
    val snapBehavior = rememberSnapFlingBehavior(listState)

    // Index of the item currently occupying the center row
    val selectedIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex + VISIBLE / 2 }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            onValueChange(selectedIndex % count)
        }
    }

    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(modifier = modifier.height(WHEEL_H)) {
        // Selection highlight (rendered first so it's behind the text)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(ITEM_H)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(10.dp),
                ),
        )

        LazyColumn(
            state = listState,
            flingBehavior = snapBehavior,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(WHEEL_ITEMS) { index ->
                val value = index % count
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ITEM_H),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = value.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                    )
                }
            }
        }

        // Top fade — masks items scrolling out of view
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ITEM_H * (VISIBLE / 2))
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(listOf(surfaceColor, surfaceColor.copy(alpha = 0f))),
                ),
        )
        // Bottom fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ITEM_H * (VISIBLE / 2))
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(surfaceColor.copy(alpha = 0f), surfaceColor)),
                ),
        )
    }
}
