package kr.ac.kw.followme.ui.theme

import androidx.compose.ui.graphics.Color

// 로봇 태블릿은 밝은 로비에서 서서 보기 때문에 명도 대비를 크게 잡았다.
val Blue700 = Color(0xFF1565C0)
val Blue800 = Color(0xFF0D47A1)
val Blue50 = Color(0xFFE8F1FB)

val Teal600 = Color(0xFF00897B)
val Teal50 = Color(0xFFE0F2F1)

val Green700 = Color(0xFF2E7D32)
val Green50 = Color(0xFFE8F5E9)

val Orange700 = Color(0xFFEF6C00)
val Orange50 = Color(0xFFFFF3E0)

val Red700 = Color(0xFFC62828)
val Red50 = Color(0xFFFFEBEE)

val Gray900 = Color(0xFF17202A)
val Gray700 = Color(0xFF4A5663)
val Gray300 = Color(0xFFD4DAE1)
val Gray100 = Color(0xFFF1F4F8)
val Background = Color(0xFFF4F6F9)

/**
 * 상태별 강조색. 사용자가 멀리서도 "지금 어떤 단계인지"를 색으로 먼저 알아채게 한다.
 */
data class AccentPalette(
    val main: Color,
    val container: Color,
    val onMain: Color = Color.White,
) {
    companion object {
        val Idle = AccentPalette(Blue700, Blue50)
        val Select = AccentPalette(Blue700, Blue50)
        val Navigate = AccentPalette(Teal600, Teal50)
        val Arrive = AccentPalette(Green700, Green50)
        val Recover = AccentPalette(Orange700, Orange50)
        val Fail = AccentPalette(Red700, Red50)
    }
}
