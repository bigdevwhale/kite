package app.kite.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val onboarded: Boolean = false,
    /** The tugs at all; off lets the string go slack. */
    val tugs: Boolean = true,
    /** The window tugs fall in, minutes after midnight. */
    val from: Int = 9 * 60,
    val to: Int = 21 * 60,
    val perDay: Int = 5,
    /** No tugs before this moment (epoch ms): "quiet for an hour", "quiet till tomorrow". */
    val quietUntil: Long = 0,
    /** Spool clicks under the finger while reeling. */
    val haptics: Boolean = true,
    /** Mixed into the plan so no two phones are tugged at the same moments. */
    val salt: Long = 0,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        tugs = this[TUGS] ?: true,
        from = this[FROM] ?: (9 * 60),
        to = this[TO] ?: (21 * 60),
        perDay = this[PER_DAY] ?: 5,
        quietUntil = this[QUIET_UNTIL] ?: 0,
        haptics = this[HAPTICS] ?: true,
        salt = this[SALT] ?: 0,
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[TUGS] = s.tugs
            it[FROM] = s.from
            it[TO] = s.to
            it[PER_DAY] = s.perDay
            it[QUIET_UNTIL] = s.quietUntil
            it[HAPTICS] = s.haptics
            it[SALT] = s.salt
        }
    }

    /** A salt is picked once, on the first launch. */
    suspend fun ensureSalt() {
        if (now().salt == 0L) update { it.copy(salt = (1L..Long.MAX_VALUE / 2).random()) }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val TUGS = booleanPreferencesKey("tugs")
        val FROM = intPreferencesKey("from")
        val TO = intPreferencesKey("to")
        val PER_DAY = intPreferencesKey("per_day")
        val QUIET_UNTIL = longPreferencesKey("quiet_until")
        val HAPTICS = booleanPreferencesKey("haptics")
        val SALT = longPreferencesKey("salt")
    }
}
