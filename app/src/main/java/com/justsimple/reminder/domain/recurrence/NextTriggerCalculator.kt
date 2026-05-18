package com.justsimple.reminder.domain.recurrence

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Stateless helpers for computing next occurrence epoch millis.
 * All functions are pure (no side effects, no Android deps).
 */
object NextTriggerCalculator {

    fun once(date: LocalDate, time: LocalTime, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long? {
        // TODO Module 3
        return null
    }

    fun daily(time: LocalTime, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        // TODO Module 3
        return 0L
    }

    fun weekly(dayOfWeek: java.time.DayOfWeek, time: LocalTime, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        // TODO Module 3
        return 0L
    }

    fun monthly(dayOfMonth: Int, time: LocalTime, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        // TODO Module 3
        return 0L
    }
}

