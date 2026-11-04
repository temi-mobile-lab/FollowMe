package kr.ac.kw.followme.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.ac.kw.followme.data.Destination
import kr.ac.kw.followme.data.sampleDestination
import kr.ac.kw.followme.ui.components.PrimaryButton
import kr.ac.kw.followme.ui.components.SecondaryButton
import kr.ac.kw.followme.ui.components.StatusScreen
import kr.ac.kw.followme.ui.theme.AccentPalette
import kr.ac.kw.followme.ui.theme.FollowMeTheme

/**
 * 3. 확인 화면. 여기서 "안내 시작"을 누른 시점이 평가용 소요 시간 측정의 시작점이다.
 */
@Composable
fun ConfirmingScreen(
    dest: Destination,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatusScreen(
        modifier = modifier,
        accent = AccentPalette.Select,
        icon = Icons.Rounded.Place,
        eyebrow = "선택한 목적지",
        title = dest.displayName,
        subtitle = "선택한 목적지로 안내합니다. 따라오세요.",
        body = {
            Text(
                text = "안내 구간 ${dest.route.size}단계 · 천천히 이동합니다",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        },
        actions = {
            SecondaryButton(text = "다시 선택", icon = Icons.AutoMirrored.Rounded.ArrowBack, onClick = onBack)
            PrimaryButton(text = "안내 시작", icon = Icons.AutoMirrored.Rounded.DirectionsWalk, onClick = onConfirm)
        },
    )
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun ConfirmingScreenPreview() {
    FollowMeTheme { ConfirmingScreen(dest = sampleDestination, onConfirm = {}, onBack = {}) }
}
