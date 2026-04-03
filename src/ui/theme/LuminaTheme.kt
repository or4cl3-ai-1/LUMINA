package ai.or4cl3.lumina.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * LUMINA Design System
 *
 * Design intent:
 *  - Warm, not clinical. Safe, not sterile.
 *  - Large touch targets (accessibility for young children, fine motor challenges)
 *  - High contrast for outdoor / low-light use in camp settings
 *  - Culturally neutral palette — no flags, no symbols
 *  - Agents have distinct accent colours children can learn to recognise:
 *      ARIA   — Warm amber (curiosity, learning, light)
 *      HAVEN  — Soft lavender (calm, safety, comfort)
 *      System — Teal (neutral, informational)
 */

// ── Colour tokens ───────────────────────────────────────────────────────

object LuminaColors {
    // ARIA — warm amber
    val AriaAmber        = Color(0xFFF59E0B)
    val AriaAmberLight   = Color(0xFFFEF3C7)
    val AriaAmberDark    = Color(0xFFD97706)

    // HAVEN — soft lavender
    val HavenLavender    = Color(0xFFA78BFA)
    val HavenLavenderLight = Color(0xFFEDE9FE)
    val HavenLavenderDark  = Color(0xFF7C3AED)

    // Backgrounds
    val WarmBackground   = Color(0xFFFAFAF7)
    val SurfaceWhite     = Color(0xFFFFFFFF)
    val CardSurface      = Color(0xFFF5F5F0)

    // Text
    val TextPrimary      = Color(0xFF1C1917)
    val TextSecondary    = Color(0xFF78716C)
    val TextHint         = Color(0xFFA8A29E)

    // Semantic
    val SafeGreen        = Color(0xFF10B981)
    val CautionAmber     = Color(0xFFF59E0B)
    val AlertCoral       = Color(0xFFF87171)  // Soft — not harsh red
    val NeutralTeal      = Color(0xFF14B8A6)
}

// ── Light colour scheme ─────────────────────────────────────────────────

private val LuminaLightColors = lightColorScheme(
    primary          = LuminaColors.NeutralTeal,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    secondary        = LuminaColors.HavenLavender,
    onSecondary      = Color.White,
    tertiary         = LuminaColors.AriaAmber,
    background       = LuminaColors.WarmBackground,
    surface          = LuminaColors.SurfaceWhite,
    surfaceVariant   = LuminaColors.CardSurface,
    onBackground     = LuminaColors.TextPrimary,
    onSurface        = LuminaColors.TextPrimary,
    error            = LuminaColors.AlertCoral,
    outline          = Color(0xFFE7E5E4)
)

// ── Typography ─────────────────────────────────────────────────────────
//
// Using system default (Roboto) for maximum device compatibility.
// Phase 2: embed Noto Sans for multilingual script coverage.

val LuminaTypography = Typography(
    // Agent bubble text — readable, generous line height
    bodyLarge  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,  fontSize = 17.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,  fontSize = 15.sp, lineHeight = 24.sp),
    bodySmall  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,  fontSize = 13.sp, lineHeight = 20.sp),
    // Agent name label
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,   fontSize = 11.sp),
    // Screen titles
    titleLarge  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    // Headings
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp),
)

// ── Theme entry point ────────────────────────────────────────────────────

@Composable
fun LuminaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Phase 1: light theme only — most target devices lack OLED
    MaterialTheme(
        colorScheme = LuminaLightColors,
        typography  = LuminaTypography,
        content     = content
    )
}
