package app.kite.ui.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as SystemSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.kite.R
import app.kite.container
import app.kite.data.Settings
import app.kite.notify.Tugs
import app.kite.ui.Go
import app.kite.ui.Home
import app.kite.ui.Language
import app.kite.ui.T
import app.kite.ui.components.Chip
import app.kite.ui.components.InfoRow
import app.kite.ui.components.SectionTitle
import app.kite.ui.components.Segmented
import app.kite.ui.components.Toggle
import app.kite.ui.components.TopBar
import app.kite.ui.components.UCard
import app.kite.ui.ctx
import app.kite.ui.theme.U
import app.kite.ui.theme.UType

/** The hours a tug can come in: from one of the morning hours to one of the evening ones. */
val FROM_HOURS = listOf(7, 8, 9, 10, 11)
val TO_HOURS = listOf(18, 19, 20, 21, 22)
val PER_DAY = listOf(3, 5, 7, 9)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(home: Home, go: Go) {
    val c = ctx()
    val s = home.settings

    fun set(change: (Settings) -> Settings) = c.container.write {
        settings.update(change)
        Tugs.reschedule(c)
    }

    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) Tugs.show(c) }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.settings), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            SectionTitle(stringResource(R.string.set_tugs))
            UCard {
                Toggle(stringResource(R.string.set_tugs_on), stringResource(R.string.set_tugs_on_sub), s.tugs) { v -> set { it.copy(tugs = v, quietUntil = 0) } }
                if (s.tugs) {
                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.set_per_day), style = UType.Strong)
                    Text(stringResource(R.string.set_per_day_sub), style = UType.Muted)
                    Spacer(Modifier.height(10.dp))
                    Segmented(PER_DAY, s.perDay, { it.toString() }, { n -> set { it.copy(perDay = n) } })
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.set_from), style = UType.Strong)
                    Spacer(Modifier.height(8.dp))
                    Segmented(FROM_HOURS, s.from / 60, { T.time(it * 60) }, { h -> set { it.copy(from = h * 60) } })
                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.set_to), style = UType.Strong)
                    Spacer(Modifier.height(8.dp))
                    Segmented(TO_HOURS, s.to / 60, { T.time(it * 60) }, { h -> set { it.copy(to = h * 60) } })
                }
            }

            SectionTitle(stringResource(R.string.set_feel))
            UCard {
                Toggle(stringResource(R.string.set_haptics), stringResource(R.string.set_haptics_sub), s.haptics) { v -> set { it.copy(haptics = v) } }
                InfoRow(Icons.Rounded.Vibration, stringResource(R.string.set_try_sub), stringResource(R.string.set_try)) {
                    if (Tugs.canNotify(c)) Tugs.show(c)
                    else if (Build.VERSION.SDK_INT >= 33) ask.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                InfoRow(Icons.Rounded.BatteryAlert, stringResource(R.string.set_battery_sub), stringResource(R.string.set_battery)) {
                    val app = Intent(SystemSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", c.packageName, null))
                    try {
                        c.startActivity(Intent(SystemSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    } catch (_: ActivityNotFoundException) {
                        c.startActivity(app)
                    }
                }
            }

            SectionTitle(stringResource(R.string.set_language))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val cur = Language.current()
                Chip(stringResource(R.string.set_lang_system), cur == Language.SYSTEM, { Language.set(Language.SYSTEM) })
                Chip("English", cur == Language.EN, { Language.set(Language.EN) })
                Chip("Русский", cur == Language.RU, { Language.set(Language.RU) })
            }

            SectionTitle(stringResource(R.string.set_about))
            Text(stringResource(R.string.set_privacy), style = UType.Sub)
            Spacer(Modifier.height(28.dp))
        }
    }
}
