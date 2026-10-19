package app.kite

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kite.data.Settings
import app.kite.ui.KiteRoot
import app.kite.ui.theme.KiteTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val style = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        if (savedInstanceState == null) opened(intent)
        setContent {
            val settings: Settings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            KiteTheme {
                // Wait for settings so the welcome screens don't flash for someone already here.
                settings?.let { KiteRoot(it) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        opened(intent)
    }

    /** A tap on a tug or the widget: remember where it wants to go. */
    private fun opened(intent: Intent?) {
        val open = intent?.getStringExtra(EXTRA_OPEN) ?: return
        container.open.value = open to System.currentTimeMillis()
    }

    companion object {
        const val EXTRA_OPEN = "open"
        const val OPEN_WHERE = "where"
        const val OPEN_SELF = "self"
    }
}
