package kr.ac.kw.followme.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kr.ac.kw.followme.data.Destination
import kr.ac.kw.followme.guide.GuideEvent
import kr.ac.kw.followme.guide.GuideState
import kr.ac.kw.followme.guide.GuideTransitions
import kr.ac.kw.followme.ui.screens.ArrivedScreen
import kr.ac.kw.followme.ui.screens.ConfirmingScreen
import kr.ac.kw.followme.ui.screens.ErrorScreen
import kr.ac.kw.followme.ui.screens.IdleScreen
import kr.ac.kw.followme.ui.screens.NavigatingScreen
import kr.ac.kw.followme.ui.screens.RecoveringScreen
import kr.ac.kw.followme.ui.screens.ReturningScreen
import kr.ac.kw.followme.ui.screens.SelectingScreen
import kr.ac.kw.followme.ui.screens.WaitingUserScreen
import kr.ac.kw.followme.ui.screens.WaypointGuideScreen
import kr.ac.kw.followme.ui.theme.FollowMeTheme

/**
 * 화면 라우팅은 오직 GuideState 만 보고 결정한다.
 * UI 는 어떤 상태로 가야 하는지 판단하지 않고, 사용자의 행동을 GuideEvent 로 올려보내기만 한다.
 *
 * @param onSimulateVoice 로봇 마이크 없이(에뮬레이터) 음성 입력을 흉내 낼 때만 넘긴다.
 *   null 이면(= 실제 temi 배포 시) 디버그용 입력창 자체가 보이지 않는다.
 */
@Composable
fun FollowMeApp(
    state: GuideState,
    destinations: List<Destination>,
    onEvent: (GuideEvent) -> Unit,
    modifier: Modifier = Modifier,
    maxRetry: Int = GuideTransitions.MAX_RETRY,
    onSimulateVoice: ((String) -> Unit)? = null,
) {
    FollowMeTheme {
        Surface(modifier = modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                // seg 만 바뀔 때는 화면 전체가 깜빡이지 않도록 상태 이름으로만 전환을 건다.
                Crossfade(
                    targetState = state.name,
                    animationSpec = tween(220),
                    label = "screen",
                ) { _ ->
                    when (val s = state) {
                        is GuideState.Idle ->
                            IdleScreen(onStart = { onEvent(GuideEvent.StartRequested) })

                        is GuideState.Selecting ->
                            SelectingScreen(
                                destinations = destinations,
                                onSelect = { onEvent(GuideEvent.DestinationSelected(it)) },
                                onCancel = { onEvent(GuideEvent.UserCancelled) },
                            )

                        is GuideState.Confirming ->
                            ConfirmingScreen(
                                dest = s.dest,
                                onConfirm = { onEvent(GuideEvent.UserConfirmed) },
                                onBack = { onEvent(GuideEvent.UserCancelled) },
                            )

                        is GuideState.Navigating ->
                            NavigatingScreen(
                                dest = s.dest,
                                seg = s.seg,
                                onCancel = { onEvent(GuideEvent.UserCancelled) },
                            )

                        is GuideState.WaypointGuide ->
                            WaypointGuideScreen(
                                dest = s.dest,
                                seg = s.seg,
                                onCancel = { onEvent(GuideEvent.UserCancelled) },
                            )

                        is GuideState.WaitingUser ->
                            WaitingUserScreen(
                                dest = s.dest,
                                seg = s.seg,
                                onCancel = { onEvent(GuideEvent.UserCancelled) },
                            )

                        is GuideState.Arrived ->
                            ArrivedScreen(
                                dest = s.dest,
                                onDone = { onEvent(GuideEvent.ArrivalConfirmed) },
                            )

                        is GuideState.Recovering ->
                            RecoveringScreen(
                                dest = s.dest,
                                retry = s.retry,
                                maxRetry = maxRetry,
                                onCancel = { onEvent(GuideEvent.UserCancelled) },
                            )

                        is GuideState.Returning -> ReturningScreen()

                        is GuideState.Error ->
                            ErrorScreen(
                                reason = s.reason,
                                onReset = { onEvent(GuideEvent.UserCancelled) },
                            )
                    }
                }

                if (onSimulateVoice != null) {
                    DebugVoiceBar(
                        onSubmit = onSimulateVoice,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}

/**
 * 개발/에뮬레이터 전용 — 마이크 없이 음성 입력을 글로 흉내 낸다.
 * `onSimulateVoice = null` 이면(= 실제 temi 배포) FollowMeApp 이 이 함수를 아예 부르지 않는다.
 */
@Composable
private fun DebugVoiceBar(
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf("") }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            label = { Text("음성 흉내(디버그)") },
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = { if (text.isNotBlank()) { onSubmit(text); text = "" } },
            modifier = Modifier.padding(start = 8.dp),
        ) { Text("전송") }
    }
}
