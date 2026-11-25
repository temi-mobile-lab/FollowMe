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
import kr.ac.kw.followme.robot.GoToStatus
import kr.ac.kw.followme.robot.RobotListener
import kr.ac.kw.followme.robot.RobotController
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
    fun `엘리베이터에 실제 도착하면 다음 위층 호실도 제자리에서 안내한다`() = runTest {
        val room = dest.copy(
            id = "305_1", displayName = "305-1호", floor = 3, showFloorMap = true,
            route = listOf(RouteStep("1층 엘리베이터", "")),
        )
        val machine = GuideStateMachine(FakeRobotController(backgroundScope), scope = backgroundScope)
        machine.onEvent(GuideEvent.StartRequested)
        machine.onEvent(GuideEvent.DestinationSelected(room))
        machine.onEvent(GuideEvent.UserConfirmed)
        machine.onGoToStatusChanged("새빛관 출입문", GoToStatus.COMPLETE, "")
        assertEquals(GuideState.Navigating(room, 0), machine.state.value)
        machine.onGoToStatusChanged("1층 엘리베이터", GoToStatus.COMPLETE, "")
        assertEquals(GuideState.Arrived(room), machine.state.value)
        assertEquals("1층 엘리베이터", room.arrivalPlaceName)

        val next = room.copy(id = "201", displayName = "201호", floor = 2)
        machine.onEvent(GuideEvent.DestinationSelected(next))
        assertEquals(GuideState.Arrived(next), machine.state.value)
        machine.onEvent(GuideEvent.DestinationSelected(dest))
        assertEquals(GuideState.Confirming(dest, GuideState.Arrived(next)), machine.state.value)
    }

    @Test
    fun `출발 확정 때만 따라오라는 안내를 한 번 읽는다`() = runTest {
        val speech = mutableListOf<String>()
        val fake = FakeRobotController(backgroundScope)
        val robot = object : RobotController by fake {
            override fun speak(text: String) {
                speech += text
                fake.speak(text)
            }
        }
        val machine = GuideStateMachine(robot, scope = backgroundScope)
        machine.start()
        machine.onEvent(GuideEvent.StartRequested)
        machine.onEvent(GuideEvent.DestinationSelected(dest))
        assertEquals(listOf("${dest.displayName}까지 안내할까요?"), speech)
        machine.onEvent(GuideEvent.UserConfirmed)
        assertEquals("안내를 시작합니다. 저를 따라오세요.", speech.last())
        machine.onEvent(GuideEvent.GoToAbort("재시도"))
        machine.onEvent(GuideEvent.Timeout)
        machine.onEvent(GuideEvent.GoToComplete)
        machine.onEvent(GuideEvent.TtsCompleted)
        assertEquals(1, speech.count { "따라오세요" in it })
        machine.stop()
    }

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
    fun `두 목적지를 연이어 안내하고 복귀를 선택한 뒤에만 홈베이스로 간다`() = runTest {
        val first = dest.copy(route = listOf(RouteStep("entrance", "")))
        val second = dest.copy(id = "restroom", route = listOf(RouteStep("restroom", "")))
        val robot = FakeRobotController(scope = this)
        val moves = mutableListOf<String>()
        robot.addListener(object : RobotListener {
            override fun onGoToStatusChanged(location: String, status: GoToStatus, description: String) {
                if (status == GoToStatus.START) moves += location
            }
        })
        val machine = GuideStateMachine(robot = robot, scope = this)
        machine.start()
        machine.onEvent(GuideEvent.StartRequested)
        machine.onEvent(GuideEvent.DestinationSelected(first))
        machine.onEvent(GuideEvent.UserConfirmed)
        advanceUntilIdle()
        assertEquals(GuideState.Arrived(first), machine.state.value)

        machine.onEvent(GuideEvent.DestinationSelected(second))
        advanceUntilIdle()
        assertEquals(listOf("entrance"), moves)
        machine.onEvent(GuideEvent.UserConfirmed)
        advanceUntilIdle()
        assertEquals(GuideState.Arrived(second), machine.state.value)
        assertEquals(listOf("entrance", "restroom"), moves)

        machine.onEvent(GuideEvent.ArrivalConfirmed)
        advanceUntilIdle()
        assertEquals(GuideState.Idle, machine.state.value)
        assertEquals(listOf("entrance", "restroom", "home base"), moves)
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
