package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val BengaliDisplayFamily = FontFamily(
  Font(R.font.hind_siliguri, FontWeight.Normal),
  Font(R.font.hind_siliguri, FontWeight.Medium),
  Font(R.font.hind_siliguri, FontWeight.SemiBold),
  Font(R.font.hind_siliguri, FontWeight.Bold)
)

val BengaliBodyFamily = FontFamily(
  Font(R.font.noto_sans_bengali, FontWeight.Normal),
  Font(R.font.noto_sans_bengali, FontWeight.Medium),
  Font(R.font.noto_sans_bengali, FontWeight.SemiBold),
  Font(R.font.noto_sans_bengali, FontWeight.Bold)
)

val LatinDisplayFamily = FontFamily(
  Font(R.font.plus_jakarta_sans, FontWeight.Normal),
  Font(R.font.plus_jakarta_sans, FontWeight.Medium),
  Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
  Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

fun buildStudyTypography(isBengali: Boolean): Typography {
  val displayFamily = if (isBengali) BengaliDisplayFamily else LatinDisplayFamily
  val bodyFamily = if (isBengali) BengaliBodyFamily else LatinDisplayFamily

  return Typography(
    displayLarge = TextStyle(
      fontFamily = displayFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 40.sp,
      lineHeight = 48.sp
    ),
    displaySmall = TextStyle(
      fontFamily = displayFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 30.sp,
      lineHeight = 38.sp
    ),
    headlineMedium = TextStyle(
      fontFamily = displayFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 24.sp,
      lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
      fontFamily = displayFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 20.sp,
      lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
      fontFamily = displayFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 18.sp,
      lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
      fontFamily = bodyFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 16.sp,
      lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
      fontFamily = bodyFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 14.sp,
      lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
      fontFamily = bodyFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 15.sp,
      lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
      fontFamily = bodyFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 14.sp,
      lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
      fontFamily = bodyFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 12.sp,
      lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
      fontFamily = bodyFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 14.sp,
      lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
      fontFamily = bodyFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 12.sp,
      lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
      fontFamily = bodyFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 11.sp,
      lineHeight = 14.sp
    )
  )
}
