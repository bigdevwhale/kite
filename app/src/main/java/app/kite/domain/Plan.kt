package app.kite.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.random.Random

/**
 * When the string is tugged. The day's window is cut into [count] equal slots and each slot gets one
 * moment somewhere in its middle, so tugs never bunch up yet can't be guessed. The plan is worked out
 * from the date and a per-install [salt], so it needs no storage and comes out the same after a reboot.
 */
object Plan {
    /** Minutes after midnight of each tug on [day], in order. */
    fun of(day: LocalDate, from: Int, to: Int, count: Int, salt: Long): List<Int> {
        if (count <= 0 || to <= from) return emptyList()
        val r = Random(day.toEpochDay() * 7919 + salt)
        val slot = (to - from).toDouble() / count
        return (0 until count).map { i ->
            (from + slot * i + slot * (0.15 + 0.7 * r.nextDouble())).toInt().coerceIn(from, to - 1)
        }
    }

    /** The first tug strictly after [now] and not before [quietUntil], looking up to a week ahead. */
    fun next(now: LocalDateTime, from: Int, to: Int, count: Int, salt: Long, quietUntil: LocalDateTime? = null): LocalDateTime? {
        for (d in 0..7) {
            val day = now.toLocalDate().plusDays(d.toLong())
            for (m in of(day, from, to, count, salt)) {
                val at = day.atTime(LocalTime.of(m / 60, m % 60))
                if (at.isAfter(now) && (quietUntil == null || !at.isBefore(quietUntil))) return at
            }
        }
        return null
    }
}
