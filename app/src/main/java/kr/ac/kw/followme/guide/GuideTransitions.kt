package kr.ac.kw.followme.guide

/**
 * GuideState 의 상태 전이 규칙.
 *
 * [transition] 은 side effect 가 없는 순수 함수다 — goTo/speak 호출은 절대 여기서 하지 않는다.
 * 그건 GuideStateMachine.onEnter() 의 몫이다. 정의되지 않은 (state, event) 조합은 state 를
 * 그대로 돌려준다(무시) — 호출부는 `next == prev` 로 "실제 전이가 없었다"를 판단할 수 있다.
 *
 * ## 취소(UserCancelled) 처리 방침
 * 스펙 문서는 "어느 상태든 사용자가 취소하면 안내 종료 TTS 후 Returning 으로 간다"고 했지만,
 * 그대로 적용하면 Selecting/Confirming 처럼 **로봇이 아직 출발하지 않은** 상태에서도 매번
 * "안내를 마칩니다" TTS 와 home base goTo 가 발생해 어색하다. 그래서 로봇이 실제로 움직이기
 * 시작한 뒤(Navigating 이후)에만 Returning 으로 보내고, 그 전(Selecting/Confirming)에는
 * 한 단계 뒤로 돌아가는 것으로 구현했다. 팀 논의 후 다르게 가도 된다.
 *
 * ## retry 흐름
 * Navigating(dest, seg, retry) 의 retry 는 "이 구간에서 지금까지 실패한 횟수"다.
 * abort 가 나면 Recovering(retry+1) 으로 가고, 재시도 타임아웃 뒤 다시 Navigating 으로
 * 돌아갈 때 그 retry 값을 그대로 실어 보낸다 — 그래야 또 실패했을 때 몇 번째인지 셀 수 있다.
 */
object GuideTransitions {

    /** 이 횟수를 넘겨 실패하면 Recovering → Error 로 간다. */
    const val MAX_RETRY = 3

    fun transition(state: GuideState, event: GuideEvent): GuideState = when (state) {
        is GuideState.Idle -> idle(event)
        is GuideState.Selecting -> selecting(event)
        is GuideState.Confirming -> confirming(state, event)
        is GuideState.Navigating -> navigating(state, event)
        is GuideState.WaypointGuide -> waypointGuide(state, event)
        is GuideState.Arrived -> arrived(state, event)
        is GuideState.Recovering -> recovering(state, event)
        is GuideState.Returning -> returning(state, event)
        is GuideState.Error -> error(state, event)
        is GuideState.WaitingUser -> waitingUser(state, event)
    }

    private fun idle(event: GuideEvent): GuideState = when (event) {
        is GuideEvent.StartRequested -> GuideState.Selecting
        else -> GuideState.Idle
    }

    private fun selecting(event: GuideEvent): GuideState = when (event) {
        is GuideEvent.DestinationSelected -> GuideState.Confirming(event.dest)
        // 일정 시간 입력이 없으면 대기 화면으로.
        is GuideEvent.Timeout -> GuideState.Idle
        // 로봇이 아직 움직이지 않았으므로 "안내 종료" 없이 바로 대기 화면으로.
        is GuideEvent.UserCancelled -> GuideState.Idle
        else -> GuideState.Selecting
    }

    private fun confirming(state: GuideState.Confirming, event: GuideEvent): GuideState = when (event) {
        is GuideEvent.UserConfirmed -> GuideState.Navigating(state.dest, seg = 0)
        // "다시 선택" 버튼. 아직 출발 전이라 Returning 까지 갈 필요 없이 선택 화면으로.
        is GuideEvent.UserCancelled -> GuideState.Selecting
        else -> state
    }

    private fun navigating(state: GuideState.Navigating, event: GuideEvent): GuideState = when (event) {
        is GuideEvent.GoToComplete ->
            if (state.seg >= state.dest.route.lastIndex) {
                GuideState.Arrived(state.dest)
            } else {
                GuideState.WaypointGuide(state.dest, state.seg)
            }

        is GuideEvent.GoToAbort -> GuideState.Recovering(state.dest, state.seg, retry = state.retry + 1)

        // 이동을 시작한 뒤의 취소는 실제로 복귀 동작(TTS + goTo home base)이 필요하다.
        is GuideEvent.UserCancelled -> GuideState.Returning

        // (확장) 사람 감지: 따라오던 사용자를 놓치면 제자리에서 기다린다.
        is GuideEvent.UserLost -> GuideState.WaitingUser(state.dest, state.seg)

        else -> state
    }

    private fun waypointGuide(state: GuideState.WaypointGuide, event: GuideEvent): GuideState = when (event) {
        is GuideEvent.TtsCompleted -> GuideState.Navigating(state.dest, state.seg + 1)
        is GuideEvent.UserCancelled -> GuideState.Returning
        else -> state
    }

    private fun arrived(state: GuideState.Arrived, event: GuideEvent): GuideState = when (event) {
        is GuideEvent.ArrivalConfirmed -> GuideState.Returning
        // Arrived 화면엔 취소 버튼이 없지만, 방어적으로 같은 결과(Returning)로 처리한다.
        is GuideEvent.UserCancelled -> GuideState.Returning
        else -> state
    }

    private fun recovering(state: GuideState.Recovering, event: GuideEvent): GuideState = when (event) {
        is GuideEvent.Timeout ->
            if (state.retry >= MAX_RETRY) {
                GuideState.Error("'${state.dest.displayName}' 경로 재시도 ${state.retry}회 실패")
            } else {
                GuideState.Navigating(state.dest, state.seg, retry = state.retry)
            }
        is GuideEvent.UserCancelled -> GuideState.Returning
        else -> state
    }

    private fun returning(state: GuideState.Returning, event: GuideEvent): GuideState = when (event) {
        is GuideEvent.GoToComplete -> GuideState.Idle
        else -> state
    }

    private fun error(state: GuideState.Error, event: GuideEvent): GuideState = when (event) {
        // ErrorScreen 의 "처음으로" 버튼이 UserCancelled 를 보낸다 (FollowMeApp.kt 배선).
        is GuideEvent.UserCancelled -> GuideState.Returning
        else -> state
    }

    private fun waitingUser(state: GuideState.WaitingUser, event: GuideEvent): GuideState = when (event) {
        // 사용자를 다시 찾으면 그 구간부터 재시도(retry 는 새로 센다 — 로봇 쪽 실패가 아니었으므로).
        is GuideEvent.UserFound -> GuideState.Navigating(state.dest, state.seg)
        is GuideEvent.UserCancelled -> GuideState.Returning
        else -> state
    }
}
