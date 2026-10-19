package app.kite.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.kite.R
import app.kite.domain.Where

/**
 * A field on a windy afternoon: rice-paper ground, ink-blue letters, one red paper kite. Everything
 * is light and plain so the sky can do the talking.
 */
@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    /** The kite's red: for the one thing to do. */
    val kite: Color,
    val kiteSoft: Color,
    val onKite: Color,
    val grass: Color,
)

val Field = Palette(
    bg = Color(0xFFF5F1E8),
    surface = Color(0xFFFCFAF5),
    surface2 = Color(0xFFEDE7DA),
    line = Color(0xFFDDD5C5),
    ink = Color(0xFF1F2B38),
    muted = Color(0xFF5B6876),
    faint = Color(0xFF959DA5),
    kite = Color(0xFFD4543D),
    kiteSoft = Color(0xFFF5DDD4),
    onKite = Color(0xFFFFFBF6),
    grass = Color(0xFF6E9E5E),
)

/** A colour for each place the mind goes: grass for here, dusk for the past, sunrise for the future, rose for cares. */
object Places {
    val here = Color(0xFF5E9B5C)
    val past = Color(0xFF6C77B6)
    val future = Color(0xFFE29A36)
    val cares = Color(0xFFC65F7B)

    fun of(w: Where): Color = when (w) {
        Where.HERE -> here
        Where.PAST -> past
        Where.FUTURE -> future
        Where.CARES -> cares
    }
}

val LocalPalette = staticCompositionLocalOf { Field }

/** Lora: a calm book face, for the few words that matter. */
val Serif = FontFamily(
    Font(R.font.lora_regular, FontWeight.Normal),
    Font(R.font.lora_medium, FontWeight.Medium),
    Font(R.font.lora_italic, FontWeight.Normal, FontStyle.Italic),
)

/** Nunito: round and soft, for everything else. */
val Sans = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
)

object U {
    val c: Palette @Composable get() = LocalPalette.current

    val RXl = RoundedCornerShape(26.dp)
    val RLg = RoundedCornerShape(20.dp)
    val RMd = RoundedCornerShape(16.dp)
    val RSm = RoundedCornerShape(12.dp)
}

object UType {
    val Hero: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 36.sp, lineHeight = 42.sp, color = U.c.ink)
    val Display: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 34.sp, color = U.c.ink)
    val Title: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp, color = U.c.ink)
    /** The thing to notice, as it is asked. */
    val Prompt: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontStyle = FontStyle.Italic, fontSize = 27.sp, lineHeight = 35.sp, color = U.c.ink)
    val Sub: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 23.sp, color = U.c.muted)
    val Eyebrow: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, letterSpacing = 0.14.em, color = U.c.faint)
    val Strong: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 16.5.sp, color = U.c.ink)
    val Body: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 23.sp, color = U.c.ink)
    val Muted: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 20.sp, color = U.c.muted)
    val Small: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = U.c.faint)
    val Button: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 16.sp)
}

@Composable
fun KiteTheme(content: @Composable () -> Unit) {
    val p = Field
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = p.kite,
                onPrimary = p.onKite,
                primaryContainer = p.kiteSoft,
                onPrimaryContainer = p.ink,
                secondary = p.kite,
                background = p.bg,
                surface = p.surface,
                surfaceContainer = p.surface,
                surfaceContainerHigh = p.surface2,
                surfaceContainerHighest = p.surface2,
                surfaceContainerLow = p.surface,
                onBackground = p.ink,
                onSurface = p.ink,
                onSurfaceVariant = p.muted,
                outline = p.line,
                outlineVariant = p.line,
            ),
            typography = MaterialTheme.typography.copy(
                bodyLarge = UType.Body,
                bodyMedium = UType.Body.copy(fontSize = 15.sp),
                labelLarge = UType.Button,
            ),
            content = content,
        )
    }
}
