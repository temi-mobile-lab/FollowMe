package kr.ac.kw.followme.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kr.ac.kw.followme.data.Destination
import kr.ac.kw.followme.data.sampleDestination
import kr.ac.kw.followme.ui.components.RouteProgress
import kr.ac.kw.followme.ui.components.SecondaryButton
import kr.ac.kw.followme.ui.components.StatusScreen
import kr.ac.kw.followme.ui.theme.AccentPalette
import kr.ac.kw.followme.ui.theme.FollowMeTheme

/** 4-1. 이동 중. */
@Composable
fun NavigatingScreen(
    dest: Destination,
    seg: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatusScreen(
        modifier = modifier,
        accent = AccentPalette.Navigate,
        icon = Icons.AutoMirrored.Rounded.DirectionsWalk,
        pulsing = true,
        eyebrow = "안내 중",
        title = dest.displayName,
        subtitle = "제 뒤를 따라와 주세요.",
        body = { RouteProgress(total = dest.route.size, current = seg, accent = AccentPalette.Navigate) },
        actions = {
            SecondaryButton(text = "안내 취소", icon = Icons.Rounded.Close, onClick = onCancel)
        },
    )
}

/** 4-2. 갈림길 경유지에서 방향을 안내하는 중. 안내 문구 자체를 가장 크게 띄운다. */
@Composable
fun WaypointGuideScreen(
    dest: Destination,
    seg: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val say = dest.route.getOrNull(seg)?.say.orEmpty().ifBlank { "계속 따라와 주세요" }
    StatusScreen(
        modifier = modifier,
        accent = AccentPalette.Navigate,
        icon = Icons.Rounded.Campaign,
        eyebrow = "${dest.displayName} 가는 길",
        title = say,
        body = { RouteProgress(total = dest.route.size, current = seg, accent = AccentPalette.Navigate) },
        actions = {
            SecondaryButton(text = "안내 취소", icon = Icons.Rounded.Close, onClick = onCancel)
        },
    )
}

/** (확장) 사용자가 따라오지 않아 제자리에서 기다리는 중. */
@Composable
fun WaitingUserScreen(
    dest: Destination,
    seg: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatusScreen(
        modifier = modifier,
        accent = AccentPalette.Recover,
        icon = Icons.Rounded.HourglassTop,
        pulsing = true,
        eyebrow = "${dest.displayName} 가는 길",
        title = "여기서 기다리고 있을게요",
        subtitle = "가까이 오시면 다시 출발합니다.",
        body = { RouteProgress(total = dest.route.size, current = seg, accent = AccentPalette.Recover) },
        actions = {
            SecondaryButton(text = "안내 취소", icon = Icons.Rounded.Close, onClick = onCancel)
        },
    )
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun NavigatingScreenPreview() {
    FollowMeTheme { NavigatingScreen(dest = sampleDestination, seg = 0, onCancel = {}) }
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun WaypointGuideScreenPreview() {
    FollowMeTheme { WaypointGuideScreen(dest = sampleDestination, seg = 1, onCancel = {}) }
}
