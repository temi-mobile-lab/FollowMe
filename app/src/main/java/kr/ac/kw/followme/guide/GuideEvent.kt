package kr.ac.kw.followme.guide

import kr.ac.kw.followme.data.Destination

/**
 * 상태 전이를 일으키는 입력.
 * 사용자 입력(UI), 로봇 콜백(RobotController), 내부 타이머 세 종류가 모두 여기로 들어온다.
 */
sealed interface GuideEvent {

    // --- 사용자 입력 -------------------------------------------------------
    /** 대기 화면에서 화면을 터치했다. (명세에 없던 이벤트지만 Idle -> Selecting 전이에 필요해 추가) */
    data object StartRequested : GuideEvent

    data class DestinationSelected(val dest: Destination) : GuideEvent
    data object UserConfirmed : GuideEvent
    data object UserCancelled : GuideEvent
    data object ArrivalConfirmed : GuideEvent

    // --- 로봇 콜백 ---------------------------------------------------------
    data object GoToComplete : GuideEvent
    data class GoToAbort(val description: String) : GuideEvent
    data object TtsCompleted : GuideEvent

    // --- 내부 타이머 -------------------------------------------------------
    data object Timeout : GuideEvent

    // --- (확장) 사람 감지 --------------------------------------------------
    data object UserLost : GuideEvent
    data object UserFound : GuideEvent

    /** 로그/CSV 에 남길 짧은 이름. */
    val name: String
        get() = when (this) {
            is StartRequested -> "StartRequested"
            is DestinationSelected -> "DestinationSelected"
            is UserConfirmed -> "UserConfirmed"
            is UserCancelled -> "UserCancelled"
            is ArrivalConfirmed -> "ArrivalConfirmed"
            is GoToComplete -> "GoToComplete"
            is GoToAbort -> "GoToAbort"
            is TtsCompleted -> "TtsCompleted"
            is Timeout -> "Timeout"
            is UserLost -> "UserLost"
            is UserFound -> "UserFound"
        }
}
