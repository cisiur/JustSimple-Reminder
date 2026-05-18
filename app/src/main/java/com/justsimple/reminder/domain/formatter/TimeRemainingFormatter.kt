package com.justsimple.reminder.domain.formatter

import android.content.Context
import com.justsimple.reminder.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class TimeRemainingFormatter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun format(triggerMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diffMillis = triggerMillis - nowMillis
        if (diffMillis <= 0) return context.getString(R.string.time_remaining_expired)
        if (diffMillis < 60_000) return context.getString(R.string.time_remaining_now)

        val minutes = diffMillis / 60_000
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7

        return when {
            weeks >= 2 -> context.getString(R.string.time_remaining_weeks, weeks.toInt())
            days >= 2 -> context.getString(R.string.time_remaining_days, days.toInt())
            hours >= 1 -> context.getString(R.string.time_remaining_hours, hours.toInt())
            else -> context.getString(R.string.time_remaining_minutes, minutes.toInt())
        }
    }
}

