package kr.ac.kw.followme.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.ac.kw.followme.R
import kr.ac.kw.followme.data.Destination
import kr.ac.kw.followme.data.sampleDestination
import kr.ac.kw.followme.data.sampleDestinations
import kr.ac.kw.followme.ui.components.PrimaryButton
import kr.ac.kw.followme.ui.components.SecondaryButton
import kr.ac.kw.followme.ui.theme.FollowMeTheme
import kr.ac.kw.followme.ui.theme.Green700

@Composable
fun ArrivedScreen(
    dest: Destination,
    destinations: List<Destination>,
    onSelect: (Destination) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var choosingNext by rememberSaveable(dest.id) { mutableStateOf(false) }
    var showMap by rememberSaveable(dest.id) { mutableStateOf(dest.showFloorMap) }
    if (choosingNext) {
        SelectingScreen(
            destinations = destinations.filter { it.id != dest.id },
            onSelect = {
                choosingNext = false
                onSelect(it)
            },
            onCancel = { choosingNext = false },
            modifier = modifier,
        )
        return
    }
    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(Color(0xFFF2F7F4)),
    ) {
        val wide = maxWidth >= 760.dp
        val viewportHeight = maxHeight
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .heightIn(min = viewportHeight)
                .padding(horizontal = if (wide) 48.dp else 24.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (wide) {
                Row(
                    modifier = Modifier.widthIn(max = 1160.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ArrivalDetails(dest, { choosingNext = true }, onDone, Modifier.weight(1.25f))
                    ArrivalQrCard(showMap, dest.showFloorMap, { showMap = !showMap }, Modifier.weight(1f))
                }
            } else {
                ArrivalDetails(dest, { choosingNext = true }, onDone, Modifier.fillMaxWidth())
                ArrivalQrCard(showMap, dest.showFloorMap, { showMap = !showMap }, Modifier.widthIn(max = 460.dp).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ArrivalDetails(
    dest: Destination,
    onChooseNext: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(Green700, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Text(if (dest.viaElevator) "엘리베이터 도착" else "목적지 도착", style = MaterialTheme.typography.titleMedium, color = Green700)
        }
        Text(
            text = dest.arrivalPlaceName,
            style = MaterialTheme.typography.displaySmall,
            color = colors.onBackground,
            modifier = Modifier.padding(top = 16.dp).semantics { heading() },
        )
        val directions = dest.arrivalMessage.removePrefix("${dest.arrivalPlaceName}에 도착했습니다.").trim()
        if (directions.isNotEmpty()) {
            Text(
                text = directions,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 28.dp), color = colors.outline)
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PrimaryButton(
                text = "다른 목적지 선택",
                icon = Icons.AutoMirrored.Rounded.DirectionsWalk,
                onClick = onChooseNext,
                containerColor = Green700,
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                text = "안내 종료 · 출입문 복귀",
                icon = Icons.Rounded.Home,
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ArrivalQrCard(
    showMap: Boolean,
    canSwitch: Boolean,
    onSwitch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = if (showMap) "새빛관 층별 안내도" else "길 안내 설문"
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = Color(0xFF184C40),
        contentColor = Color.White,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(30.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Surface(
                modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth().aspectRatio(1f),
                shape = MaterialTheme.shapes.medium,
                color = Color.White,
            ) {
                Image(
                    bitmap = ImageBitmap.imageResource(if (showMap) R.drawable.floor_map_qr else R.drawable.survey_qr),
                    contentDescription = "$title QR 코드",
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    filterQuality = FilterQuality.None,
                )
            }
            Text(
                "휴대폰 카메라로 스캔해 주세요.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            if (canSwitch) {
                TextButton(onClick = onSwitch) {
                    Text(if (showMap) "길 안내 설문 보기" else "층별 안내도 보기", color = Color.White)
                }
            }
        }
    }
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun ArrivedScreenPreview() {
    FollowMeTheme {
        ArrivedScreen(dest = sampleDestination, destinations = sampleDestinations, onSelect = {}, onDone = {})
    }
}
