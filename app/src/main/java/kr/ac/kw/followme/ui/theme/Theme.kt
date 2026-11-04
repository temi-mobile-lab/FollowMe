package kr.ac.kw.followme.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Blue700,
    onPrimary = Color.White,
    primaryContainer = Blue50,
    onPrimaryContainer = Blue800,
    secondary = Teal600,
    onSecondary = Color.White,
    secondaryContainer = Teal50,
    onSecondaryContainer = Color(0xFF00695C),
    tertiary = Green700,
    onTertiary = Color.White,
    error = Red700,
    onError = Color.White,
    errorContainer = Red50,
    onErrorContainer = Color(0xFF8E0000),
    background = Background,
    onBackground = Gray900,
    surface = Color.White,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray700,
    outline = Gray300,
    outlineVariant = Gray300,
)

private val FollowMeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/**
 * 로비 키오스크용이라 다크 테마는 두지 않는다. 항상 밝은 화면으로 고정한다.
 */
@Composable
fun FollowMeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = FollowMeTypography,
        shapes = FollowMeShapes,
        content = content,
    )
}
