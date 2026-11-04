package kr.ac.kw.followme.guide

import kr.ac.kw.followme.data.Destination
import kr.ac.kw.followme.data.RouteStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * [GuideTransitions.transition] 은 순수 함수라서 로봇/코루틴 없이 바로 테스트할 수 있다.
 * 테스트용 목적지는 경유지 1개 + 최종 목적지, 총 2구간(route.size == 2)으로 고정해서
 * "마지막 구간이면 Arrived, 아니면 WaypointGuide" 분기를 명확히 검증한다.
 */
class GuideTransitionsTest {

    private val dest = Destination(
        id = "test_dest",
        displayName = "테스트 목적지",
        category = "강의실",
        building = "테스트관",
        route = listOf(
            RouteStep("junction_a", "여기서 왼쪽으로 갑니다"),
            RouteStep("test_dest", ""),
        ),
        arrivalMessage = "도착했습니다.",
    )

    private fun transition(state: GuideState, event: GuideEvent) = GuideTransitions.transition(state, event)

    // --- Idle ---------------------------------------------------------

    @Test
    fun `Idle에서 StartRequested를 받으면 Selecting으로 전이한다`() {
        assertEquals(GuideState.Selecting, transition(GuideState.Idle, GuideEvent.StartRequested))
    }

    @Test
    fun `Idle은 관계없는 이벤트를 무시한다`() {
        assertEquals(GuideState.Idle, transition(GuideState.Idle, GuideEvent.Timeout))
        assertEquals(GuideState.Idle, transition(GuideState.Idle, GuideEvent.GoToComplete))
    }

    // --- Selecting ------------------------------------------------------

    @Test
    fun `Selecting에서 목적지를 고르면 Confirming으로 전이한다`() {
        val next = transition(GuideState.Selecting, GuideEvent.DestinationSelected(dest))
        assertEquals(GuideState.Confirming(dest), next)
    }

    @Test
    fun `Selecting은 Timeout에 Idle로 돌아간다`() {
        assertEquals(GuideState.Idle, transition(GuideState.Selecting, GuideEvent.Timeout))
    }

    @Test
    fun `Selecting은 UserCancelled에 Idle로 돌아간다`() {
        assertEquals(GuideState.Idle, transition(GuideState.Selecting, GuideEvent.UserCancelled))
    }

    // --- Confirming -------------------------------------------------------

    @Test
    fun `Confirming에서 확인하면 Navigating 0구간으로 전이한다`() {
        val next = transition(GuideState.Confirming(dest), GuideEvent.UserConfirmed)
        assertEquals(GuideState.Navigating(dest, seg = 0, retry = 0), next)
    }

    @Test
    fun `Confirming에서 다시 선택을 누르면 Selecting으로 돌아간다`() {
        val next = transition(GuideState.Confirming(dest), GuideEvent.UserCancelled)
        assertEquals(GuideState.Selecting, next)
    }

    // --- Navigating ---------------------------------------------------

    @Test
    fun `Navigating 중간 구간 도착은 WaypointGuide로 전이한다`() {
        val state = GuideState.Navigating(dest, seg = 0)
        val next = transition(state, GuideEvent.GoToComplete)
        assertEquals(GuideState.WaypointGuide(dest, seg = 0), next)
    }

    @Test
    fun `Navigating 마지막 구간 도착은 Arrived로 전이한다`() {
        val state = GuideState.Navigating(dest, seg = 1) // dest.route.lastIndex == 1
        val next = transition(state, GuideEvent.GoToComplete)
        assertEquals(GuideState.Arrived(dest), next)
    }

    @Test
    fun `Navigating 중 abort는 retry를 1로 만들며 Recovering으로 전이한다`() {
        val state = GuideState.Navigating(dest, seg = 0, retry = 0)
        val next = transition(state, GuideEvent.GoToAbort("장애물"))
        assertEquals(GuideState.Recovering(dest, seg = 0, retry = 1), next)
    }

    @Test
    fun `이미 한 번 실패한 Navigating이 다시 abort되면 retry가 2로 증가한다`() {
        val state = GuideState.Navigating(dest, seg = 0, retry = 1)
        val next = transition(state, GuideEvent.GoToAbort("장애물"))
        assertEquals(GuideState.Recovering(dest, seg = 0, retry = 2), next)
    }

    @Test
    fun `Navigating 중 취소는 Returning으로 전이한다`() {
        val state = GuideState.Navigating(dest, seg = 0)
        assertEquals(GuideState.Returning, transition(state, GuideEvent.UserCancelled))
    }

    @Test
    fun `Navigating 중 사용자를 놓치면 WaitingUser로 전이한다`() {
        val state = GuideState.Navigating(dest, seg = 0)
        assertEquals(GuideState.WaitingUser(dest, seg = 0), transition(state, GuideEvent.UserLost))
    }

    // --- WaypointGuide ----------------------------------------------------

    @Test
    fun `WaypointGuide는 TtsCompleted에 다음 구간 Navigating으로 전이하고 retry는 0으로 리셋된다`() {
        val state = GuideState.WaypointGuide(dest, seg = 0)
        val next = transition(state, GuideEvent.TtsCompleted)
        assertEquals(GuideState.Navigating(dest, seg = 1, retry = 0), next)
    }

    @Test
    fun `WaypointGuide 중 취소는 Returning으로 전이한다`() {
        val state = GuideState.WaypointGuide(dest, seg = 0)
        assertEquals(GuideState.Returning, transition(state, GuideEvent.UserCancelled))
    }

    // --- Arrived ------------------------------------------------------

    @Test
    fun `Arrived에서 도착 확인을 누르면 Returning으로 전이한다`() {
        val state = GuideState.Arrived(dest)
        assertEquals(GuideState.Returning, transition(state, GuideEvent.ArrivalConfirmed))
    }

    // --- Recovering -----------------------------------------------------

    @Test
    fun `Recovering은 재시도 한도 전이면 같은 구간 Navigating으로 돌아간다`() {
        val state = GuideState.Recovering(dest, seg = 0, retry = 1)
        val next = transition(state, GuideEvent.Timeout)
        assertEquals(GuideState.Navigating(dest, seg = 0, retry = 1), next)
    }

    @Test
    fun `Recovering은 MAX_RETRY에 도달하면 Error로 전이한다`() {
        val state = GuideState.Recovering(dest, seg = 0, retry = GuideTransitions.MAX_RETRY)
        val next = transition(state, GuideEvent.Timeout)
        assert(next is GuideState.Error) { "Error 상태가 아님: $next" }
    }

    @Test
    fun `Recovering 중 취소는 Returning으로 전이한다`() {
        val state = GuideState.Recovering(dest, seg = 0, retry = 1)
        assertEquals(GuideState.Returning, transition(state, GuideEvent.UserCancelled))
    }

    // --- Returning / Error ----------------------------------------------

    @Test
    fun `Returning은 GoToComplete에 Idle로 전이한다`() {
        assertEquals(GuideState.Idle, transition(GuideState.Returning, GuideEvent.GoToComplete))
    }

    @Test
    fun `Error에서 UserCancelled를 받으면 Returning으로 전이한다`() {
        val state = GuideState.Error("테스트 실패")
        assertEquals(GuideState.Returning, transition(state, GuideEvent.UserCancelled))
    }

    // --- WaitingUser (확장) -----------------------------------------------

    @Test
    fun `WaitingUser는 UserFound에 같은 구간 Navigating으로 돌아간다`() {
        val state = GuideState.WaitingUser(dest, seg = 0)
        val next = transition(state, GuideEvent.UserFound)
        assertEquals(GuideState.Navigating(dest, seg = 0, retry = 0), next)
    }

    @Test
    fun `WaitingUser 중 취소는 Returning으로 전이한다`() {
        val state = GuideState.WaitingUser(dest, seg = 0)
        assertEquals(GuideState.Returning, transition(state, GuideEvent.UserCancelled))
    }

    // --- 정의되지 않은 조합은 무시(동일 state 유지) -------------------------------

    @Test
    fun `정의되지 않은 조합은 같은 state를 그대로 돌려준다`() {
        val state = GuideState.Navigating(dest, seg = 0)
        val next = transition(state, GuideEvent.DestinationSelected(dest))
        assertSame(state, next)
    }
}
