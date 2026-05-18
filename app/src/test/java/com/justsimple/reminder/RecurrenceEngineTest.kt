package com.justsimple.reminder

import com.justsimple.reminder.data.db.ReminderEntity
import com.justsimple.reminder.domain.recurrence.RecurrenceEngine
import com.justsimple.reminder.domain.recurrence.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * All tests pin [zone] to UTC so results are identical on every machine.
 *
 * Fixed "now" reference point used throughout:
 *   2025-07-14  Monday  10:00 UTC  → epoch = 1_752_490_800_000
 */
class RecurrenceEngineTest {

    private lateinit var engine: RecurrenceEngine
    private val UTC = ZoneOffset.UTC

    // Monday 2025-07-14 10:00:00 UTC
    private val NOW = utcMillis(2025, 7, 14, 10, 0)

    @Before
    fun setUp() {
        engine = RecurrenceEngine()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun utcMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute)
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()

    private fun reminder(
        date: LocalDate,
        time: LocalTime,
        recurrence: RecurrenceType,
        snoozeUntil: Long? = null,
    ) = ReminderEntity(
        id = 1L,
        title = "Test",
        enabled = true,
        scheduledDate = date.toString(),                        // "YYYY-MM-DD"
        scheduledTime = "%02d:%02d".format(time.hour, time.minute), // "HH:mm"
        recurrenceType = recurrence,
        nextTriggerAt = 0L,
        lastTriggeredAt = null,
        snoozeUntil = snoozeUntil,
        createdAt = 0L,
        updatedAt = 0L,
    )

    // ── ONCE ─────────────────────────────────────────────────────────────────

    @Test
    fun `ONCE - future date returns scheduled epoch millis`() {
        // 2025-07-15 09:00 UTC is in the future relative to NOW (2025-07-14 10:00)
        val r = reminder(LocalDate.of(2025, 7, 15), LocalTime.of(9, 0), RecurrenceType.ONCE)
        val expected = utcMillis(2025, 7, 15, 9, 0)
        assertEquals(expected, engine.nextTriggerMillis(r, NOW, UTC))
    }

    @Test
    fun `ONCE - same day but later time returns that time`() {
        // 2025-07-14 11:00 UTC is 1 hour after NOW
        val r = reminder(LocalDate.of(2025, 7, 14), LocalTime.of(11, 0), RecurrenceType.ONCE)
        val expected = utcMillis(2025, 7, 14, 11, 0)
        assertEquals(expected, engine.nextTriggerMillis(r, NOW, UTC))
    }

    @Test
    fun `ONCE - past date returns null`() {
        // 2025-07-13 09:00 UTC is yesterday — expired
        val r = reminder(LocalDate.of(2025, 7, 13), LocalTime.of(9, 0), RecurrenceType.ONCE)
        assertNull(engine.nextTriggerMillis(r, NOW, UTC))
    }

    @Test
    fun `ONCE - same day but earlier time returns null`() {
        // 2025-07-14 09:00 UTC is 1 hour before NOW
        val r = reminder(LocalDate.of(2025, 7, 14), LocalTime.of(9, 0), RecurrenceType.ONCE)
        assertNull(engine.nextTriggerMillis(r, NOW, UTC))
    }

    @Test
    fun `ONCE - isExpired returns true when past`() {
        val r = reminder(LocalDate.of(2025, 7, 13), LocalTime.of(9, 0), RecurrenceType.ONCE)
        assertEquals(true, engine.isExpired(r, NOW))
    }

    @Test
    fun `ONCE - isExpired returns false for recurring types even if date is past`() {
        val r = reminder(LocalDate.of(2025, 7, 13), LocalTime.of(9, 0), RecurrenceType.DAILY)
        assertEquals(false, engine.isExpired(r, NOW))
    }

    // ── DAILY ────────────────────────────────────────────────────────────────

    @Test
    fun `DAILY - time later today returns today`() {
        // NOW = 10:00, alarm time = 11:00 → fires today
        val r = reminder(LocalDate.of(2025, 7, 14), LocalTime.of(11, 0), RecurrenceType.DAILY)
        val expected = utcMillis(2025, 7, 14, 11, 0)
        assertEquals(expected, engine.nextTriggerMillis(r, NOW, UTC))
    }

    @Test
    fun `DAILY - time already passed today returns tomorrow`() {
        // NOW = 10:00, alarm time = 09:00 → fires tomorrow
        val r = reminder(LocalDate.of(2025, 7, 14), LocalTime.of(9, 0), RecurrenceType.DAILY)
        val expected = utcMillis(2025, 7, 15, 9, 0)
        assertEquals(expected, engine.nextTriggerMillis(r, NOW, UTC))
    }

    @Test
    fun `DAILY - exact same minute is treated as passed`() {
        // Trigger at exactly NOW (not strictly greater) → tomorrow
        val r = reminder(LocalDate.of(2025, 7, 14), LocalTime.of(10, 0), RecurrenceType.DAILY)
        val expected = utcMillis(2025, 7, 15, 10, 0)
        assertEquals(expected, engine.nextTriggerMillis(r, NOW, UTC))
    }

    // ── WEEKLY ───────────────────────────────────────────────────────────────

    @Test
    fun `WEEKLY - correct weekday and time in future returns today`() {
        // NOW = Monday 10:00, alarm = Monday 11:00 → fires today (same Monday)
        val r = reminder(LocalDate.of(2025, 7, 14), LocalTime.of(11, 0), RecurrenceType.WEEKLY)
        val expected = utcMillis(2025, 7, 14, 11, 0)
        assertEquals(expected, engine.nextTriggerMillis(r, NOW, UTC))
    }

    @Test
    fun `WEEKLY - correct weekday but time passed returns next week`() {
        // NOW = Monday 10:00, alarm = Monday 09:00 → next Monday
        val r = reminder(LocalDate.of(2025, 7, 14), LocalTime.of(9, 0), RecurrenceType.WEEKLY)
        val expected = utcMillis(2025, 7, 21, 9, 0)
        assertEquals(expected, engine.nextTriggerMillis(r, NOW, UTC))
    }

    @Test
    fun `WEEKLY - different weekday returns next occurrence of that day`() {
        // NOW = Monday 10:00, alarm = Wednesday → next Wednesday 2025-07-16
        val r = reminder(LocalDate.of(2025, 7, 16), LocalTime.of(9, 0), RecurrenceType.WEEKLY)
        val expected = utcMillis(2025, 7, 16, 9, 0)
        assertEquals(expected, engine.nextTriggerMillis(r, NOW, UTC))
    }

    @Test
    fun `WEEKLY - day already passed this week returns next week occurrence`() {
        // NOW = Monday 10:00, alarm = Sunday (yesterday) → next Sunday 2025-07-20
        val r = reminder(LocalDate.of(2025, 7, 13), LocalTime.of(9, 0), RecurrenceType.WEEKLY)
        val expected = utcMillis(2025, 7, 20, 9, 0)
        assertEquals(expected, engine.nextTriggerMillis(r, NOW, UTC))
    }

    // ── MONTHLY ──────────────────────────────────────────────────────────────

    @Test
    fun `MONTHLY - same day of month but time in future returns this month`() {
        // NOW = 14th 10:00, alarm = 14th 11:00 → still today
        val r = reminder(LocalDate.of(2025, 7, 14), LocalTime.of(11, 0), RecurrenceType.MONTHLY)
        val expected = utcMillis(2025, 7, 14, 11, 0)
        assertEquals(expected, engine.nextTriggerMillis(r, NOW, UTC))
    }

    @Test
    fun `MONTHLY - day of month not yet reached returns this month`() {
        // NOW = 14th, alarm = 20th → 2025-07-20
        val r = reminder(LocalDate.of(2025, 7, 20), LocalTime.of(9, 0), RecurrenceType.MONTHLY)
        val expected = utcMillis(2025, 7, 20, 9, 0)
        assertEquals(expected, engine.nextTriggerMillis(r, NOW, UTC))
    }

    @Test
    fun `MONTHLY - day of month already passed returns next month`() {
        // NOW = 14th, alarm = 10th → next occurrence is 2025-08-10
        val r = reminder(LocalDate.of(2025, 7, 10), LocalTime.of(9, 0), RecurrenceType.MONTHLY)
        val expected = utcMillis(2025, 8, 10, 9, 0)
        assertEquals(expected, engine.nextTriggerMillis(r, NOW, UTC))
    }

    @Test
    fun `MONTHLY - day 31 in 30-day month is clamped to day 30`() {
        // NOW = 2025-09-01 (September has 30 days), alarm = 31st
        // This month: clamped to Sep-30. That is in the future → return Sep-30.
        val nowSep1 = utcMillis(2025, 9, 1, 10, 0)
        val r = reminder(LocalDate.of(2025, 7, 31), LocalTime.of(9, 0), RecurrenceType.MONTHLY)
        val expected = utcMillis(2025, 9, 30, 9, 0)
        assertEquals(expected, engine.nextTriggerMillis(r, nowSep1, UTC))
    }

    @Test
    fun `MONTHLY - day 31 when this month is already past rolls to next month clamped`() {
        // NOW = 2025-09-30 11:00, alarm = 31st 09:00
        // This month clamped to Sep-30 09:00 which is in the past → go to Oct-31 (exists)
        val nowSep30 = utcMillis(2025, 9, 30, 11, 0)
        val r = reminder(LocalDate.of(2025, 7, 31), LocalTime.of(9, 0), RecurrenceType.MONTHLY)
        val expected = utcMillis(2025, 10, 31, 9, 0)
        assertEquals(expected, engine.nextTriggerMillis(r, nowSep30, UTC))
    }

    // ── Snooze precedence ────────────────────────────────────────────────────

    @Test
    fun `snoozeUntil in future overrides ONCE`() {
        val snooze = utcMillis(2025, 7, 14, 10, 30) // 30 min after NOW
        val r = reminder(LocalDate.of(2025, 7, 14), LocalTime.of(11, 0), RecurrenceType.ONCE, snoozeUntil = snooze)
        assertEquals(snooze, engine.nextTriggerMillis(r, NOW, UTC))
    }

    @Test
    fun `snoozeUntil in future overrides DAILY`() {
        val snooze = utcMillis(2025, 7, 14, 10, 15) // 15 min after NOW
        val r = reminder(LocalDate.of(2025, 7, 14), LocalTime.of(11, 0), RecurrenceType.DAILY, snoozeUntil = snooze)
        assertEquals(snooze, engine.nextTriggerMillis(r, NOW, UTC))
    }

    @Test
    fun `snoozeUntil in future overrides WEEKLY`() {
        val snooze = utcMillis(2025, 7, 14, 10, 45)
        val r = reminder(LocalDate.of(2025, 7, 14), LocalTime.of(11, 0), RecurrenceType.WEEKLY, snoozeUntil = snooze)
        assertEquals(snooze, engine.nextTriggerMillis(r, NOW, UTC))
    }

    @Test
    fun `snoozeUntil in future overrides MONTHLY`() {
        val snooze = utcMillis(2025, 7, 14, 10, 5)
        val r = reminder(LocalDate.of(2025, 7, 14), LocalTime.of(11, 0), RecurrenceType.MONTHLY, snoozeUntil = snooze)
        assertEquals(snooze, engine.nextTriggerMillis(r, NOW, UTC))
    }

    @Test
    fun `snoozeUntil in past is ignored and normal recurrence applies`() {
        // snooze was 5 min ago — should be ignored; daily 11:00 is in future → today
        val snooze = utcMillis(2025, 7, 14, 9, 55) // 5 min before NOW
        val r = reminder(LocalDate.of(2025, 7, 14), LocalTime.of(11, 0), RecurrenceType.DAILY, snoozeUntil = snooze)
        val expected = utcMillis(2025, 7, 14, 11, 0)
        assertEquals(expected, engine.nextTriggerMillis(r, NOW, UTC))
    }

    @Test
    fun `snoozeUntil exactly equal to now is treated as expired`() {
        // snoozeUntil == NOW is not strictly greater → ignored
        val r = reminder(LocalDate.of(2025, 7, 14), LocalTime.of(11, 0), RecurrenceType.DAILY, snoozeUntil = NOW)
        val expected = utcMillis(2025, 7, 14, 11, 0)
        assertEquals(expected, engine.nextTriggerMillis(r, NOW, UTC))
    }
}
