package com.justsimple.reminder.domain.recurrence

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Stateless helpers for computing next occurrence epoch millis.
 * All functions are pure — no side effects, no Android dependencies.
 *
 * The [zone] parameter defaults to the system default but is injectable
 * so unit tests can pin it to UTC and get deterministic results.
 */
object NextTriggerCalculator {

    /**
     * ONCE: returns the epoch millis of [date]+[time] if it is strictly
     * in the future, or null if the moment has already passed.
     */
    fun once(
        date: LocalDate,
        time: LocalTime,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long? {
        val triggerMillis = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
        return if (triggerMillis > nowMillis) triggerMillis else null
    }

    /**
     * DAILY: returns the next future occurrence of [time].
     * Tries today first; if today's slot has already passed, returns tomorrow.
     */
    fun daily(
        time: LocalTime,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val nowDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val todaySlot = nowDate.atTime(time).atZone(zone).toInstant().toEpochMilli()
        return if (todaySlot > nowMillis) {
            todaySlot
        } else {
            nowDate.plusDays(1).atTime(time).atZone(zone).toInstant().toEpochMilli()
        }
    }

    /**
     * WEEKLY: returns the next future occurrence of [dayOfWeek]+[time].
     *
     * Algorithm:
     *  1. Find the nearest date that is [dayOfWeek] (today included).
     *  2. If that date+time is still in the future → use it.
     *  3. Otherwise the slot is today but already elapsed → jump to next week.
     */
    fun weekly(
        dayOfWeek: DayOfWeek,
        time: LocalTime,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val nowDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val nearestSameDay = nowDate.with(TemporalAdjusters.nextOrSame(dayOfWeek))
        val candidate = nearestSameDay.atTime(time).atZone(zone).toInstant().toEpochMilli()
        return if (candidate > nowMillis) {
            candidate
        } else {
            // Same weekday but time already passed → next week
            nowDate.with(TemporalAdjusters.next(dayOfWeek))
                .atTime(time)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
        }
    }

    /**
     * MONTHLY: returns the next future occurrence of [dayOfMonth]+[time].
     *
     * If [dayOfMonth] exceeds the length of the target month (e.g. day 31
     * in a 30-day month), it is clamped to the last day of that month.
     *
     * Algorithm:
     *  1. Try the slot in the current month.
     *  2. If it is still in the future → use it.
     *  3. Otherwise advance to next month and repeat the clamp.
     */
    fun monthly(
        dayOfMonth: Int,
        time: LocalTime,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val nowDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()

        val thisMonthSlot = nowDate
            .withDayOfMonth(dayOfMonth.coerceAtMost(nowDate.lengthOfMonth()))
            .atTime(time)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        if (thisMonthSlot > nowMillis) return thisMonthSlot

        val nextMonth = nowDate.plusMonths(1)
        return nextMonth
            .withDayOfMonth(dayOfMonth.coerceAtMost(nextMonth.lengthOfMonth()))
            .atTime(time)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }
}
