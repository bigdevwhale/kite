package app.kite

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import app.kite.data.DemoSeed
import app.kite.ui.Go
import app.kite.ui.Home
import app.kite.ui.art.Art
import app.kite.ui.home.HomeScreen
import app.kite.ui.onboarding.WelcomeScreen
import app.kite.ui.pull.ReelScreen
import app.kite.ui.pull.WhereScreen
import app.kite.ui.settings.SettingsScreen
import app.kite.ui.sky.SkyScreen
import app.kite.ui.theme.KiteTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every main screen over the demo weeks, drawn to PNGs (run with `-Prender`), English and Russian: a look without a phone. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }

    @Before fun still() {
        Art.still = true
        Art.fixedHour = 13.5f
    }

    private fun home(): Home = runBlocking {
        val app = ApplicationProvider.getApplicationContext<KiteApp>()
        val c = app.container
        DemoSeed.seedIfEmpty(c)
        Home(c.db.dao().all(), c.settings.now(), app.ru)
    }

    private fun shot(name: String, h: Home, settle: Long = 3_000, content: @Composable (Home, Go) -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val nav = rememberNavController()
            KiteTheme { content(h, Go(nav, null)) }
        }
        rule.mainClock.advanceTimeBy(settle)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun welcome() = home().let { shot("0-welcome", it) { _, _ -> WelcomeScreen {} } }
    @Test fun main() = home().let { shot("1-home", it) { h, go -> HomeScreen(h, go) } }
    @Test fun where() = home().let { shot("2-where", it) { _, go -> WhereScreen(self = false, outside = false, go) } }
    @Test fun reel() = home().let { h -> shot("3-reel", h) { hh, go -> ReelScreen(hh.pulls.last { it.whereOf.away }.id, false, hh, go) } }
    @Test fun sky() = home().let { h ->
        // The day with the most varied sky.
        val day = h.pulls.groupBy { it.day() }.filterKeys { it != h.today }.maxBy { (_, p) -> p.map { it.whereOf }.distinct().size * 10 + p.size }.key
        shot("4-sky", h) { hh, go -> SkyScreen(day, hh, go) }
    }
    @Test fun settings() = home().let { shot("5-settings", it) { h, go -> SettingsScreen(h, go) } }
}

class RenderArtScreensEn : ScreensBase() {
    override val lang = "en"
}

@Config(qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderArtScreensRu : ScreensBase() {
    override val lang = "ru"
}
