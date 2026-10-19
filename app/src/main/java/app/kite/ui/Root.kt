package app.kite.ui

import android.app.Activity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.kite.MainActivity
import app.kite.container
import app.kite.data.Pull
import app.kite.data.Settings
import app.kite.domain.Day
import app.kite.notify.Tugs
import app.kite.ru
import app.kite.ui.components.rememberNow
import app.kite.ui.home.HomeScreen
import app.kite.ui.onboarding.WelcomeScreen
import app.kite.ui.pull.ReelScreen
import app.kite.ui.pull.WhereScreen
import app.kite.ui.settings.SettingsScreen
import app.kite.ui.sky.SkyScreen
import app.kite.ui.theme.U
import java.time.LocalDate
import java.time.ZoneId

/** Everything stored, plus what is derived from it. Screens read through here. */
@Immutable
class Home(
    val pulls: List<Pull>,
    val settings: Settings,
    val ru: Boolean,
) {
    val zone: ZoneId = ZoneId.systemDefault()
    val today: LocalDate = LocalDate.now(zone)

    private val byDay = pulls.groupBy { it.day(zone) }

    fun day(d: LocalDate) = Day(d, byDay[d].orEmpty())
    fun pull(id: Long) = pulls.firstOrNull { it.id == id }
    val first: LocalDate get() = pulls.firstOrNull()?.day(zone) ?: today
}

/**
 * Everything a screen can open. [outside]: the screen was opened by a tug or the widget, so when it's
 * done the app steps aside and you're back where you were.
 */
class Go(private val nav: NavHostController, private val activity: Activity?) {
    fun where(self: Boolean, outside: Boolean = false) = nav.navigate("where/${self.int}/${outside.int}") { launchSingleTop = true }
    fun reel(id: Long, outside: Boolean) = nav.navigate("reel/$id/${outside.int}") { popUpTo("main") }
    fun sky(day: LocalDate) = nav.navigate("sky/${day.toEpochDay()}") { launchSingleTop = true }
    fun settings() = nav.navigate("settings") { launchSingleTop = true }
    fun back() { nav.popBackStack() }

    /** Done with a pull: home, or out of the way if a tug brought you here. */
    fun done(outside: Boolean) {
        nav.popBackStack("main", false)
        if (outside) activity?.moveTaskToBack(true)
    }

    private val Boolean.int get() = if (this) 1 else 0
}

@Composable
fun KiteRoot(settings: Settings) {
    val context = LocalContext.current
    val container = context.container
    val nav = rememberNavController()
    val start = remember { if (settings.onboarded) "main" else "welcome" }

    val pulls by container.db.dao().observe().collectAsStateWithLifecycle(initialValue = null)
    // Past midnight the day rolls over.
    val tick = rememberNow(60_000) / 3_600_000
    val ru = context.ru
    val home = remember(pulls, settings, tick, ru) { Home(pulls.orEmpty(), settings, ru) }
    val go = remember(nav) { Go(nav, context as? Activity) }

    // Onboarded elsewhere (the demo seed): leave the welcome screens.
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    LaunchedEffect(settings.onboarded, route) {
        if (settings.onboarded && route == "welcome") nav.navigate("main") { popUpTo("welcome") { inclusive = true } }
    }
    val open by container.open.collectAsStateWithLifecycle()
    LaunchedEffect(open, settings.onboarded) {
        val (what, _) = open ?: return@LaunchedEffect
        if (!settings.onboarded) return@LaunchedEffect
        container.open.value = null
        Tugs.clear(context)
        nav.popBackStack("main", false)
        go.where(self = what == MainActivity.OPEN_SELF, outside = true)
    }

    Box(Modifier.fillMaxSize().background(U.c.bg)) {
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.background(U.c.bg),
            enterTransition = { fadeIn(tween(260)) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(260)) },
            popExitTransition = { fadeOut(tween(180)) },
        ) {
            composable("welcome") { WelcomeScreen { nav.navigate("main") { popUpTo("welcome") { inclusive = true } } } }
            composable("main") { HomeScreen(home, go) }
            composable(
                "where/{self}/{outside}",
                arguments = listOf(navArgument("self") { type = NavType.IntType }, navArgument("outside") { type = NavType.IntType }),
            ) { e ->
                WhereScreen(e.arguments?.getInt("self") == 1, e.arguments?.getInt("outside") == 1, go)
            }
            composable(
                "reel/{id}/{outside}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }, navArgument("outside") { type = NavType.IntType }),
            ) { e ->
                ReelScreen(e.arguments?.getLong("id") ?: 0L, e.arguments?.getInt("outside") == 1, home, go)
            }
            composable("sky/{day}", arguments = listOf(navArgument("day") { type = NavType.LongType })) { e ->
                SkyScreen(LocalDate.ofEpochDay(e.arguments?.getLong("day") ?: 0L), home, go)
            }
            composable("settings") { SettingsScreen(home, go) }
        }
    }
}
