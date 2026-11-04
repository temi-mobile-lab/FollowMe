package kr.ac.kw.followme.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Done
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
import kr.ac.kw.followme.ui.components.StatusScreen
import kr.ac.kw.followme.ui.theme.AccentPalette
import kr.ac.kw.followme.ui.theme.FollowMeTheme
import kr.ac.kw.followme.ui.theme.Green700

/**
 * 5. 도착 화면.
 * 계단·엘리베이터처럼 로봇이 갈 수 없는 구간은 arrivalMessage 에 적어 두고 여기서 말로 안내한다.
 */
@Composable
fun ArrivedScreen(
    dest: Destination,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatusScreen(
        modifier = modifier,
        accent = AccentPalette.Arrive,
        icon = Icons.Rounded.CheckCircle,
        eyebrow = "도착했습니다",
        title = dest.displayName,
        body = {
            if (dest.arrivalMessage.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 920.dp)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.shapes.medium,
                        )
                        .padding(horizontal = 40.dp, vertical = 28.dp),
                ) {
                    Text(
                        text = dest.arrivalMessage,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
        actions = {
            PrimaryButton(
                text = "안내 완료",
                icon = Icons.Rounded.Done,
                onClick = onDone,
                containerColor = Green700,
            )
        },
        caption = "완료를 누르면 로봇은 대기 장소로 돌아갑니다",
    )
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun ArrivedScreenPreview() {
    FollowMeTheme { ArrivedScreen(dest = sampleDestination, onDone = {}) }
}
