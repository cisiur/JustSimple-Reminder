package com.justsimple.reminder

import android.content.Context
import com.justsimple.reminder.domain.formatter.TimeRemainingFormatter
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TimeRemainingFormatterTest {

    private val context: Context = mockk()
    private lateinit var formatter: TimeRemainingFormatter

    @Before
    fun setUp() {
        every { context.getString(R.string.time_remaining_expired) } returns "Expired"
        every { context.getString(R.string.time_remaining_now)     } returns "Now"
        every { context.getString(R.string.time_remaining_minutes, any()) } answers { "${(args[1] as Array<*>)[0]} min" }
        every { context.getString(R.string.time_remaining_hours,   any()) } answers { "${(args[1] as Array<*>)[0]} h" }
        every { context.getString(R.string.time_remaining_days,    any()) } answers { "${(args[1] as Array<*>)[0]} days" }
        every { context.getString(R.string.time_remaining_weeks,   any()) } answers { "${(args[1] as Array<*>)[0]} weeks" }

        formatter = TimeRemainingFormatter(context)
    }

    // ── Boundary: past / immediate ────────────────────────────────────────────

    @Test
    fun `past trigger returns expired`() {
        assertEquals("Expired", formatter.format(triggerMillis = 999L, nowMillis = 1_000L))
    }

    @Test
    fun `trigger equal to now returns expired`() {
        assertEquals("Expired", formatter.format(triggerMillis = 1_000L, nowMillis = 1_000L))
    }

    @Test
    fun `less than one minute ahead returns now`() {
        assertEquals("Now", formatter.format(triggerMillis = 59_999L, nowMillis = 0L))
    }

    // ── Minutes ───────────────────────────────────────────────────────────────

    @Test
    fun `formats minutes correctly`() {
        assertEquals("1 min",  formatter.format(triggerMillis = 60_000L,      nowMillis = 0L))
        assertEquals("59 min", formatter.format(triggerMillis = 59 * 60_000L, nowMillis = 0L))
    }

    // ── Hours ─────────────────────────────────────────────────────────────────

    @Test
    fun `formats hours correctly`() {
        assertEquals("1 h",  formatter.format(triggerMillis = 3_600_000L,       nowMillis = 0L))
        assertEquals("23 h", formatter.format(triggerMillis = 23 * 3_600_000L,  nowMillis = 0L))
    }

    // ── Days ──────────────────────────────────────────────────────────────────

    @Test
    fun `formats days correctly`() {
        assertEquals("2 days",  formatter.format(triggerMillis = 2  * 86_400_000L, nowMillis = 0L))
        assertEquals("13 days", formatter.format(triggerMillis = 13 * 86_400_000L, nowMillis = 0L))
    }

    // ── Weeks ─────────────────────────────────────────────────────────────────

    @Test
    fun `formats weeks correctly`() {
        // threshold is >= 2 weeks (14 days)
        assertEquals("2 weeks", formatter.format(triggerMillis = 14 * 86_400_000L, nowMillis = 0L))
        assertEquals("3 weeks", formatter.format(triggerMillis = 21 * 86_400_000L, nowMillis = 0L))
    }

    // ── Boundary between days and weeks ──────────────────────────────────────

    @Test
    fun `13 days is days not weeks`() {
        assertEquals("13 days", formatter.format(triggerMillis = 13 * 86_400_000L, nowMillis = 0L))
    }

    @Test
    fun `14 days crosses into weeks`() {
        assertEquals("2 weeks", formatter.format(triggerMillis = 14 * 86_400_000L, nowMillis = 0L))
    }
}
