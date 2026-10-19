package app.kite

import app.kite.data.DemoSeed
import app.kite.data.Pull
import app.kite.domain.Day
import app.kite.domain.Plan
import app.kite.domain.Prompts
import app.kite.domain.Sense
import app.kite.domain.Where
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class KiteTest {
    private val day = LocalDate.of(2026, 10, 19)

    @Test fun planFallsInsideTheWindowOnePerSlot() {
        val times = Plan.of(day, 9 * 60, 21 * 60, 5, 42)
        assertEquals(5, times.size)
        val slot = 12 * 60 / 5
        times.forEachIndexed { i, m ->
            assertTrue(m in (9 * 60 + slot * i) until (9 * 60 + slot * (i + 1)))
        }
        assertEquals(times.sorted(), times)
    }

    @Test fun planIsStableAndDiffersByDayAndSalt() {
        assertEquals(Plan.of(day, 540, 1260, 5, 42), Plan.of(day, 540, 1260, 5, 42))
        assertNotEquals(Plan.of(day, 540, 1260, 5, 42), Plan.of(day.plusDays(1), 540, 1260, 5, 42))
        assertNotEquals(Plan.of(day, 540, 1260, 5, 42), Plan.of(day, 540, 1260, 5, 43))
    }

    @Test fun nextSkipsPastAndQuietAndRollsToTomorrow() {
        val times = Plan.of(day, 540, 1260, 5, 42)
        val at = { m: Int -> day.atTime(m / 60, m % 60) }
        assertEquals(at(times[0]), Plan.next(day.atTime(6, 0), 540, 1260, 5, 42))
        assertEquals(at(times[1]), Plan.next(at(times[0]), 540, 1260, 5, 42))
        assertEquals(at(times[3]), Plan.next(day.atTime(6, 0), 540, 1260, 5, 42, quietUntil = at(times[2] + 1)))
        val tomorrow = Plan.of(day.plusDays(1), 540, 1260, 5, 42)
        assertEquals(day.plusDays(1).atTime(tomorrow[0] / 60, tomorrow[0] % 60), Plan.next(day.atTime(22, 0), 540, 1260, 5, 42))
    }

    @Test fun emptyWindowHasNoTugs() {
        assertTrue(Plan.of(day, 600, 600, 5, 1).isEmpty())
        assertNull(Plan.next(LocalDateTime.of(2026, 1, 1, 8, 0), 600, 500, 5, 1))
    }

    @Test fun reelAsksOneOfEachSense() {
        val r = Prompts.reel(7)
        assertEquals(3, r.size)
        Sense.entries.forEachIndexed { i, s -> assertTrue(r[i] in Prompts.of(s)) }
        assertEquals(r, Prompts.reel(7))
    }

    @Test fun promptsHaveBothLanguages() {
        Sense.entries.flatMap { Prompts.of(it) }.forEach {
            assertTrue(it.en.isNotBlank() && it.ru.isNotBlank() && it.en != it.ru)
        }
    }

    private fun pull(id: Long, w: Where) = Pull(id = id, at = day.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli(), where = w.name)

    @Test fun dayFindsWhereTheMindWentMost() {
        val d = Day(day, listOf(pull(1, Where.FUTURE), pull(2, Where.FUTURE), pull(3, Where.PAST), pull(4, Where.HERE)))
        assertEquals(Where.FUTURE, d.drift)
        assertTrue(!d.mostlyHere)
        assertNull(Day(day, listOf(pull(1, Where.FUTURE), pull(2, Where.PAST))).drift)
        assertTrue(Day(day, listOf(pull(1, Where.HERE), pull(2, Where.HERE), pull(3, Where.CARES))).mostlyHere)
    }

    @Test fun hereLiesInTheGrassAwayFliesHigh() {
        assertEquals(0f, Day.height(pull(5, Where.HERE)))
        (1L..50L).forEach { assertTrue(Day.height(pull(it, Where.PAST)) in 0.38f..0.88f) }
    }

    @Test fun demoHasThreeWeeksAndNothingAfterTwoToday() {
        val zone = ZoneOffset.UTC
        val pulls = DemoSeed.pulls(day, zone)
        assertTrue(pulls.size > 60)
        assertEquals(day.minusDays(20), pulls.first().day(zone))
        assertTrue(pulls.filter { it.day(zone) == day }.all { it.minute(zone) < 14 * 60 })
    }
}
