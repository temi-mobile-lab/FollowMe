package kr.ac.kw.followme.guide

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.ac.kw.followme.data.Destination
import kr.ac.kw.followme.data.RouteStep
import kr.ac.kw.followme.robot.FakeRobotController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GuideStateMachine 은 onEnter(side effect) + 타임아웃 + 로그를 FakeRobotController 와
 * 묶어서 돌려야 의미가 있는 부분이라, 순수 transition() 테스트와 별도로 여기서 검증한다.
 * TestScope/StandardTestDispatcher 로 가상 시간을 써서 delay() 를 실제로 기다리지 않는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GuideStateMachineTest {

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

    private fun entries() = mutableListOf<GuideLogEntry>()

    @Test
    fun `정상 흐름이 Idle에서 시작해서 Idle로 돌아온다`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val robot = FakeRobotController(scope = scope, abortRate = 0f)
        val log = entries()
        val machine = GuideStateMachine(robot = robot, logger = { log += it }, scope = scope)
        machine.start()

        machine.onEvent(GuideEvent.StartRequested)
        assertEquals(GuideState.Selecting, machine.state.value)

        machine.onEvent(GuideEvent.DestinationSelected(dest))
        assertEquals(GuideState.Confirming(dest), machine.state.value)

        machine.onEvent(GuideEvent.UserConfirmed)
        assertEquals(GuideState.Navigating(dest, seg = 0), machine.state.value)

        // advanceUntilIdle() 은 예약된 코루틴이 하나도 안 남을 때까지 돌리므로,
        // 1구간 goTo 완료 -> WaypointGuide -> TTS 완료 -> 2구간 Navigating -> goTo 완료 -> Arrived
        // 까지 한 번에 끝까지 진행된다 (중간에 멈춰 세울 수 없다).
        advanceUntilIdle()
        assertEquals(GuideState.Arrived(dest), machine.state.value)

        machine.onEvent(GuideEvent.ArrivalConfirmed)
        assertEquals(GuideState.Returning, machine.state.value)

        // Returning: speak + goTo(home base) 완료 -> Idle
        advanceUntilIdle()
        assertEquals(GuideState.Idle, machine.state.value)

        // 실제로 상태가 바뀐 전이만 로그에 남았는지 확인
        assertEquals(
            listOf(
                "Idle->Selecting", "Selecting->Confirming", "Confirming->Navigating",
                "Navigating->WaypointGuide", "WaypointGuide->Navigating",
                "Navigating->Arrived", "Arrived->Returning", "Returning->Idle",
            ),
            log.map { "${it.prevState}->${it.nextState}" },
        )
        machine.stop()
    }

    @Test
    fun `Selecting에서 입력이 없으면 타임아웃으로 Idle로 돌아간다`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val robot = FakeRobotController(scope = scope)
        val machine = GuideStateMachine(
            robot = robot,
            scope = scope,
            selectTimeoutMillis = 5_000,
        )
        machine.start()

        machine.onEvent(GuideEvent.StartRequested)
        assertEquals(GuideState.Selecting, machine.state.value)

        advanceTimeBy(4_999)
        assertEquals(GuideState.Selecting, machine.state.value) // 아직 타임아웃 전

        advanceTimeBy(2)
        assertEquals(GuideState.Idle, machine.state.value) // 타임아웃으로 복귀

        machine.stop()
    }

    @Test
    fun `goTo가 계속 abort되면 MAX_RETRY 후 Error로 간다`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        // abortRate = 1f: 항상 실패하는 로봇
        val robot = FakeRobotController(scope = scope, abortRate = 1f, moveMillis = 100)
        val machine = GuideStateMachine(robot = robot, scope = scope, recoverDelayMillis = 100)
        machine.start()

        machine.onEvent(GuideEvent.StartRequested)
        machine.onEvent(GuideEvent.DestinationSelected(dest))
        machine.onEvent(GuideEvent.UserConfirmed)
        assertEquals(GuideState.Navigating(dest, seg = 0, retry = 0), machine.state.value)

        advanceUntilIdle()

        val finalState = machine.state.value
        assertTrue("Error 상태가 아님: $finalState", finalState is GuideState.Error)

        machine.stop()
    }

    @Test
    fun `사용자가 이동 중 취소하면 Returning을 거쳐 Idle로 돌아간다`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val robot = FakeRobotController(scope = scope, abortRate = 0f)
        val machine = GuideStateMachine(robot = robot, scope = scope)
        machine.start()

        machine.onEvent(GuideEvent.StartRequested)
        machine.onEvent(GuideEvent.DestinationSelected(dest))
        machine.onEvent(GuideEvent.UserConfirmed)
        assertEquals(GuideState.Navigating(dest, seg = 0), machine.state.value)

        machine.onEvent(GuideEvent.UserCancelled)
        assertEquals(GuideState.Returning, machine.state.value)

        advanceUntilIdle()
        assertEquals(GuideState.Idle, machine.state.value)

        machine.stop()
    }
}
