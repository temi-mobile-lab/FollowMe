package kr.ac.kw.followme.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kr.ac.kw.followme.ui.components.StatusScreen
import kr.ac.kw.followme.ui.theme.AccentPalette
import kr.ac.kw.followme.ui.theme.FollowMeTheme

/** 6. 복귀 중. 버튼을 두지 않아 사용자가 로봇을 붙잡지 않도록 했다. */
@Composable
fun ReturningScreen(modifier: Modifier = Modifier) {
    StatusScreen(
        modifier = modifier,
        accent = AccentPalette.Idle,
        icon = Icons.Rounded.Home,
        pulsing = true,
        title = "안내를 마칩니다",
        subtitle = "대기 장소로 돌아가는 중입니다. 이용해 주셔서 감사합니다.",
    )
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun ReturningScreenPreview() {
    FollowMeTheme { ReturningScreen() }
}
