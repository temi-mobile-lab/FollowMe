package kr.ac.kw.followme.guide

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import kr.ac.kw.followme.robot.FakeRobotController
import kr.ac.kw.followme.robot.GoToSpeed
import kr.ac.kw.followme.robot.GoToStatus
import kr.ac.kw.followme.robot.RobotBattery
import kr.ac.kw.followme.robot.RobotController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryReturnTest {
    private class Fixture(scope: CoroutineScope, initial: Int) {
        val moves = mutableListOf<String>()
        val speech = mutableListOf<String>()
        private var reading = RobotBattery(initial, false)
        private val robot = object : RobotController by FakeRobotController(scope) {
            override fun getBattery() = reading
            override fun goTo(location: String, speed: GoToSpeed) { moves += location }
            override fun speak(text: String) { speech += text }
        }
        val machine = GuideStateMachine(robot, scope = scope)

        init {
            machine.onRobotReady(true)
            arrive("새빛관 출입문")
        }

        fun measure(percent: Int, charging: Boolean = false) {
            reading = RobotBattery(percent, charging)
            machine.onBatteryChanged(reading)
        }

        fun arrive(location: String) = machine.onGoToStatusChanged(location, GoToStatus.COMPLETE, "")
    }

    @Test
    fun `일반 모드는 시작과 안내 중에도 낮은 배터리로 홈베이스에 가지 않는다`() = runTest {
        val f = Fixture(backgroundScope, 79)
        f.measure(80)
        f.measure(15)
        assertEquals(GuideState.Idle, f.machine.state.value)
        f.machine.onEvent(GuideEvent.StartRequested)
        f.measure(1)
        assertEquals(GuideState.Selecting, f.machine.state.value)
        assertEquals(listOf("새빛관 출입문"), f.moves)
    }

    @Test
    fun `관리자 감시는 80에서 한 번 복귀하고 100에서만 대기를 재개한다`() = runTest {
        val f = Fixture(backgroundScope, 81)
        f.machine.startBatteryMonitoring(simulateLow = false)
        assertEquals(GuideState.Idle, f.machine.state.value)
        f.measure(80)
        f.measure(79)
        f.arrive("새빛관 출입문") // 취소된 이전 이동의 콜백
        assertEquals(GuideState.Docking, f.machine.state.value)
        assertEquals(listOf("새빛관 출입문", "home base"), f.moves)
        assertTrue(f.speech.single().contains("80퍼센트 이하"))
        f.measure(79, charging = true)
        f.arrive("home base")
        f.measure(99, charging = true)
        assertEquals(GuideState.Charging, f.machine.state.value)
        f.measure(100, charging = true)
        f.arrive("새빛관 출입문")
        f.measure(80)
        assertFalse(f.machine.adminMode.value)
        assertEquals(listOf("새빛관 출입문", "home base", "새빛관 출입문"), f.moves)
    }

    @Test
    fun `복귀 시험은 본체 잔량을 보존하고 완충이어도 도착 후에 출입문으로 돌아간다`() = runTest {
        val f = Fixture(backgroundScope, 100)
        f.machine.startBatteryMonitoring(simulateLow = true)
        assertEquals(100, f.machine.battery.value?.percent)
        assertEquals(GuideState.Docking, f.machine.state.value)
        f.measure(100)
        assertEquals(GuideState.Docking, f.machine.state.value)
        f.arrive("home base")
        assertEquals(GuideState.Returning, f.machine.state.value)
        assertFalse(f.machine.adminMode.value)
        assertEquals(listOf("새빛관 출입문", "home base", "새빛관 출입문"), f.moves)
    }

    @Test
    fun `관리자 종료 뒤 늦게 온 저전력과 도착 콜백으로 충전 복귀가 재개되지 않는다`() = runTest {
        val f = Fixture(backgroundScope, 80)
        f.machine.startBatteryMonitoring(simulateLow = false)
        assertEquals(GuideState.Docking, f.machine.state.value)
        f.machine.exitAdminMode()
        f.measure(70)
        f.arrive("home base")
        f.arrive("새빛관 출입문")
        assertFalse(f.machine.adminMode.value)
        assertEquals(GuideState.Idle, f.machine.state.value)
        assertEquals(listOf("새빛관 출입문", "home base", "새빛관 출입문"), f.moves)
    }
}
