package app.kite

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import app.kite.data.DemoSeed
import app.kite.data.KiteDatabase
import app.kite.data.SettingsStore
import app.kite.notify.Tugs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val db = KiteDatabase.create(context)
    val settings = SettingsStore(context)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Where a tap from outside wants to go ("where" from a tug, "self" from the widget) and when. */
    val open = MutableStateFlow<Pair<String, Long>?>(null)

    /** Fire-and-forget so a write survives the screen being closed right after tapping. */
    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch { block() }
    }
}

class KiteApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Tugs.ensureChannels(this)
        container.scope.launch {
            container.settings.ensureSalt()
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(container)
            Tugs.reschedule(this@KiteApp)
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as KiteApp).container

/** Resources in the in-app language, for notifications, which AppCompat doesn't localise before Android 13. */
fun Context.localized(): Context {
    val tags = AppCompatDelegate.getApplicationLocales()
    if (tags.isEmpty) return this
    val config = Configuration(resources.configuration)
    config.setLocale(tags[0])
    return createConfigurationContext(config)
}

/** Is this context in Russian? Prompts have both languages and pick by this. */
val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"
