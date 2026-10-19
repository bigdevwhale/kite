package app.kite.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import app.kite.R
import app.kite.domain.Where
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Words and dates in the app's language. Plain functions over a [Context] so notifications can use them too. */
object T {
    private fun locale(c: Context): Locale = c.resources.configuration.locales[0]

    fun plural(c: Context, id: Int, n: Int): String = c.resources.getQuantityString(id, n, n)

    /** "Saturday, 17 October". */
    fun weekday(c: Context, d: LocalDate): String =
        d.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale(c))).replaceFirstChar { it.titlecase(locale(c)) }

    /** "Sat", under a small sky. */
    fun dow(c: Context, d: LocalDate): String =
        d.format(DateTimeFormatter.ofPattern("EEE", locale(c))).trimEnd('.').replaceFirstChar { it.titlecase(locale(c)) }

    /** "19:30". */
    fun time(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

    fun time(t: LocalDateTime): String = time(t.hour * 60 + t.minute)

    fun where(c: Context, w: Where): String = c.getString(
        when (w) {
            Where.HERE -> R.string.w_here
            Where.PAST -> R.string.w_past
            Where.FUTURE -> R.string.w_future
            Where.CARES -> R.string.w_cares
        }
    )

    fun whereSub(c: Context, w: Where): String = c.getString(
        when (w) {
            Where.HERE -> R.string.w_here_sub
            Where.PAST -> R.string.w_past_sub
            Where.FUTURE -> R.string.w_future_sub
            Where.CARES -> R.string.w_cares_sub
        }
    )

    /** "Today you drifted most into the future." */
    fun drift(c: Context, w: Where): String = c.getString(
        when (w) {
            Where.HERE -> R.string.day_here
            Where.PAST -> R.string.day_past
            Where.FUTURE -> R.string.day_future
            Where.CARES -> R.string.day_cares
        }
    )
}

/** In-app language switching (persisted by AppCompat; shows up in Android 13+ per-app language settings). */
object Language {
    const val SYSTEM = ""
    const val EN = "en"
    const val RU = "ru"

    fun current(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')

    fun set(tag: String) {
        AppCompatDelegate.setApplicationLocales(if (tag == SYSTEM) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag))
    }
}

@Composable
fun ctx(): Context = LocalContext.current
