package kr.ac.kw.followme.robot

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * 로봇 없이 에뮬레이터/JUnit 에서 전체 흐름을 돌리기 위한 가짜 로봇.
 *
 * - goTo 는 [moveMillis] 뒤에 [abortRate] 확률로 ABORT, 아니면 COMPLETE 콜백을 보낸다.
 * - speak 는 글자 수에 비례한 시간 뒤에 COMPLETED 콜백을 보낸다.
 * - 이동과 발화는 temi 처럼 동시에 진행될 수 있어 Job 을 따로 관리한다
 *   (goTo 중에 speak 를 불러도 이동이 취소되지 않는다. 단, goTo 를 다시 부르면
 *   이전 goTo 는 취소된다 — stopMovement 와 같은 효과).
 *
 * @param scope 코루틴을 실행할 스코프. 바깥(GuideStateMachine 을 만드는 쪽)에서 주입한다 —
 *   JUnit 테스트에서는 TestScope 를 넣어 가상 시간으로 delay 를 즉시 흘려보낼 수 있다.
 */
class FakeRobotController(
    private val scope: CoroutineScope,
    private val moveMillis: Long = 2_500,
    private val speakMillisPerChar: Long = 90,
    private val minSpeakMillis: Long = 600,
    private val abortRate: Float = 0f,
    private val savedLocations: Set<String> = emptySet(),
    private val random: Random = Random.Default,
) : RobotController {

    private val listeners = mutableSetOf<RobotListener>()
    private var moveJob: Job? = null
    private var speakJob: Job? = null
    private var ready = true

    /** 연결할 실제 로봇이 없어서 할 일이 없다. */
    override fun start() = Unit
    override fun stop() = Unit

    override fun addListener(listener: RobotListener) {
        listeners += listener
    }

    override fun removeListener(listener: RobotListener) {
        listeners -= listener
    }

    override fun goTo(location: String, speed: GoToSpeed) {
        moveJob?.cancel()
        notifyGoTo(location, GoToStatus.START)
        moveJob = scope.launch {
            notifyGoTo(location, GoToStatus.CALCULATING)
            delay(moveMillis / 4)
            notifyGoTo(location, GoToStatus.GOING)
            delay(moveMillis)
            if (random.nextFloat() < abortRate) {
                notifyGoTo(location, GoToStatus.ABORT, "simulated obstacle near $location")
            } else {
                notifyGoTo(location, GoToStatus.COMPLETE)
            }
        }
    }

    override fun stopMovement() {
        moveJob?.cancel()
    }

    override fun speak(text: String) {
        speakJob?.cancel()
        notifyTts(TtsStatus.STARTED)
        val duration = (text.length * speakMillisPerChar).coerceAtLeast(minSpeakMillis)
        speakJob = scope.launch {
            delay(duration)
            notifyTts(TtsStatus.COMPLETED)
        }
    }

    override fun getLocations(): List<String> = savedLocations.toList()

    override fun isReady(): Boolean = ready

    // 가짜 로봇은 마이크가 없다. startListening/stopListening 은 상태만 기록해 두고,
    // 실제 인식 결과는 테스트/에뮬레이터에서 [simulateSpeech] 로 직접 흘려보낸다.
    private var listening = false
    override fun startListening() { listening = true }
    override fun stopListening() { listening = false }

    /** 에뮬레이터/테스트에서 마이크 없이 음성 입력을 흉내 낼 때 쓴다. 듣고 있을 때만 반영된다. */
    fun simulateSpeech(text: String) {
        if (!listening) return
        listening = false
        listeners.toList().forEach { it.onSpeechRecognized(text) }
    }

    /** 테스트/데모에서 "로봇이 아직 준비 안 됨" 상황을 흉내 낼 때 쓴다. */
    fun setReady(value: Boolean) {
        ready = value
        listeners.toList().forEach { it.onRobotReady(value) }
    }

    private fun notifyGoTo(location: String, status: GoToStatus, description: String = "") {
        listeners.toList().forEach { it.onGoToStatusChanged(location, status, description) }
    }

    private fun notifyTts(status: TtsStatus) {
        listeners.toList().forEach { it.onTtsStatusChanged(status) }
    }
}
