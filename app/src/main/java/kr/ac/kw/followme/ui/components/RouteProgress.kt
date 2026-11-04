package kr.ac.kw.followme.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kr.ac.kw.followme.ui.theme.AccentPalette

/**
 * 경로 진행 상황. "몇 단계 중 몇 번째"를 막대로 보여 준다.
 *
 * @param total   경로 구간 수 (Destination.route.size)
 * @param current 현재 구간 인덱스 (0-based)
 */
@Composable
fun RouteProgress(
    total: Int,
    current: Int,
    accent: AccentPalette,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(total) { index ->
                val done = index < current
                val active = index == current
                val color by animateColorAsState(
                    targetValue = when {
                        active -> accent.main
                        done -> accent.main.copy(alpha = 0.45f)
                        else -> MaterialTheme.colorScheme.outline
                    },
                    label = "segmentColor",
                )
                Box(
                    modifier = Modifier
                        .width(if (active) 96.dp else 56.dp)
                        .height(14.dp)
                        .background(color, RoundedCornerShape(7.dp)),
                )
            }
        }
        Text(
            text = "${total}단계 중 ${(current + 1).coerceAtMost(total)}번째",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
