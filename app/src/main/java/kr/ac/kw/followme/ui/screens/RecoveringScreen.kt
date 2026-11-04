package kr.ac.kw.followme.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kr.ac.kw.followme.data.Destination
import kr.ac.kw.followme.data.sampleDestination
import kr.ac.kw.followme.ui.components.SecondaryButton
import kr.ac.kw.followme.ui.components.StatusScreen
import kr.ac.kw.followme.ui.theme.AccentPalette
import kr.ac.kw.followme.ui.theme.FollowMeTheme

/**
 * goTo 가 abort 된 뒤 재시도 준비 중. 사용자에게는 "기다려 달라"는 말만 보여 준다.
 *
 * @param retry   지금까지의 재시도 횟수
 * @param maxRetry 최대 재시도 횟수
 */
@Composable
fun RecoveringScreen(
    dest: Destination,
    retry: Int,
    maxRetry: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatusScreen(
        modifier = modifier,
        accent = AccentPalette.Recover,
        icon = Icons.Rounded.Autorenew,
        pulsing = true,
        eyebrow = "${dest.displayName} 가는 길",
        title = "잠시만 기다려주세요",
        subtitle = "경로를 다시 확인하고 있습니다.",
        caption = "재시도 $retry / $maxRetry",
        actions = {
            SecondaryButton(text = "안내 취소", icon = Icons.Rounded.Close, onClick = onCancel)
        },
    )
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun RecoveringScreenPreview() {
    FollowMeTheme {
        RecoveringScreen(dest = sampleDestination, retry = 1, maxRetry = 3, onCancel = {})
    }
}
