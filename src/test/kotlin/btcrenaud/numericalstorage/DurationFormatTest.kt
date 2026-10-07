package btcrenaud.numericalstorage

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

class DurationFormatTest {

    private val defaults = DurationFormat(
        days = "{value}j",
        hours = "{value}h",
        minutes = "{value}m",
        seconds = "{value}s",
        separator = " ",
    )

    @Test
    fun `non-zero units are written from days to seconds`() {
        val duration = Duration.ofDays(1).plusHours(2).plusMinutes(3).plusSeconds(4)

        assertEquals("1j 2h 3m 4s", duration.asReadable(defaults))
    }

    @Test
    fun `zero units are left out`() {
        assertEquals("2h 4s", Duration.ofHours(2).plusSeconds(4).asReadable(defaults))
    }

    @Test
    fun `a duration under one second reads as zero seconds`() {
        assertEquals("0s", Duration.ofMillis(400).asReadable(defaults))
    }

    @Test
    fun `an admin format changes the words and the separator`() {
        val format = DurationFormat("{value} days", "{value} hours", "{value} min", "{value} sec", ", ")

        assertEquals("1 days, 5 min", Duration.ofDays(1).plusMinutes(5).asReadable(format))
    }
}
