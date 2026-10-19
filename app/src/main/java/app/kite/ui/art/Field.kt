package app.kite.ui.art

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import app.kite.BuildConfig
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** The paper kite, the sky it flies in and the field below, all drawn in code. */
object Art {
    /** Renders and screenshots hold the clock at a fixed hour, so the sky is the same every time. */
    var fixedHour: Float? = if (BuildConfig.SHOTS) 13.5f else null

    /** Renders and screenshots hold the wind still. */
    var still: Boolean = BuildConfig.SHOTS

    fun hour(): Float = fixedHour ?: LocalTime.now().let { it.hour + it.minute / 60f }

    val red = Color(0xFFD4543D)
    val cream = Color(0xFFF7EBD3)
    val spar = Color(0xFF6B4B33)
    val string = Color(0xFF4A4038)

    /** Top and bottom of the sky at [hour]: night, dawn, day, dusk, night again. */
    fun sky(hour: Float): Pair<Color, Color> {
        val stops = listOf(
            0f to (Color(0xFF1B2440) to Color(0xFF2E3A5C)),
            5f to (Color(0xFF1F2A48) to Color(0xFF3A4568)),
            7f to (Color(0xFF8FB3D9) to Color(0xFFF6D5BC)),
            10f to (Color(0xFF7FB2E0) to Color(0xFFDDEBF2)),
            16f to (Color(0xFF7FB0DD) to Color(0xFFE3EDF0)),
            19f to (Color(0xFF8D8FC4) to Color(0xFFF2C6A4)),
            21f to (Color(0xFF2A3157) to Color(0xFF5A5480)),
            24f to (Color(0xFF1B2440) to Color(0xFF2E3A5C)),
        )
        val h = ((hour % 24f) + 24f) % 24f
        val i = stops.indexOfLast { it.first <= h }.coerceIn(0, stops.size - 2)
        val (h0, c0) = stops[i]
        val (h1, c1) = stops[i + 1]
        val t = ((h - h0) / (h1 - h0)).coerceIn(0f, 1f)
        return lerp(c0.first, c1.first, t) to lerp(c0.second, c1.second, t)
    }

    fun night(hour: Float): Boolean = hour < 6f || hour >= 20.5f

    /** Letters that read on the sky at [hour]: ink by day, moonlight at night. */
    fun ink(hour: Float): Color = if (hour < 6.5f || hour >= 20f) Color(0xFFF3EEE2) else Color(0xFF1F2B38)
}

/** Seconds of wind since the screen opened; stands still in renders. */
@Composable
fun rememberWind(): Float {
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Art.still) {
        if (Art.still) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { t = (it - start) / 1e9f }
        }
    }
    return t
}

/** Sky gradient, sun or moon and stars, and slow clouds; a small sky ([clouds] off) is just the gradient and stars. */
fun DrawScope.sky(hour: Float, wind: Float, clouds: Boolean = true) {
    val (top, bottom) = Art.sky(hour)
    drawRect(Brush.verticalGradient(listOf(top, bottom)))
    val night = Art.night(hour)
    if (night) {
        for (i in 0 until 46) {
            val x = ((i * 7919) % 1000) / 1000f * size.width
            val y = ((i * 104729) % 1000) / 1000f * size.height * 0.62f
            val tw = 0.45f + 0.35f * sin(wind * 0.8f + i)
            drawCircle(Color.White.copy(alpha = tw * 0.8f), radius = size.minDimension * (0.0025f + (i % 3) * 0.0012f), center = Offset(x, y))
        }
        drawCircle(Color(0xFFF4EED8), radius = size.minDimension * 0.05f, center = Offset(size.width * 0.8f, size.height * 0.16f))
        drawCircle(top, radius = size.minDimension * 0.045f, center = Offset(size.width * 0.775f, size.height * 0.15f))
    } else if (clouds) {
        // The sun keeps to the left, out of the way of words, and climbs and sinks with the day.
        val p = ((hour - 6f) / 14f).coerceIn(0f, 1f)
        val sx = size.width * (0.14f + 0.12f * p)
        val sy = size.height * (0.46f - 0.17f * sin(p * PI).toFloat())
        drawCircle(Color(0xFFFFF1C9).copy(alpha = 0.35f), radius = size.minDimension * 0.11f, center = Offset(sx, sy))
        drawCircle(Color(0xFFFFF6DA), radius = size.minDimension * 0.055f, center = Offset(sx, sy))
    }
    if (!clouds) return
    val cloud = Color.White.copy(alpha = if (night) 0.08f else 0.72f)
    listOf(Triple(0.18f, 0.22f, 1f), Triple(0.62f, 0.34f, 0.7f), Triple(0.9f, 0.12f, 0.85f)).forEachIndexed { i, (x0, y, s) ->
        val w = size.width * 0.26f * s
        val x = ((x0 * size.width + wind * (6f + i * 3f)) % (size.width + w * 2)) - w
        puff(Offset(x, size.height * y), w, cloud)
    }
}

private fun DrawScope.puff(at: Offset, w: Float, color: Color) {
    val h = w * 0.32f
    drawOval(color, Offset(at.x - w / 2, at.y - h * 0.2f), Size(w, h))
    drawCircle(color, h * 0.62f, Offset(at.x - w * 0.14f, at.y - h * 0.05f))
    drawCircle(color, h * 0.8f, Offset(at.x + w * 0.1f, at.y - h * 0.18f))
}

/** Two soft hills and a fringe of grass from [groundY] down. */
fun DrawScope.field(groundY: Float, wind: Float, hour: Float) {
    val dark = Art.night(hour)
    val far = if (dark) Color(0xFF2C3F3A) else Color(0xFF9CC08A)
    val near = if (dark) Color(0xFF243530) else Color(0xFF79A866)
    val back = Path().apply {
        moveTo(0f, groundY - size.height * 0.03f)
        cubicTo(size.width * 0.3f, groundY - size.height * 0.09f, size.width * 0.55f, groundY + size.height * 0.01f, size.width, groundY - size.height * 0.06f)
        lineTo(size.width, size.height); lineTo(0f, size.height); close()
    }
    drawPath(back, far)
    val front = Path().apply {
        moveTo(0f, groundY + size.height * 0.02f)
        cubicTo(size.width * 0.35f, groundY - size.height * 0.02f, size.width * 0.7f, groundY + size.height * 0.04f, size.width, groundY)
        lineTo(size.width, size.height); lineTo(0f, size.height); close()
    }
    drawPath(front, near)
    val blade = if (dark) Color(0xFF1C2B27) else Color(0xFF5E9152)
    val n = (size.width / 9f).toInt()
    for (i in 0..n) {
        val x = i * size.width / n + ((i * 37) % 7)
        val base = groundY + size.height * 0.02f + ((i * 53) % 11) * size.height * 0.004f
        val h = size.height * (0.018f + ((i * 31) % 5) * 0.004f)
        val sway = sin(wind * 1.3f + i * 0.7f) * h * 0.25f
        drawLine(blade, Offset(x, base), Offset(x + sway + h * 0.2f, base - h), strokeWidth = 2.2f, cap = StrokeCap.Round)
    }
}

/**
 * The kite: a diamond of four paper panels on two spars, a tail of bows. [at] is where the spars cross,
 * [s] its height, [tilt] in degrees. Returns the bridle point the string ties to.
 */
fun DrawScope.kite(at: Offset, s: Float, tilt: Float, wind: Float, a: Color = Art.red, b: Color = Art.cream, tail: Float = 1.6f, bows: Int = 4): Offset {
    val w = s * 0.36f
    val up = s * 0.34f
    val down = s * 0.66f
    rotate(tilt, pivot = at) {
        // The tail, hanging off the bottom point and waving in the wind.
        val start = Offset(at.x, at.y + down)
        val len = s * tail
        val path = Path().apply { moveTo(start.x, start.y) }
        val steps = 18
        val knots = mutableListOf<Offset>()
        for (i in 1..steps) {
            val f = i / steps.toFloat()
            // The tail streams downwind, away from the string.
            val x = start.x + s * 0.85f * f * sqrt(f) + sin(wind * 3.1f - f * 5.5f) * s * 0.13f * f
            val y = start.y + len * f
            path.lineTo(x, y)
            if (i % (steps / bows) == 0) knots += Offset(x, y)
        }
        drawPath(path, Art.string, style = Stroke(width = s * 0.018f, cap = StrokeCap.Round))
        knots.forEachIndexed { i, k ->
            val bw = s * 0.09f
            val c = if (i % 2 == 0) a else b
            val bow = Path().apply {
                moveTo(k.x, k.y); lineTo(k.x - bw, k.y - bw * 0.55f); lineTo(k.x - bw, k.y + bw * 0.55f); close()
                moveTo(k.x, k.y); lineTo(k.x + bw, k.y - bw * 0.55f); lineTo(k.x + bw, k.y + bw * 0.55f); close()
            }
            drawPath(bow, c)
        }
        // Four panels, alternating.
        val top = Offset(at.x, at.y - up)
        val right = Offset(at.x + w, at.y)
        val bottom = Offset(at.x, at.y + down)
        val left = Offset(at.x - w, at.y)
        fun tri(p1: Offset, p2: Offset, c: Color) = drawPath(Path().apply { moveTo(at.x, at.y); lineTo(p1.x, p1.y); lineTo(p2.x, p2.y); close() }, c)
        tri(top, right, b)
        tri(right, bottom, a)
        tri(bottom, left, b)
        tri(left, top, a)
        // A faint shade on the lower half, as paper bellies in the wind.
        drawPath(Path().apply { moveTo(left.x, left.y); lineTo(right.x, right.y); lineTo(bottom.x, bottom.y); close() }, Color.Black.copy(alpha = 0.06f))
        val outline = Path().apply { moveTo(top.x, top.y); lineTo(right.x, right.y); lineTo(bottom.x, bottom.y); lineTo(left.x, left.y); close() }
        drawPath(outline, Art.spar.copy(alpha = 0.55f), style = Stroke(width = s * 0.014f))
        drawLine(Art.spar, top, bottom, strokeWidth = s * 0.022f, cap = StrokeCap.Round)
        drawLine(Art.spar, left, right, strokeWidth = s * 0.022f, cap = StrokeCap.Round)
    }
    // The bridle ties just below the cross, wherever the tilt has put it.
    val r = (tilt * PI / 180).toFloat()
    val dy = s * 0.1f
    return Offset(at.x - sin(r) * dy, at.y + cos(r) * dy)
}

/**
 * The string from the kite's bridle [from] down to the hand at [to]. Taut, it bows gently with the wind;
 * slack, it hangs in a deep belly.
 */
fun DrawScope.string(from: Offset, to: Offset, wind: Float, slack: Float = 0f, width: Float = 2.2f, color: Color = Art.string) {
    val mid = Offset((from.x + to.x) / 2, (from.y + to.y) / 2)
    val len = (to - from).getDistance()
    val bow = len * (0.08f + 0.02f * sin(wind * 0.9f)) + len * 0.35f * slack
    val ctrl = Offset(mid.x + bow * 0.3f, mid.y + bow * 0.5f)
    val p = Path().apply { moveTo(from.x, from.y); quadraticTo(ctrl.x, ctrl.y, to.x, to.y) }
    drawPath(p, color, style = Stroke(width = width, cap = StrokeCap.Round))
}

/** A loose end: a kite let go, its string dangling below it. */
fun DrawScope.loose(from: Offset, len: Float, wind: Float, width: Float = 1.6f, color: Color = Art.string) {
    val p = Path().apply {
        moveTo(from.x, from.y)
        val steps = 10
        for (i in 1..steps) {
            val f = i / steps.toFloat()
            lineTo(from.x + sin(wind * 2f + f * 4f) * len * 0.08f * f - len * 0.18f * f, from.y + len * f)
        }
    }
    drawPath(p, color.copy(alpha = 0.7f), style = Stroke(width = width, cap = StrokeCap.Round))
}

/**
 * The main scene: sky, field, and the kite at [height] (0 = lying in the grass, 1 = high up), its
 * string down to the hand at the bottom. [slack] loosens the string while you reel.
 */
fun DrawScope.scene(height: Float, wind: Float, hour: Float, slack: Float = 0f, kiteSize: Float = 0.2f, hand: Offset? = null) {
    sky(hour, wind)
    val groundY = size.height * 0.86f
    field(groundY, wind, hour)
    val s = size.minDimension * kiteSize
    val end = hand ?: Offset(size.width * 0.5f, size.height + 4f)
    val h = height.coerceIn(0f, 1f)
    if (h < 0.02f) {
        // Resting in the grass, tilted over, string in a loose coil.
        val at = Offset(size.width * 0.56f, groundY + s * 0.05f)
        val bridle = kite(at, s * 0.8f, -62f, wind * 0.15f, tail = 1.0f)
        string(bridle, end, wind * 0.2f, slack = 0.6f)
        return
    }
    val gust = if (Art.still) 0f else sin(wind * 0.7f)
    val x = size.width * (0.5f + 0.16f * h) + gust * size.width * 0.03f * h
    val y = groundY - (groundY - size.height * 0.16f) * h + cos(wind * 1.1f) * s * 0.08f * h
    val tilt = 8f * h + gust * 6f * h
    val bridle = kite(Offset(x, y), s, tilt, wind)
    string(bridle, end, wind, slack)
}

/** A small kite of one colour, for legends and the day's sky. */
fun DrawScope.glyph(at: Offset, s: Float, c: Color, tilt: Float = 10f, wind: Float = 0f, tail: Float = 1.1f): Offset =
    kite(at, s, tilt, wind, a = c, b = lerp(c, Color.White, 0.62f), tail = tail, bows = 3)
