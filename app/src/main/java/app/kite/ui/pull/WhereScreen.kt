package app.kite.ui.pull

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kite.R
import app.kite.container
import app.kite.data.Pull
import app.kite.domain.Source
import app.kite.domain.Where
import app.kite.ui.Go
import app.kite.ui.T
import app.kite.ui.art.Art
import app.kite.ui.art.glyph
import app.kite.ui.art.scene
import app.kite.ui.art.rememberWind
import app.kite.ui.ctx
import app.kite.ui.theme.Places
import app.kite.ui.theme.U
import app.kite.ui.theme.UType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * "Where were you just now?" One honest tap. Here: the kite was in the grass all along. Anywhere
 * else: reel the string in. [self]: you caught yourself drifting, so "here" isn't offered.
 */
@Composable
fun WhereScreen(self: Boolean, outside: Boolean, go: Go) {
    val c = ctx()
    val dao = c.container.db.dao()
    val scope = rememberCoroutineScope()
    var here by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    fun pick(w: Where) {
        if (busy) return
        busy = true
        scope.launch {
            val id = dao.insert(Pull(where = w.name, source = (if (self) Source.SELF else Source.TUG).name, reeled = !w.away))
            if (w.away) go.reel(id, outside) else here = true
        }
    }

    AnimatedContent(here, transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(250)) }, label = "where") { landed ->
        if (landed) Landed(outside, go) else Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(36.dp))
            Text(stringResource(if (self) R.string.where_self_eyebrow else R.string.where_eyebrow).uppercase(), style = UType.Eyebrow)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(if (self) R.string.where_self_title else R.string.where_title), style = UType.Hero)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(if (self) R.string.where_self_sub else R.string.where_sub), style = UType.Sub)
            Spacer(Modifier.height(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                (if (self) Where.drifts else Where.entries).forEach { w ->
                    Choice(T.where(c, w), T.whereSub(c, w), w) { pick(w) }
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { go.done(outside) }, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp)) {
                Text(stringResource(R.string.not_now), style = UType.Button, color = U.c.faint)
            }
        }
    }
}

@Composable
private fun Choice(title: String, sub: String, w: Where, onClick: () -> Unit) {
    val color = Places.of(w)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(U.RLg)
            .background(U.c.surface)
            .border(1.dp, U.c.line, U.RLg)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(width = 40.dp, height = 52.dp)) {
            val s = size.height * 0.5f
            glyph(Offset(size.width / 2, size.height * 0.26f), s, color, tilt = if (w.away) 12f else -8f, tail = if (w.away) 0.9f else 0.5f)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = UType.Title.copy(fontSize = 21.sp))
            Spacer(Modifier.height(2.dp))
            Text(sub, style = UType.Muted)
        }
    }
}

/** Here: the kite is in the grass. A breath, and back to what you were doing. */
@Composable
private fun Landed(outside: Boolean, go: Go) {
    val wind = rememberWind()
    LaunchedEffect(Unit) {
        delay(2600)
        go.done(outside)
    }
    val hour = remember { Art.hour() }
    val ink = Art.ink(hour)
    Box(Modifier.fillMaxSize().clickable { go.done(outside) }) {
        Canvas(Modifier.fillMaxSize()) { scene(0f, wind, hour) }
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 120.dp, start = 28.dp, end = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.landed_title), style = UType.Hero.copy(textAlign = TextAlign.Center), color = ink)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.landed_sub), style = UType.Sub.copy(textAlign = TextAlign.Center), color = ink.copy(alpha = 0.78f))
        }
    }
}
