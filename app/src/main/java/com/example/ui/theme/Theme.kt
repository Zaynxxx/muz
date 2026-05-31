package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark Schemes for our immersive futuristic environments
private val CyberpunkColorScheme = darkColorScheme(
    primary = CyberpunkPrimary,
    secondary = CyberpunkSecondary,
    tertiary = CyberpunkTertiary,
    background = CyberpunkBg,
    surface = CyberpunkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFFECEEF5),
    onSurface = Color(0xFFDCDFEF)
)

private val SpaceColorScheme = darkColorScheme(
    primary = SpacePrimary,
    secondary = SpaceSecondary,
    tertiary = SpaceTertiary,
    background = SpaceBg,
    surface = SpaceSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFFAAB8FE),
    onSurface = Color(0xFFCAD1FE)
)

private val RainColorScheme = darkColorScheme(
    primary = RainPrimary,
    secondary = RainSecondary,
    tertiary = RainTertiary,
    background = RainBg,
    surface = RainSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFFD4E3FC),
    onSurface = Color(0xFFBCDEF5)
)

private val DriveColorScheme = darkColorScheme(
    primary = DrivePrimary,
    secondary = DriveSecondary,
    tertiary = DriveTertiary,
    background = DriveBg,
    surface = DriveSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFFFDECE2),
    onSurface = Color(0xFFFED7C1)
)

private val ZenColorScheme = darkColorScheme(
    primary = ZenPrimary,
    secondary = ZenSecondary,
    tertiary = ZenTertiary,
    background = ZenBg,
    surface = ZenSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFFE2F3E5),
    onSurface = Color(0xFFCFEBD2)
)

private val StudioColorScheme = darkColorScheme(
    primary = StudioPrimary,
    secondary = StudioSecondary,
    tertiary = StudioTertiary,
    background = StudioBg,
    surface = StudioSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE5E5EA),
    onSurface = Color(0xFFD1D1D6)
)

@Composable
fun NovaBeatTheme(
    spaceId: String = "cyberpunk",
    content: @Composable () -> Unit
) {
    val colorScheme = when (spaceId) {
        "cyberpunk" -> CyberpunkColorScheme
        "space" -> SpaceColorScheme
        "rain" -> RainColorScheme
        "night_drive" -> DriveColorScheme
        "meditation" -> ZenColorScheme
        "studio" -> StudioColorScheme
        "rgb_reactive" -> CyberpunkColorScheme // Will sweep hue dynamically in sliders
        "retro_cassette" -> DriveColorScheme // Vintage warmth colors
        else -> CyberpunkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
