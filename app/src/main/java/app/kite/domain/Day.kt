package app.kite.domain

import app.kite.data.Pull
import java.time.LocalDate
import java.time.ZoneId

/** One day's sky: its pulls, and where the mind went most. */
class Day(val date: LocalDate, val pulls: List<Pull>) {
    val count: Map<Where, Int> = Where.entries.associateWith { w -> pulls.count { it.whereOf == w } }

    /** Where the mind went most when it was away; null with no drifts or a tie. */
    val drift: Where? = Where.drifts.map { it to count.getValue(it) }.sortedByDescending { it.second }.let { top ->
        if (top[0].second == 0 || top[0].second == top[1].second) null else top[0].first
    }

    /** More "here" than away. */
    val mostlyHere: Boolean = pulls.isNotEmpty() && count.getValue(Where.HERE) * 2 > pulls.size

    companion object {
        fun of(pulls: List<Pull>, date: LocalDate, zone: ZoneId = ZoneId.systemDefault()) = Day(date, pulls.filter { it.day(zone) == date })

        /** Height of a kite in the day's sky, 0 = grass, 1 = top. Stable for a pull, so the sky doesn't shuffle. */
        fun height(p: Pull): Float {
            if (!p.whereOf.away) return 0f
            val h = ((p.id * -7046029254386353131L) ushr 44) % 1000 / 1000f
            return 0.38f + 0.5f * h
        }
    }
}
