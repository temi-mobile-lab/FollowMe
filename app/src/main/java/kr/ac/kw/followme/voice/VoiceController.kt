package kr.ac.kw.followme.voice

import android.util.Log
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

/**
 * 음성 입력을 GuideEvent 로 바꿔 주는 오케스트레이터.
 *
 * 터치(FollowMeApp 의 onClick 들)와 이 클래스는 둘 다 `onEvent(GuideEvent)` 라는
 * 같은 입구로 들어간다 — GuideStateMachine/GuideTransitions 는 음성에 대해 아무것도
 * 모르고, VoiceController 는 상태 전이 규칙에 대해 아무것도 모른다(들은 state 가
 * 바뀌지 않는 조합이면 onEvent 가 그냥 무시한다).
 *
 * 듣기 시작 시점 = "지금 상태에서 로봇이 TTS 를 말하고 있지 않은 때". onEnter 의 speak()
 * 호출은 GuideStateMachine 안에만 있어서 VoiceController 가 직접 알 수 없기 때문에,
 * TTS STARTED/COMPLETED 콜백을 관찰해서 "말하는 중이 아니면 듣는다"는 규칙으로 간접적으로
 * 맞춘다. 상태가 바뀐 직후 onEnter 의 speak() 호출이 아직 STARTED 콜백을 안 쏜 찰나의
 * 순간을 피하려고 아주 짧은 유예(ARM_DELAY_MS)를 둔다.
 *
 * ⚠️ 실기기 확인 필요: 이 유예 시간이 실제 로봇에서도 충분한지(onEnter 의 speak() 호출이
 * 그보다 늦게 STARTED 를 쏘면 로봇이 말하는 도중에 듣기 시작할 수 있다).
 */
class VoiceController(
    private val robot: RobotController,
    private val interpreter: VoiceIntentInterpreter,
    private val catalog: DestinationCatalog,
    private val stateFlow: StateFlow<GuideState>,
    private val onEvent: (GuideEvent) -> Unit,
    private val scope: CoroutineScope,
) : RobotListener {

    private var ttsInProgress = false
    private var listening = false
    private var armJob: Job? = null
    private var collectJob: Job? = null

    fun start() {
        robot.addListener(this)
        collectJob = scope.launch {
            stateFlow.collect { arm() }
        }
    }

    fun stop() {
        robot.removeListener(this)
        collectJob?.cancel()
        armJob?.cancel()
        if (listening) {
            listening = false
            robot.stopListening()
        }
    }

    private fun isVoiceState(state: GuideState) =
        state is GuideState.Idle || state is GuideState.Selecting ||
            state is GuideState.Confirming || state is GuideState.Arrived

    /** 상태가 바뀔 때마다(혹은 TTS 가 끝났을 때) 지금 들어도 되는 상태인지 다시 판단한다. */
    private fun arm() {
        armJob?.cancel()
        val state = stateFlow.value
        if (!isVoiceState(state)) {
            stopListeningIfNeeded()
            return
        }
        armJob = scope.launch {
            delay(ARM_DELAY_MS)
            if (!ttsInProgress) startListeningIfNeeded()
        }
    }

    private fun startListeningIfNeeded() {
        if (listening) return
        listening = true
        robot.startListening()
    }

    private fun stopListeningIfNeeded() {
        if (!listening) return
        listening = false
        robot.stopListening()
    }

    override fun onTtsStatusChanged(status: TtsStatus) {
        when (status) {
            TtsStatus.STARTED -> {
                ttsInProgress = true
                armJob?.cancel()
            }
            TtsStatus.COMPLETED, TtsStatus.ERROR -> {
                ttsInProgress = false
                if (isVoiceState(stateFlow.value)) startListeningIfNeeded()
            }
        }
    }

    override fun onSpeechRecognized(text: String) {
        listening = false
        Log.d(TAG, "들은 말: $text")
        val stateWhenHeard = stateFlow.value
        scope.launch {
            val intent = interpreter.interpret(text, stateWhenHeard, catalog)
            apply(intent, stateWhenHeard)
        }
    }

    private fun apply(intent: VoiceIntent, stateWhenHeard: GuideState) {
        when (intent) {
            is VoiceIntent.SelectDestination -> {
                val dest = catalog.byId(intent.destinationId)
                if (dest == null) {
                    robot.speak(intent.reply.ifBlank { "그 목적지는 안내할 수 없어요." })
                    return
                }
                // 대기 화면에서 바로 목적지 이름을 말해도 선택이 되도록, 먼저 Selecting 으로
                // 올린 뒤 바로 선택 이벤트를 보낸다 — 터치로 치면 "길 안내 시작" + 카드 선택을
                // 한 번에 하는 것과 같다. 어느 쪽이든 Confirming 의 onEnter 가 안내 멘트를
                // 하므로 reply 는 따로 말하지 않는다.
                if (stateWhenHeard is GuideState.Idle) onEvent(GuideEvent.StartRequested)
                onEvent(GuideEvent.DestinationSelected(dest))
            }
            is VoiceIntent.Confirm -> dispatchOrExplain(stateWhenHeard, GuideEvent.UserConfirmed, intent.reply)
            is VoiceIntent.Cancel -> dispatchOrExplain(stateWhenHeard, GuideEvent.UserCancelled, intent.reply)
            is VoiceIntent.Done -> dispatchOrExplain(stateWhenHeard, GuideEvent.ArrivalConfirmed, intent.reply)
            is VoiceIntent.Answer, is VoiceIntent.Unknown ->
                robot.speak(intent.reply.ifBlank { "죄송해요, 다시 한 번 말씀해 주시겠어요?" })
        }
    }

    /**
     * event 를 보내 보고, 실제로 상태가 바뀌었으면(= 지금 맥락에 맞는 말이었으면) 그 상태의
     * onEnter 가 알아서 TTS 를 하므로 아무 것도 더 하지 않는다. 상태가 안 바뀌었으면(= 지금
     * 맥락에 안 맞는 말이었으면, 예: 목적지를 고르기 전에 "네") Gemini 의 reply 로 알려준다.
     * 어느 쪽이 될지는 순수 함수인 [GuideTransitions.transition] 으로 미리 알 수 있다 —
     * 실제로 보내 보지 않아도 결과를 예측할 수 있다는 게 순수 함수의 장점이다.
     */
    private fun dispatchOrExplain(state: GuideState, event: GuideEvent, reply: String) {
        val changed = GuideTransitions.transition(state, event) != state
        onEvent(event)
        if (!changed) robot.speak(reply.ifBlank { "지금은 그렇게 할 수 없어요." })
    }

    private companion object {
        const val TAG = "VoiceController"
        const val ARM_DELAY_MS = 150L
    }
}
