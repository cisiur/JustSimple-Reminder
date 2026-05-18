package com.justsimple.reminder

import com.justsimple.reminder.domain.recurrence.RecurrenceEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RecurrenceEngineTest {

    private lateinit var engine: RecurrenceEngine

    @Before
    fun setUp() {
        engine = RecurrenceEngine()
    }

    @Test
    fun `ONCE in future returns scheduled time`() {
        // TODO Module 3
    }

    @Test
    fun `ONCE in past returns null`() {
        // TODO Module 3
    }

    @Test
    fun `DAILY returns next occurrence after now`() {
        // TODO Module 3
    }

    @Test
    fun `WEEKLY returns correct weekday`() {
        // TODO Module 3
    }

    @Test
    fun `MONTHLY rolls to next month when day passed`() {
        // TODO Module 3
    }

    @Test
    fun `snoozeUntil takes precedence over normal recurrence`() {
        // TODO Module 3
    }
}

