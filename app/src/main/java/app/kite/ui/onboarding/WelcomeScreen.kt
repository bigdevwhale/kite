package app.kite.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.kite.R
import app.kite.container
import app.kite.notify.Tugs
import app.kite.ui.T
import app.kite.ui.art.rememberWind
import app.kite.ui.art.scene
import app.kite.ui.components.PrimaryButton
import app.kite.ui.components.Segmented
import app.kite.ui.ctx
import app.kite.ui.settings.FROM_HOURS
import app.kite.ui.settings.PER_DAY
import app.kite.ui.settings.TO_HOURS
import app.kite.ui.theme.U
import app.kite.ui.theme.UType

/** Three pages: the kite, the tug, and when to be tugged. */
@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    val c = ctx()
    var page by rememberSaveable { mutableIntStateOf(0) }
    var from by rememberSaveable { mutableIntStateOf(9) }
    var to by rememberSaveable { mutableIntStateOf(21) }
    var perDay by rememberSaveable { mutableIntStateOf(5) }
    val wind = rememberWind()
    // The kite flies away on the first page, is tugged on the second, lies in the grass on the third.
    val height by animateFloatAsState(listOf(0.85f, 0.45f, 0f)[page], tween(1600), label = "kite")

    fun finish() {
        c.container.write {
            settings.update { it.copy(onboarded = true, from = from * 60, to = to * 60, perDay = perDay, tugs = true) }
            Tugs.reschedule(c)
        }
        onDone()
    }

    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { finish() }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().weight(if (page == 2) 0.7f else 1f).padding(16.dp).clip(U.RXl)) {
            Canvas(Modifier.fillMaxSize()) { scene(height, wind, 13.5f, kiteSize = 0.26f) }
        }
        AnimatedContent(page, transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(200)) }, label = "page", modifier = Modifier.padding(horizontal = 24.dp)) { p ->
            Column {
                Text(stringResource(listOf(R.string.w1_title, R.string.w2_title, R.string.w3_title)[p]), style = UType.Hero)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(listOf(R.string.w1_text, R.string.w2_text, R.string.w3_text)[p]), style = UType.Sub)
                if (p == 2) {
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.set_from), style = UType.Small)
                    Spacer(Modifier.height(6.dp))
                    Segmented(FROM_HOURS, from, { T.time(it * 60) }, { from = it })
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.set_to), style = UType.Small)
                    Spacer(Modifier.height(6.dp))
                    Segmented(TO_HOURS, to, { T.time(it * 60) }, { to = it })
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.set_per_day), style = UType.Small)
                    Spacer(Modifier.height(6.dp))
                    Segmented(PER_DAY, perDay, { it.toString() }, { perDay = it })
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { i -> Box(Modifier.size(8.dp).clip(CircleShape).background(if (i == page) U.c.kite else U.c.line)) }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            stringResource(if (page < 2) R.string.next else R.string.start),
            {
                if (page < 2) page++
                else if (Build.VERSION.SDK_INT >= 33 && !Tugs.canNotify(c)) ask.launch(Manifest.permission.POST_NOTIFICATIONS)
                else finish()
            },
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(16.dp))
    }
}
