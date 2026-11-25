package kr.ac.kw.followme.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AssistantDirection
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.ac.kw.followme.ui.components.PrimaryButton
import kr.ac.kw.followme.ui.components.SecondaryButton
import kr.ac.kw.followme.ui.theme.Blue50
import kr.ac.kw.followme.ui.theme.Blue700
import kr.ac.kw.followme.ui.theme.FollowMeTheme

/**
 * 1. 대기 화면. 로비에서 상시 표시된다.
 * 버튼뿐 아니라 화면 아무 곳이나 눌러도 안내가 시작되도록 했다.
 */
@Composable
fun IdleScreen(
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    onVoiceSetup: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Blue50, MaterialTheme.colorScheme.background))
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onStart,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 48.dp, vertical = 36.dp),
        ) {
            val transition = rememberInfiniteTransition(label = "idle")
            val scale by transition.animateFloat(
                initialValue = 1f,
                targetValue = 1.06f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "idleScale",
            )

            Box(
                modifier = Modifier
                    .scale(scale)
                    .size(168.dp)
                    .background(Blue700, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.AssistantDirection,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(96.dp),
                )
            }

            Text(
                text = "길 안내가 필요하신가요?",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 36.dp),
            )

            Text(
                text = if (onVoiceSetup == null) "“엘리베이터로 안내해 줘”라고 말씀해 보세요."
                    else "음성 안내를 사용하려면 테미의 키오스크 앱으로 Follow Me를 선택해 주세요.",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp),
            )

            PrimaryButton(
                text = "길 안내 시작",
                icon = Icons.Rounded.TouchApp,
                onClick = onStart,
                modifier = Modifier.padding(top = 44.dp),
            )
            if (onVoiceSetup != null) {
                SecondaryButton(
                    text = "음성 안내 설정",
                    onClick = onVoiceSetup,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            Text(
                text = "화면을 아무 곳이나 눌러도 시작됩니다",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 32.dp),
            )
        }
    }
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun IdleScreenPreview() {
    FollowMeTheme { IdleScreen(onStart = {}) }
}
