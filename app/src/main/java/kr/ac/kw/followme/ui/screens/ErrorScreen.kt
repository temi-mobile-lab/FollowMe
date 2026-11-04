package kr.ac.kw.followme.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kr.ac.kw.followme.ui.components.PrimaryButton
import kr.ac.kw.followme.ui.components.StatusScreen
import kr.ac.kw.followme.ui.theme.AccentPalette
import kr.ac.kw.followme.ui.theme.FollowMeTheme
import kr.ac.kw.followme.ui.theme.Red700

/** 재시도 한도를 넘겼거나 복구 불가. 사용자에게 대체 안내를 주고 처음으로 돌린다. */
@Composable
fun ErrorScreen(
    reason: String,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatusScreen(
        modifier = modifier,
        accent = AccentPalette.Fail,
        icon = Icons.Rounded.ErrorOutline,
        eyebrow = "안내를 계속할 수 없습니다",
        title = "죄송합니다",
        subtitle = "지금은 길 안내를 마칠 수 없습니다. 가까운 안내판을 확인하거나 사무실에 문의해 주세요.",
        caption = reason,
        actions = {
            PrimaryButton(
                text = "처음으로",
                icon = Icons.Rounded.Home,
                onClick = onReset,
                containerColor = Red700,
            )
        },
    )
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun ErrorScreenPreview() {
    FollowMeTheme { ErrorScreen(reason = "goTo abort: obstacle detected (재시도 3회 초과)", onReset = {}) }
}
