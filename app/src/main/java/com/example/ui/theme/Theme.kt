package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val StudyShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(24.dp),
  extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun StudyPlannerTheme(
  themeMode: String = "system",
  accentTheme: String = "indigo",
  isBengali: Boolean = true,
  content: @Composable () -> Unit
) {
  val darkTheme = when (themeMode) {
    "dark" -> true
    "light" -> false
    else -> isSystemInDarkTheme()
  }

  val (primaryLight, containerLight, onContainerLight, primaryDark) = when (accentTheme) {
    "emerald" -> QuadColor(EmeraldPrimary, EmeraldContainer, EmeraldOnContainer, Color(0xFF34D399))
    "amber" -> QuadColor(AmberPrimary, AmberThemeContainer, AmberThemeOnContainer, Color(0xFFFBBF24))
    "rose" -> QuadColor(RosePrimary, RoseContainer, RoseOnContainer, Color(0xFFFB7185))
    else -> QuadColor(IndigoPrimary, IndigoContainer, IndigoOnContainer, Color(0xFF93C5FD))
  }

  val colorScheme = if (darkTheme) {
    darkColorScheme(
      primary = primaryDark,
      onPrimary = Color(0xFF0A192F),
      primaryContainer = primaryLight,
      onPrimaryContainer = Color.White,
      secondary = Color(0xFF2DD4BF),
      onSecondary = Color(0xFF042F2E),
      secondaryContainer = Color(0xFF115E59),
      onSecondaryContainer = Color(0xFFCCFBF1),
      tertiary = Color(0xFFFBBF24),
      onTertiary = Color(0xFF451A03),
      tertiaryContainer = Color(0xFF78350F),
      onTertiaryContainer = Color(0xFFFEF3C7),
      background = DarkBackground,
      onBackground = DarkOnBackground,
      surface = DarkSurface,
      onSurface = DarkOnSurface,
      surfaceVariant = DarkSurfaceVariant,
      onSurfaceVariant = DarkOnSurfaceVariant,
      outline = DarkOutline
    )
  } else {
    lightColorScheme(
      primary = primaryLight,
      onPrimary = Color.White,
      primaryContainer = containerLight,
      onPrimaryContainer = onContainerLight,
      secondary = TealSecondary,
      onSecondary = TealOnSecondary,
      secondaryContainer = TealContainer,
      onSecondaryContainer = TealOnContainer,
      tertiary = AmberTertiary,
      onTertiary = AmberOnTertiary,
      tertiaryContainer = AmberContainer,
      onTertiaryContainer = AmberOnContainer,
      background = LightBackground,
      onBackground = LightOnBackground,
      surface = LightSurface,
      onSurface = LightOnSurface,
      surfaceVariant = LightSurfaceVariant,
      onSurfaceVariant = LightOnSurfaceVariant,
      outline = LightOutline
    )
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = buildStudyTypography(isBengali),
    shapes = StudyShapes,
    content = content
  )
}

private data class QuadColor(
  val primaryLight: Color,
  val containerLight: Color,
  val onContainerLight: Color,
  val primaryDark: Color
)
