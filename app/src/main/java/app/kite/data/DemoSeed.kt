package app.kite.data

import app.kite.AppContainer
import app.kite.domain.Plan
import app.kite.domain.Source
import app.kite.domain.Where
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.random.Random

/**
 * Three weeks of made-up pulls for the demo build and the screenshots: busy weekdays that drift into
 * the future and into cares, slower weekends that stay here more, and a few drifts caught by yourself.
 */
object DemoSeed {
    suspend fun seedIfEmpty(c: AppContainer) {
        val dao = c.db.dao()
        if (dao.last() != null) return
        c.settings.update { it.copy(onboarded = true, salt = 42) }
        dao.insertAll(pulls(LocalDate.now(), ZoneId.systemDefault()))
    }

    fun pulls(today: LocalDate, zone: ZoneId): List<Pull> {
        val r = Random(7)
        val out = mutableListOf<Pull>()
        for (back in 20 downTo 0) {
            val day = today.minusDays(back.toLong())
            val weekend = day.dayOfWeek.value >= 6
            val times = Plan.of(day, 9 * 60, 21 * 60, 5, 42).toMutableList()
            // Today is only half lived: the morning and early afternoon.
            if (back == 0) times.retainAll { it < 14 * 60 }
            // Some tugs go unanswered.
            times.removeAll { r.nextFloat() < 0.15f }
            for (m in times) {
                val roll = r.nextFloat()
                val where = when {
                    roll < (if (weekend) 0.55f else 0.32f) -> Where.HERE
                    roll < (if (weekend) 0.70f else 0.62f) -> Where.FUTURE
                    roll < (if (weekend) 0.85f else 0.84f) -> Where.CARES
                    else -> Where.PAST
                }
                out += pull(day, m, where, Source.TUG, reeled = where == Where.HERE || r.nextFloat() < 0.9f, zone)
            }
            // Catching yourself, once or twice on a busy day.
            if (!weekend && r.nextFloat() < 0.6f && back != 0) {
                out += pull(day, 10 * 60 + r.nextInt(9 * 60), if (r.nextBoolean()) Where.FUTURE else Where.PAST, Source.SELF, true, zone)
            }
        }
        return out.sortedBy { it.at }
    }

    private fun pull(day: LocalDate, minute: Int, where: Where, source: Source, reeled: Boolean, zone: ZoneId) = Pull(
        at = day.atTime(LocalTime.of(minute / 60, minute % 60)).atZone(zone).toInstant().toEpochMilli(),
        where = where.name,
        source = source.name,
        reeled = reeled,
    )
}
