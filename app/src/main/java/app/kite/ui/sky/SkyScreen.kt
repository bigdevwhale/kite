package app.kite.ui.sky

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.kite.R
import app.kite.domain.Source
import app.kite.domain.Where
import app.kite.ui.Go
import app.kite.ui.Home
import app.kite.ui.T
import app.kite.ui.art.DaySky
import app.kite.ui.art.glyph
import app.kite.ui.components.SectionTitle
import app.kite.ui.components.TopBar
import app.kite.ui.components.UCard
import app.kite.ui.ctx
import app.kite.ui.theme.Places
import app.kite.ui.theme.U
import app.kite.ui.theme.UType
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * A day's sky, to look at in the evening: a kite for each tug, where the mind went most, and the
 * week's skies below to step between. No scores; just the picture.
 */
@Composable
fun SkyScreen(date: LocalDate, home: Home, go: Go) {
    val c = ctx()
    var epoch by rememberSaveable { mutableStateOf(date.toEpochDay()) }
    val d = LocalDate.ofEpochDay(epoch)
    val day = home.day(d)
    val s = home.settings

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.sky_title), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { epoch-- }, enabled = d > home.first) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.sky_prev), tint = if (d > home.first) U.c.ink else U.c.line)
                }
                Text(
                    if (d == home.today) stringResource(R.string.sky_today) else T.weekday(c, d),
                    style = UType.Title,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                IconButton(onClick = { epoch++ }, enabled = d < home.today) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.sky_next), tint = if (d < home.today) U.c.ink else U.c.line)
                }
            }
            Spacer(Modifier.height(8.dp))
            DaySky(day, s.from, s.to, Modifier.fillMaxWidth().height(300.dp).clip(U.RXl))
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
                Text(T.time(s.from), style = UType.Small)
                Spacer(Modifier.weight(1f))
                Text(T.time(s.to), style = UType.Small)
            }

            Spacer(Modifier.height(10.dp))
            Text(line(c, day), style = UType.Prompt.copy(fontSize = UType.Title.fontSize, lineHeight = UType.Title.lineHeight), modifier = Modifier.padding(horizontal = 4.dp))

            if (day.pulls.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                UCard {
                    Where.entries.forEach { w ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Canvas(Modifier.size(width = 26.dp, height = 34.dp)) {
                                glyph(Offset(size.width / 2, size.height * 0.3f), size.height * 0.48f, Places.of(w), tilt = if (w.away) 10f else -8f, tail = if (w.away) 0.7f else 0.45f)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(T.where(c, w), style = UType.Body, modifier = Modifier.weight(1f))
                            Text(day.count.getValue(w).toString(), style = UType.Strong)
                        }
                    }
                    val away = day.pulls.count { it.whereOf.away }
                    val reeled = day.pulls.count { it.whereOf.away && it.reeled }
                    val self = day.pulls.count { it.sourceOf == Source.SELF }
                    if (away > 0 || self > 0) {
                        Spacer(Modifier.height(8.dp))
                        if (away > 0) Text(stringResource(R.string.sky_reeled, reeled, away), style = UType.Muted)
                        if (self > 0) Text(T.plural(c, R.plurals.sky_self, self), style = UType.Muted)
                    }
                }
            }

            SectionTitle(stringResource(R.string.sky_week))
            val monday = d.with(DayOfWeek.MONDAY)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0L until 7L).map { monday.plusDays(it) }.forEach { wd ->
                    val open = wd >= home.first && wd <= home.today
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        DaySky(
                            home.day(wd), s.from, s.to,
                            Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .clip(U.RSm)
                                .then(if (wd == d) Modifier.border(2.dp, U.c.kite, U.RSm) else Modifier)
                                .then(if (open) Modifier.clickable { epoch = wd.toEpochDay() } else Modifier),
                            small = true,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(T.dow(c, wd), style = UType.Small, color = if (wd == d) U.c.kite else U.c.faint)
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

/** One line about the day, in words, never a score. */
private fun line(c: android.content.Context, day: app.kite.domain.Day): String = when {
    day.pulls.isEmpty() -> c.getString(R.string.day_empty)
    day.mostlyHere -> c.getString(R.string.day_here)
    day.drift != null -> T.drift(c, day.drift)
    else -> c.getString(R.string.day_mixed)
}
