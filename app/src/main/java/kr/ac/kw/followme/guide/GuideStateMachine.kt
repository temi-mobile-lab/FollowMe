package kr.ac.kw.followme.guide

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.ac.kw.followme.robot.GoToStatus
import kr.ac.kw.followme.robot.RobotController
import kr.ac.kw.followme.robot.RobotListener
import kr.ac.kw.followme.robot.TtsStatus

/**
 * 안내 서비스의 핵심 로직.
 *
 * - 상태 전이 자체는 [GuideTransitions.transition](순수 함수)에 전부 위임한다.
 * - side effect(goTo, speak, 타임아웃 예약)는 오직 [onEnter] 에서만 실행한다.
 * - 로봇 콜백([RobotListener])은 전부 GuideEvent 로 바꿔 [dispatch] 로 들여보낸다 —
 *   그래야 "로봇에서 왔든 UI에서 왔든 같은 전이표를 탄다"가 보장된다.
 * - 정의되지 않은 (state, event) 조합(transition 이 state 를 그대로 돌려주는 경우)은
 *   로그를 남기지 않는다 — CSV 에는 실제로 상태가 바뀐 경우만 쌓인다.
 */
class GuideStateMachine(
    private val robot: RobotController,
    private val logger: GuideLogger = GuideLogger.NOOP,
    private val scope: CoroutineScope,
    private val homeBase: String = "home base",
    private val selectTimeoutMillis: Long = 60_000,
    private val recoverDelayMillis: Long = 2_500,
) : RobotListener {

    private val _state = MutableStateFlow<GuideState>(GuideState.Idle)
    val state: StateFlow<GuideState> = _state.asStateFlow()

    private var timeoutJob: Job? = null

    /** Activity.onStart 에서 호출. 로봇 콜백을 받기 시작한다. */
    fun start() {
        robot.addListener(this)
    }

    /** Activity.onStop 에서 호출. */
    fun stop() {
        robot.removeListener(this)
        timeoutJob?.cancel()
    }

    /** UI 에서 올라오는 사용자 이벤트의 진입점. */
    fun onEvent(event: GuideEvent) = dispatch(event)

    private fun dispatch(event: GuideEvent) {
        val prev = _state.value
        val next = GuideTransitions.transition(prev, event)
        if (next == prev) return // 정의되지 않은 조합은 조용히 무시

        logger.log(
            GuideLogEntry(
                timestamp = System.currentTimeMillis(),
                prevState = prev.name,
                event = event.name,
                nextState = next.name,
                destination = (next.destination ?: prev.destination)?.id,
                segment = next.segment ?: prev.segment,
            )
        )
        _state.value = next
        onEnter(next)
    }

    /** 상태에 들어갈 때만 실행되는 side effect. 여기 말고는 goTo/speak 를 부르지 않는다. */
    private fun onEnter(state: GuideState) {
        timeoutJob?.cancel()
        when (state) {
            is GuideState.Idle -> Unit

            is GuideState.Selecting -> scheduleTimeout(selectTimeoutMillis)

            is GuideState.Confirming ->
                robot.speak("선택한 목적지로 안내합니다. 따라오세요.")

            is GuideState.Navigating -> {
                val step = state.dest.route.getOrNull(state.seg)
                if (step == null) {
                    // route 인덱스가 범위를 벗어나는 건 데이터 오류다. abort 로 취급해서
                    // 기존 Recovering/Error 경로를 그대로 타게 한다 (별도 처리 분기를 늘리지 않음).
                    dispatch(GuideEvent.GoToAbort("구간 인덱스(${state.seg}) 범위 초과"))
                } else {
                    robot.goTo(step.location)
                }
            }

            is GuideState.WaypointGuide -> {
                val say = state.dest.route.getOrNull(state.seg)?.say.orEmpty()
                if (say.isBlank()) {
                    // 말할 내용이 없으면 실제 TTS 없이 바로 다음 구간으로 넘어간다.
                    dispatch(GuideEvent.TtsCompleted)
                } else {
                    robot.speak(say)
                }
            }

            is GuideState.Arrived ->
                if (state.dest.arrivalMessage.isNotBlank()) robot.speak(state.dest.arrivalMessage)

            is GuideState.Recovering -> {
                robot.speak("잠시만 기다려주세요. 경로를 다시 확인하고 있습니다.")
                scheduleTimeout(recoverDelayMillis)
            }

            is GuideState.Returning -> {
                // 작별 인사와 이동을 동시에 시작한다 (temi 는 이동 중에도 말할 수 있다 — 확인 필요).
                robot.speak("안내를 마칩니다. 대기 장소로 돌아가는 중입니다. 이용해 주셔서 감사합니다.")
                robot.goTo(homeBase)
            }

            is GuideState.Error ->
                robot.speak("죄송합니다. 지금은 길 안내를 마칠 수 없습니다. 가까운 안내판을 확인하거나 사무실에 문의해 주세요.")

            is GuideState.WaitingUser ->
                robot.speak("여기서 기다리고 있을게요. 가까이 오시면 다시 출발합니다.")
        }
    }

    private fun scheduleTimeout(delayMillis: Long) {
        timeoutJob = scope.launch {
            delay(delayMillis)
            dispatch(GuideEvent.Timeout)
        }
    }

    // --- RobotListener: 로봇 콜백을 GuideEvent 로 변환해 dispatch 로 넘긴다 -----------

    override fun onGoToStatusChanged(location: String, status: GoToStatus, description: String) {
        when (status) {
            GoToStatus.COMPLETE -> dispatch(GuideEvent.GoToComplete)
            GoToStatus.ABORT -> dispatch(GuideEvent.GoToAbort(description))
            // START/CALCULATING/GOING/REPOSING: 지금은 화면 갱신이 필요 없어 무시한다.
            GoToStatus.START, GoToStatus.CALCULATING, GoToStatus.GOING, GoToStatus.REPOSING -> Unit
        }
    }

    override fun onTtsStatusChanged(status: TtsStatus) {
        if (status == TtsStatus.COMPLETED) dispatch(GuideEvent.TtsCompleted)
    }

    override fun onRobotReady(isReady: Boolean) = Unit
}
