package com.justsimple.reminder.domain.formatter

import android.content.Context
import com.justsimple.reminder.R
import com.justsimple.reminder.data.db.ReminderEntity
import com.justsimple.reminder.domain.recurrence.RecurrenceType
import com.justsimple.reminder.ui.reminders.ReminderUiModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderDisplayFormatter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeRemainingFormatter: TimeRemainingFormatter,
) {
    private val timeInputFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun toUiModel(
        entity: ReminderEntity,
        use24h: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
    ): ReminderUiModel {
        val date = LocalDate.parse(entity.scheduledDate)
        val time = LocalTime.parse(entity.scheduledTime, timeInputFormatter)

        return ReminderUiModel(
            id = entity.id,
            title = entity.title,
            dateLabel = formatDate(date),
            timeLabel = formatTime(time, use24h),
            recurrenceLabel = formatRecurrence(entity.recurrenceType),
            timeRemaining = if (entity.enabled && entity.nextTriggerAt > nowMillis) {
                timeRemainingFormatter.format(entity.nextTriggerAt, nowMillis)
            } else if (!entity.enabled) {
                ""
            } else {
                context.getString(R.string.time_remaining_expired)
            },
            enabled = entity.enabled,
            nextTriggerAt = entity.nextTriggerAt,
            isExpired = entity.recurrenceType == RecurrenceType.ONCE &&
                entity.nextTriggerAt <= nowMillis,
        )
    }

    private fun formatDate(date: LocalDate): String {
        val locale = Locale.getDefault()
        val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
        val day = date.dayOfMonth
        val month = date.month.getDisplayName(TextStyle.SHORT, locale)
        return "$dayName, $day $month"
    }

    private fun formatTime(time: LocalTime, use24h: Boolean): String =
        if (use24h) {
            time.format(DateTimeFormatter.ofPattern("HH:mm"))
        } else {
            time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        }

    private fun formatRecurrence(type: RecurrenceType): String = context.getString(
        when (type) {
            RecurrenceType.ONCE    -> R.string.recurrence_once
            RecurrenceType.DAILY   -> R.string.recurrence_daily
            RecurrenceType.WEEKLY  -> R.string.recurrence_weekly
            RecurrenceType.MONTHLY -> R.string.recurrence_monthly
        }
    )
}
