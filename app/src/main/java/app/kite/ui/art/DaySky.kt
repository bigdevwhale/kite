package app.kite.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import app.kite.domain.Day
import app.kite.ui.theme.Places

/**
 * A day's sky: one small kite for each pull, across the day from [from] to [to] (minutes). Where the
 * mind was gives its colour; here lies in the grass, away flies high. A reeled-in kite has its string
 * down to the ground; one let go has a loose end.
 */
@Composable
fun DaySky(day: Day, from: Int, to: Int, modifier: Modifier = Modifier, small: Boolean = false, hour: Float = 13f) {
    val wind = if (small) 0f else rememberWind()
    Canvas(modifier) {
        sky(hour, wind, clouds = !small)
        val groundY = size.height * if (small) 0.8f else 0.84f
        field(groundY, wind, hour)
        val lo = minOf(from, day.pulls.minOfOrNull { it.minute() } ?: from)
        val hi = maxOf(to, day.pulls.maxOfOrNull { it.minute() + 1 } ?: to)
        val s = size.height * if (small) 0.2f else 0.11f
        val pad = size.width * 0.07f
        // Kites close in time are nudged apart so each can be seen.
        val xs = day.pulls.map { pad + (size.width - pad * 2) * (it.minute() - lo) / (hi - lo).toFloat() }.toMutableList()
        val gap = s * 0.85f
        for (i in 1 until xs.size) xs[i] = maxOf(xs[i], xs[i - 1] + gap)
        val over = (xs.lastOrNull() ?: 0f) - (size.width - pad)
        if (over > 0) for (i in xs.indices.reversed()) xs[i] = minOf(xs[i], if (i == xs.lastIndex) size.width - pad else xs[i + 1] - gap)
        day.pulls.forEachIndexed { i, p ->
            val x = xs[i].coerceAtLeast(pad * 0.5f)
            val h = Day.height(p)
            val c = Places.of(p.whereOf)
            if (h == 0f) {
                glyph(Offset(x, groundY + s * 0.05f), s * 0.75f, c, tilt = -60f, wind = 0f, tail = 0.7f)
                return@forEachIndexed
            }
            val y = groundY - (groundY - s) * h
            val bridle = glyph(Offset(x, y), s, c, tilt = 8f, wind = wind + p.id)
            if (p.reeled) string(bridle, Offset(x - s * 0.4f, groundY + s * 0.2f), wind + p.id, width = if (small) 1f else 1.6f, color = Art.string.copy(alpha = 0.55f))
            else loose(bridle, s * 1.6f, wind + p.id, width = if (small) 1f else 1.4f)
        }
    }
}
