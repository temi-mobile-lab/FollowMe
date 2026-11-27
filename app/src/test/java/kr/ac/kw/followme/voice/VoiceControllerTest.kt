package kr.ac.kw.followme.voice

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kr.ac.kw.followme.data.DestinationCatalog
import kr.ac.kw.followme.data.sampleDestinations
import kr.ac.kw.followme.guide.GuideEvent
import kr.ac.kw.followme.guide.GuideState
import kr.ac.kw.followme.guide.GuideStateMachine
import kr.ac.kw.followme.guide.VoiceIntent
import kr.ac.kw.followme.robot.FakeRobotController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceControllerTest {
    private val entrance = sampleDestinations.first().copy(voiceAliases = listOf("입구"))
    private val elevator = sampleDestinations.last().copy(voiceAliases = listOf("일층 엘리베이터", "엘리베이터"))
    private val catalog = DestinationCatalog(1, "home base", listOf(entrance, elevator))
    private val interpreter = KoreanGuideInterpreter()

    @Test
    fun `분할 호실을 본 호실과 구별하고 층을 함께 말해도 인식한다`() = runTest {
        val room = elevator.copy(id = "305", displayName = "305호", floor = 3, voiceAliases = listOf("삼백오호"))
        val annex = room.copy(id = "305_1", displayName = "305-1호", voiceAliases = listOf("삼백오 다시 일호"))
        val rooms = catalog.copy(destinations = listOf(room, annex))
        for (phrase in listOf("새빛관 3층 305-1호로 가자", "삼백오 다시 일 호 안내해 줘")) {
            assertEquals(VoiceIntent.SelectDestination(annex.id, ""), interpreter.interpret(phrase, GuideState.Idle, rooms))
        }
        assertEquals(VoiceIntent.SelectDestination(room.id, ""), interpreter.interpret("305호", GuideState.Idle, rooms))
        assertTrue(interpreter.interpret("1층 화장실", GuideState.Idle, rooms) is VoiceIntent.Unknown)
    }

    @Test
    fun `한국어 인식 결과로 연속 안내하고 터치 확인도 함께 쓸 수 있다`() = runTest {
        val robot = FakeRobotController(backgroundScope, moveMillis = 400, minSpeakMillis = 20, speakMillisPerChar = 1)
        val machine = GuideStateMachine(robot, scope = backgroundScope)
        val voice = VoiceController(robot, interpreter, catalog, machine.state, machine::onEvent, backgroundScope)
        machine.start()
        robot.setReady(false)
        voice.start()
        advanceTimeBy(500)
        robot.simulateSpeech("입구")
        assertEquals(GuideState.Idle, machine.state.value)
        robot.setReady(true)
        advanceTimeBy(500)
        voice.onListeningEnded() // 아무도 말하지 않고 인식 세션이 만료됨
        advanceTimeBy(1_100)
        robot.simulateSpeech("일 층 엘리베이터로 안내해 줘.")
        runCurrent()
        assertEquals(GuideState.Confirming(elevator), machine.state.value)
        robot.simulateSpeech("네") // 확인 질문을 읽는 동안의 입력은 무시
        runCurrent()
        assertEquals(GuideState.Confirming(elevator), machine.state.value)
        advanceTimeBy(500)
        robot.simulateSpeech("네")
        runCurrent()
        assertEquals(GuideState.Navigating(elevator, 0), machine.state.value)
        advanceTimeBy(1_100)
        assertEquals(GuideState.Arrived(elevator), machine.state.value)
        robot.simulateSpeech("새빛관 출입문으로 가자")
        runCurrent()
        assertEquals(GuideState.Confirming(entrance, GuideState.Arrived(elevator)), machine.state.value)
        machine.onEvent(GuideEvent.UserConfirmed) // 같은 확인 화면의 안내 시작 버튼
        advanceTimeBy(1_100)
        robot.simulateSpeech("출입문으로 돌아가")
        runCurrent()
        assertEquals(GuideState.Returning, machine.state.value)
        voice.stop()
        machine.stop()
    }

    @Test
    fun `목적지가 모호하거나 잘못된 층이면 이동을 선택하지 않는다`() = runTest {
        for (phrase in listOf("2층 엘리베이터", "입구 말고 엘리베이터", "엘리베이터로 가지 마", "아무 데나")) {
            assertTrue(interpreter.interpret(phrase, GuideState.Idle, catalog) is VoiceIntent.Unknown)
        }
        assertTrue(interpreter.interpret("엘리베이터", GuideState.Arrived(elevator), catalog) is VoiceIntent.Answer)
    }

    @Test
    fun `버튼으로 선택한 목적지도 음성으로 확인하거나 취소할 수 있다`() = runTest {
        val robot = FakeRobotController(backgroundScope, minSpeakMillis = 20, speakMillisPerChar = 1)
        val machine = GuideStateMachine(robot, scope = backgroundScope)
        val voice = VoiceController(robot, interpreter, catalog, machine.state, machine::onEvent, backgroundScope)
        machine.start()
        voice.start()
        machine.onEvent(GuideEvent.StartRequested)
        machine.onEvent(GuideEvent.DestinationSelected(entrance))
        advanceTimeBy(500)
        robot.simulateSpeech("아니요")
        runCurrent()
        assertEquals(GuideState.Selecting, machine.state.value)
        voice.stop()
        advanceTimeBy(500)
        robot.simulateSpeech("엘리베이터")
        assertEquals(GuideState.Selecting, machine.state.value)
        machine.stop()
    }
}
