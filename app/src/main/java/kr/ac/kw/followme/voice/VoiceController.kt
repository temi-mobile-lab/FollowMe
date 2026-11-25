package kr.ac.kw.followme.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kr.ac.kw.followme.data.DestinationCatalog
import kr.ac.kw.followme.guide.GuideEvent
import kr.ac.kw.followme.guide.GuideState
import kr.ac.kw.followme.guide.GuideTransitions
import kr.ac.kw.followme.guide.VoiceIntent
import kr.ac.kw.followme.guide.VoiceIntentInterpreter
import kr.ac.kw.followme.robot.RobotController
import kr.ac.kw.followme.robot.RobotListener
import kr.ac.kw.followme.robot.TtsStatus

/** 터치와 음성 입력은 같은 상태 전이를 사용한다. 인식은 앱이 화면에 있고 응답을 기다릴 때만 켠다. */
class VoiceController(
    private val robot: RobotController,
    private val interpreter: VoiceIntentInterpreter,
    private val catalog: DestinationCatalog,
    private val stateFlow: StateFlow<GuideState>,
    private val onEvent: (GuideEvent) -> Unit,
    private val scope: CoroutineScope,
) : RobotListener {
    private var active = false
    private var speaking = false
    private var listening = false
    private var armJob: Job? = null
    private var collectJob: Job? = null
    private var interpretation: Job? = null

    fun start() {
        if (active) return
        active = true
        robot.addListener(this)
        collectJob = scope.launch { stateFlow.collect { refreshListening() } }
    }

    fun stop() {
        active = false
        robot.removeListener(this)
        collectJob?.cancel()
        interpretation?.cancel()
        armJob?.cancel()
        stopListening()
        speaking = false
    }

    private fun acceptsVoice(state: GuideState) = state is GuideState.Idle ||
        state is GuideState.Selecting || state is GuideState.Confirming || state is GuideState.Arrived

    fun refreshListening() = scheduleListening(delayMillis = 400)

    private fun scheduleListening(delayMillis: Long) {
        armJob?.cancel()
        if (!active || !acceptsVoice(stateFlow.value) || !robot.isVoiceAvailable()) {
            stopListening()
            return
        }
        armJob = scope.launch {
            // TTS 시작 콜백과 ASR 종료 콜백이 화면 전환 직후에 올 수 있다.
            delay(delayMillis)
            if (!speaking && !listening && interpretation?.isActive != true) {
                listening = true
                robot.startListening()
            }
        }
    }

    private fun stopListening() {
        if (listening) {
            listening = false
            robot.stopListening()
        }
    }

    override fun onRobotReady(isReady: Boolean) {
        if (!isReady) speaking = false
        refreshListening()
    }

    override fun onListeningEnded() {
        if (!listening) return
        listening = false
        scheduleListening(delayMillis = 1_000)
    }

    override fun onTtsStatusChanged(status: TtsStatus) {
        speaking = status == TtsStatus.STARTED
        if (speaking) {
            armJob?.cancel()
            stopListening()
        } else {
            refreshListening()
        }
    }

    override fun onSpeechRecognized(text: String) {
        val state = stateFlow.value
        if (!active || speaking || !acceptsVoice(state) || interpretation?.isActive == true) return
        stopListening()
        if (text.isBlank()) {
            refreshListening()
            return
        }
        interpretation = scope.launch {
            val intent = interpreter.interpret(text, state, catalog)
            if (active && stateFlow.value === state) apply(intent, state)
            refreshListening()
        }
    }

    private fun apply(intent: VoiceIntent, state: GuideState) {
        when (intent) {
            is VoiceIntent.SelectDestination -> {
                val dest = catalog.byId(intent.destinationId)
                if (dest == null) {
                    robot.speak("그 목적지는 안내할 수 없어요.")
                    return
                }
                if (state is GuideState.Idle) onEvent(GuideEvent.StartRequested)
                val event = GuideEvent.DestinationSelected(dest)
                dispatchOrExplain(stateFlow.value, event, "출발하려면 네, 다시 고르려면 취소라고 말씀해 주세요.")
            }
            is VoiceIntent.Confirm -> dispatchOrExplain(state, GuideEvent.UserConfirmed, intent.reply)
            is VoiceIntent.Cancel -> dispatchOrExplain(state, GuideEvent.UserCancelled, intent.reply)
            is VoiceIntent.Done -> dispatchOrExplain(state, GuideEvent.ArrivalConfirmed, intent.reply)
            is VoiceIntent.Answer, is VoiceIntent.Unknown ->
                robot.speak(intent.reply.ifBlank { "다시 말씀해 주세요." })
        }
    }

    private fun dispatchOrExplain(state: GuideState, event: GuideEvent, reply: String) {
        if (GuideTransitions.transition(state, event) != state) onEvent(event)
        else robot.speak(reply.ifBlank { "지금은 그렇게 할 수 없어요." })
    }
}
