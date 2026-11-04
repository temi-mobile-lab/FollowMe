package kr.ac.kw.followme.guide

import kr.ac.kw.followme.data.Destination

/**
 * 안내 서비스의 상태. UI 는 오직 이 값만 보고 화면을 그린다.
 *
 * seg 는 Destination.route 의 인덱스다.
 *  - Navigating(dest, seg) : route[seg].location 으로 goTo 중
 *  - WaypointGuide(dest, seg): route[seg] 에 도착해서 say 를 읽어 주는 중
 */
sealed interface GuideState {

    /** 로비 대기. "길 안내가 필요하신가요?" */
    data object Idle : GuideState

    /** 목적지 선택/검색 중. 일정 시간 입력이 없으면 Idle 로 돌아간다. */
    data object Selecting : GuideState

    /** 선택한 목적지 확인 중. */
    data class Confirming(val dest: Destination) : GuideState

    /**
     * route[seg] 로 이동 중.
     *
     * @param retry 이 구간에서 지금까지 실패(abort)한 횟수. 스펙 상 Navigating(dest, seg) 는
     *   2필드지만, 같은 구간에서 연속으로 abort 가 날 때 몇 번째 재시도인지 순수 함수인
     *   transition() 이 "현재 state" 만 보고 알아야 하기 때문에 기본값 0 인 필드로 추가했다.
     *   UI 는 이 값을 쓰지 않는다(RouteProgress 는 seg 만 본다). Recovering(dest, seg, retry)
     *   에서 재시도할 때 이 값을 그대로 실어 보낸다 — [GuideTransitions] 참고.
     */
    data class Navigating(val dest: Destination, val seg: Int, val retry: Int = 0) : GuideState

    /** 갈림길 경유지 도착, 방향 안내 TTS 재생 중. */
    data class WaypointGuide(val dest: Destination, val seg: Int) : GuideState

    /** 최종 목적지 도착. */
    data class Arrived(val dest: Destination) : GuideState

    /** goTo abort 후 재시도 준비. retry 는 현재까지의 재시도 횟수. */
    data class Recovering(val dest: Destination, val seg: Int, val retry: Int) : GuideState

    /** home base 로 복귀 중. */
    data object Returning : GuideState

    /** 복구 불가. 사용자에게 사과하고 처음으로 돌아간다. */
    data class Error(val reason: String) : GuideState

    /** (확장) 사용자가 따라오지 않아 제자리에서 기다리는 중. */
    data class WaitingUser(val dest: Destination, val seg: Int) : GuideState

    /** 로그/CSV 에 남길 짧은 이름. */
    val name: String
        get() = when (this) {
            is Idle -> "Idle"
            is Selecting -> "Selecting"
            is Confirming -> "Confirming"
            is Navigating -> "Navigating"
            is WaypointGuide -> "WaypointGuide"
            is Arrived -> "Arrived"
            is Recovering -> "Recovering"
            is Returning -> "Returning"
            is Error -> "Error"
            is WaitingUser -> "WaitingUser"
        }

    /** 현재 상태가 다루고 있는 목적지(없으면 null). */
    val destination: Destination?
        get() = when (this) {
            is Confirming -> dest
            is Navigating -> dest
            is WaypointGuide -> dest
            is Arrived -> dest
            is Recovering -> dest
            is WaitingUser -> dest
            else -> null
        }

    /** 현재 구간 인덱스(없으면 null). */
    val segment: Int?
        get() = when (this) {
            is Navigating -> seg
            is WaypointGuide -> seg
            is Recovering -> seg
            is WaitingUser -> seg
            else -> null
        }
}
