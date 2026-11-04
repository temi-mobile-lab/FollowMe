package kr.ac.kw.followme.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

private val Default = FontFamily.Default

private val koLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun style(size: Int, lineHeight: Int, weight: FontWeight, letter: Double = -0.5) = TextStyle(
    fontFamily = Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letter.sp,
    lineHeightStyle = koLineHeight,
)

/**
 * 10.1인치 로봇 태블릿에서 1~2m 떨어져 서서 읽는 것을 기준으로 한 타입 스케일.
 * Material 기본값보다 전체적으로 두 단계 정도 크다.
 */
val FollowMeTypography = Typography(
    displayLarge = style(88, 100, FontWeight.Bold, -2.0),
    displayMedium = style(68, 80, FontWeight.Bold, -1.5),
    displaySmall = style(54, 66, FontWeight.Bold, -1.0),
    headlineLarge = style(46, 58, FontWeight.Bold),
    headlineMedium = style(38, 50, FontWeight.SemiBold),
    headlineSmall = style(32, 44, FontWeight.SemiBold),
    titleLarge = style(30, 42, FontWeight.SemiBold),
    titleMedium = style(26, 36, FontWeight.Medium),
    titleSmall = style(22, 32, FontWeight.Medium),
    bodyLarge = style(28, 42, FontWeight.Normal, 0.0),
    bodyMedium = style(24, 36, FontWeight.Normal, 0.0),
    bodySmall = style(20, 30, FontWeight.Normal, 0.0),
    labelLarge = style(30, 36, FontWeight.Bold),
    labelMedium = style(24, 30, FontWeight.Medium),
    labelSmall = style(20, 26, FontWeight.Medium),
)
