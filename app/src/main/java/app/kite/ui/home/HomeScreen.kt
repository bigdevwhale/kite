package app.kite.ui.home

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kite.R
import app.kite.container
import app.kite.domain.Where
import app.kite.notify.Tugs
import app.kite.ui.Go
import app.kite.ui.Home
import app.kite.ui.T
import app.kite.ui.art.Art
import app.kite.ui.art.DaySky
import app.kite.ui.art.rememberWind
import app.kite.ui.art.scene
import app.kite.ui.components.PrimaryButton
import app.kite.ui.components.SoftButton
import app.kite.ui.components.rememberNow
import app.kite.ui.ctx
import app.kite.ui.theme.U
import app.kite.ui.theme.UType
import java.time.Instant
import java.time.LocalDateTime

/** You only just came back: the kite still lies in the grass for this long. */
private const val RESTING_MS = 30 * 60_000L

/** The field: the kite, how the string is held today, today's small sky, and "I drifted off". */
@Composable
fun HomeScreen(home: Home, go: Go) {
    val c = ctx()
    val s = home.settings
    val now = rememberNow(30_000)
    val wind = rememberWind()
    val hour = Art.hour()
    val ink = Art.ink(hour)
    val last = home.pulls.lastOrNull()
    val resting = last != null && last.reeled && now - last.at < RESTING_MS
    val quiet = s.tugs && s.quietUntil > now
    val next = Tugs.next(s, LocalDateTime.ofInstant(Instant.ofEpochMilli(now), home.zone), home.zone)
    val today = home.day(home.today)

    fun set(change: (app.kite.data.Settings) -> app.kite.data.Settings) = c.container.write {
        settings.update(change)
        Tugs.reschedule(c)
    }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().height(60.dp).padding(start = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.app_name), style = UType.Display, modifier = Modifier.weight(1f))
            IconButton(onClick = go::settings) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = U.c.muted) }
        }

        Box(Modifier.weight(1f).fillMaxWidth().clip(U.RXl)) {
            Canvas(Modifier.fillMaxSize()) { scene(if (resting) 0f else 0.5f, wind, hour) }
            Column(Modifier.padding(22.dp)) {
                val (title, sub) = when {
                    resting -> stringResource(R.string.home_resting) to stringResource(R.string.home_resting_sub, T.time(last!!.minute(home.zone)))
                    !s.tugs -> stringResource(R.string.home_slack) to stringResource(R.string.home_slack_sub)
                    quiet -> stringResource(R.string.home_quiet) to stringResource(R.string.home_quiet_sub, T.time(LocalDateTime.ofInstant(Instant.ofEpochMilli(s.quietUntil), home.zone)))
                    else -> stringResource(R.string.home_taut) to stringResource(R.string.home_taut_sub, T.time(s.from), T.time(s.to))
                }
                Text(title, style = UType.Title.copy(fontSize = 26.sp), color = ink)
                Spacer(Modifier.height(4.dp))
                Text(sub, style = UType.Muted, color = ink.copy(alpha = 0.75f))
                if (next != null && !resting && next.toLocalDate() != home.today) {
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.home_tomorrow), style = UType.Muted, color = ink.copy(alpha = 0.75f))
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Column(
            Modifier.fillMaxWidth().clip(U.RLg).background(U.c.surface).border(1.dp, U.c.line, U.RLg).clickable { go.sky(home.today) }.padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(stringResource(R.string.home_today).uppercase(), style = UType.Eyebrow)
                    Spacer(Modifier.height(2.dp))
                    Text(todayLine(c, today), style = UType.Strong)
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = U.c.faint)
            }
            Spacer(Modifier.height(10.dp))
            DaySky(today, s.from, s.to, Modifier.fillMaxWidth().height(64.dp).clip(U.RSm), small = true)
        }

        Spacer(Modifier.height(12.dp))
        PrimaryButton(stringResource(R.string.drifted), { go.where(self = true) }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            when {
                !s.tugs -> SoftButton(stringResource(R.string.tugs_on), { set { it.copy(tugs = true, quietUntil = 0) } }, icon = Icons.Rounded.NotificationsActive)
                quiet -> SoftButton(stringResource(R.string.quiet_off), { set { it.copy(quietUntil = 0) } }, icon = Icons.Rounded.NotificationsActive)
                else -> SoftButton(stringResource(R.string.quiet_hour), { set { it.copy(quietUntil = System.currentTimeMillis() + 3_600_000L) } }, icon = Icons.Rounded.NotificationsOff, color = U.c.muted, soft = U.c.surface2)
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

/** "Here 2 · Future 1", or a quiet line before the first tug. */
private fun todayLine(c: android.content.Context, day: app.kite.domain.Day): String {
    if (day.pulls.isEmpty()) return c.getString(R.string.home_today_empty)
    return Where.entries.filter { day.count.getValue(it) > 0 }.joinToString("  ·  ") { "${T.where(c, it)} ${day.count.getValue(it)}" }
}
