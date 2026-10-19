package app.kite.ui.pull

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.kite.R
import app.kite.container
import app.kite.domain.Prompts
import app.kite.domain.Sense
import app.kite.ui.Go
import app.kite.ui.Home
import app.kite.ui.art.Art
import app.kite.ui.art.rememberWind
import app.kite.ui.art.scene
import app.kite.ui.components.PrimaryButton
import app.kite.ui.ctx
import app.kite.ui.theme.U
import app.kite.ui.theme.UType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** How high the kite is before each turn of the spool, and after the last. */
private val levels = listOf(0.76f, 0.5f, 0.25f, 0f)

/** Seconds of holding for one turn. */
private const val TURN = 2.2f

/**
 * Reeling in: the kite is high up. Hold the spool while you find one thing you see, then one sound,
 * then one feeling in the body; each turn brings the kite lower, and the third lays it in the grass.
 */
@Composable
fun ReelScreen(id: Long, outside: Boolean, home: Home, go: Go) {
    val c = ctx()
    val haptic = LocalHapticFeedback.current
    val pull = home.pull(id)
    val prompts = remember(id) { Prompts.reel(id * 31 + (pull?.at ?: 0L)) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }
    var holding by remember { mutableStateOf(false) }
    val done = step >= 3
    val wind = rememberWind()
    val hour = remember { Art.hour() }
    val ink = Art.ink(hour)

    // Holding winds the spool; letting go lets it slip back a little.
    LaunchedEffect(holding, done) {
        if (done) return@LaunchedEffect
        var last = withFrameNanos { it }
        var ticks = 0
        while (true) {
            val now = withFrameNanos { it }
            val dt = (now - last) / 1e9f
            last = now
            if (holding) {
                progress = (progress + dt / TURN).coerceAtMost(1f)
                if (home.settings.haptics && (progress * 14).toInt() > ticks) {
                    ticks = (progress * 14).toInt()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                if (progress >= 1f) {
                    progress = 0f
                    ticks = 0
                    step++
                    if (home.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (step >= 3) {
                        c.container.write { db.dao().reeled(id) }
                        return@LaunchedEffect
                    }
                }
            } else {
                if (progress <= 0f) return@LaunchedEffect
                progress = (progress - dt / 1.4f).coerceAtLeast(0f)
                ticks = (progress * 14).toInt()
            }
        }
    }

    val base by animateFloatAsState(levels[step.coerceAtMost(3)], tween(1400), label = "height")
    val next = levels[(step + 1).coerceAtMost(3)]
    val height = if (done) base else base - (base - next) * progress * 0.35f

    BoxWithConstraints(Modifier.fillMaxSize().background(U.c.bg)) {
        val density = LocalDensity.current
        val spoolY = with(density) { (maxHeight - 150.dp).toPx() }
        val spool = 104.dp
        Canvas(Modifier.fillMaxSize()) {
            scene(height, wind, hour, slack = if (done) 0f else 0.1f * (1 - progress), hand = if (done) null else Offset(size.width / 2, spoolY))
        }

        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 36.dp, start = 28.dp, end = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (!done) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { i ->
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (i < step) U.c.kite else ink.copy(alpha = 0.25f)))
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
            AnimatedContent(step, transitionSpec = { fadeIn(tween(600, 300)) togetherWith fadeOut(tween(300)) }, label = "prompt") { s ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (s < 3) {
                        Text(stringResource(sense(Sense.entries[s])).uppercase(), style = UType.Eyebrow, color = ink.copy(alpha = 0.6f))
                        Spacer(Modifier.height(10.dp))
                        Text(prompts[s].of(home.ru), style = UType.Prompt.copy(textAlign = TextAlign.Center), color = ink)
                    } else {
                        Spacer(Modifier.height(60.dp))
                        Text(stringResource(R.string.reeled_title), style = UType.Hero.copy(textAlign = TextAlign.Center), color = ink)
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.reeled_sub), style = UType.Sub.copy(textAlign = TextAlign.Center), color = ink.copy(alpha = 0.78f))
                        Spacer(Modifier.height(32.dp))
                        // Up here, so the kite in the grass stays in sight.
                        AnimatedVisibility(done, enter = fadeIn(tween(600, 1200))) {
                            PrimaryButton(stringResource(if (outside) R.string.reeled_back else R.string.reeled_done), { go.done(outside) }, Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }

        if (!done) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = with(density) { spoolY.toDp() } - spool / 2)
                    .size(spool)
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            holding = true
                            tryAwaitRelease()
                            holding = false
                        })
                    },
            ) { Spool(progress, step) }
            Text(
                stringResource(if (step == 0 && progress == 0f) R.string.reel_hint_first else R.string.reel_hint),
                style = UType.Small,
                color = ink.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 28.dp),
            )
        }
    }
}

private fun sense(s: Sense) = when (s) {
    Sense.SEE -> R.string.sense_see
    Sense.HEAR -> R.string.sense_hear
    Sense.FEEL -> R.string.sense_feel
}

/** A wooden spool wound with string; it turns as you hold, and a ring fills for each turn. */
@Composable
private fun Spool(progress: Float, step: Int) {
    val red = U.c.kite
    Canvas(Modifier.fillMaxSize()) {
        val r = size.minDimension / 2
        val center = Offset(size.width / 2, size.height / 2)
        drawCircle(Color.Black.copy(alpha = 0.12f), r * 0.86f, center + Offset(0f, r * 0.06f))
        drawCircle(Color(0xFFE9D6B4), r * 0.82f, center)
        drawCircle(Color(0xFFB98D5E), r * 0.82f, center, style = Stroke(r * 0.07f))
        // String wound on: fuller as it's reeled in.
        val wound = 0.42f + 0.12f * (step + progress)
        for (i in 0 until 6) {
            drawCircle(Art.string.copy(alpha = 0.5f), r * wound * (0.55f + i * 0.09f), center, style = Stroke(r * 0.035f))
        }
        rotate((step + progress) * 540f, center) {
            for (i in 0 until 3) {
                val a = i * 2 * PI / 3
                val p = Offset(center.x + cos(a).toFloat() * r * 0.66f, center.y + sin(a).toFloat() * r * 0.66f)
                drawCircle(Color(0xFF8C6239), r * 0.085f, p)
            }
            drawLine(Color(0xFF8C6239), center, Offset(center.x + r * 0.78f, center.y), strokeWidth = r * 0.06f, cap = StrokeCap.Round)
        }
        drawCircle(Color(0xFF8C6239), r * 0.14f, center)
        val ring = r * 0.95f
        val box = Size(ring * 2, ring * 2)
        drawArc(red.copy(alpha = 0.18f), 0f, 360f, false, center - Offset(ring, ring), box, style = Stroke(r * 0.09f))
        drawArc(red, -90f, 360f * progress, false, center - Offset(ring, ring), box, style = Stroke(r * 0.09f, cap = StrokeCap.Round))
    }
}
