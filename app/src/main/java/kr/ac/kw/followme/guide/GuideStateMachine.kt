package kr.ac.kw.followme.guide

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.ac.kw.followme.robot.GoToStatus
import kr.ac.kw.followme.robot.RobotBattery
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
    private val standbyLocation: String = "새빛관 출입문",
    private val selectTimeoutMillis: Long = 60_000,
    private val recoverDelayMillis: Long = 2_500,
) : RobotListener {

    private val _state = MutableStateFlow<GuideState>(GuideState.Idle)
    val state: StateFlow<GuideState> = _state.asStateFlow()

    private var timeoutJob: Job? = null
    private var connected = false
    private val _adminMode = MutableStateFlow(false)
    val adminMode = _adminMode.asStateFlow()
    private val _battery = MutableStateFlow<RobotBattery?>(null)
    val battery = _battery.asStateFlow()

    fun startBatteryMonitoring(simulateLow: Boolean) {
        val previous = state.value
        if (!robot.isReady() || (previous != GuideState.Idle && previous !is GuideState.Error)) return
        _adminMode.value = true
        onBatteryChanged(robot.getBattery())
        if (simulateLow && state.value == previous) dispatch(GuideEvent.BatteryReturn)
    }

    fun exitAdminMode() {
        _adminMode.value = false
        if (state.value == GuideState.Docking || state.value == GuideState.Charging) {
            dispatch(GuideEvent.ReturnToStandby)
        }
    }

    /** Activity.onStart 에서 호출. 로봇 콜백을 받기 시작한다. */
    fun start() {
        robot.addListener(this)
    }

    /** Activity.onStop 에서 호출. */
    fun stop() {
        robot.removeListener(this)
        timeoutJob?.cancel()
        connected = false
        _battery.value = null
    }

    /** UI 에서 올라오는 사용자 이벤트의 진입점. */
    fun onEvent(event: GuideEvent) = dispatch(event)

    private fun dispatch(event: GuideEvent) {
        if (event == GuideEvent.BatteryReturn && !_adminMode.value) return
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
        if (prev == GuideState.Docking && next is GuideState.Error) _adminMode.value = false
        onEnter(next, event)
    }

    /** 상태에 들어갈 때만 실행되는 side effect. 여기 말고는 goTo/speak 를 부르지 않는다. */
    private fun onEnter(state: GuideState, event: GuideEvent) {
        timeoutJob?.cancel()
        when (state) {
            is GuideState.Idle -> Unit

            is GuideState.Selecting -> scheduleTimeout(selectTimeoutMillis)

            is GuideState.Confirming ->
                robot.speak(with(state.dest) {
                    if (viaElevator) "${displayName}는 ${floor}층에 있습니다. ${finalLocation}까지 안내할까요?"
                    else "${displayName}까지 안내할까요?"
                })

            is GuideState.Navigating -> {
                val step = state.dest.route.getOrNull(state.seg)
                if (step == null) {
                    // route 인덱스가 범위를 벗어나는 건 데이터 오류다. abort 로 취급해서
                    // 기존 Recovering/Error 경로를 그대로 타게 한다 (별도 처리 분기를 늘리지 않음).
                    dispatch(GuideEvent.GoToAbort("구간 인덱스(${state.seg}) 범위 초과"))
                } else {
                    if (event == GuideEvent.UserConfirmed) {
                        robot.speak("안내를 시작합니다. 저를 따라오세요.")
                    }
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
                if (event != GuideEvent.ReturnToStandby) robot.speak("안내를 마치고 새빛관 출입문으로 돌아갑니다.")
                robot.goTo(standbyLocation)
            }

            GuideState.Docking -> {
                robot.speak("배터리가 80퍼센트 이하이므로 충전을 위해 홈베이스로 이동합니다.")
                if (_battery.value?.isCharging == true) dispatch(GuideEvent.DockDetected)
                else robot.goTo(homeBase)
            }

            GuideState.Charging -> onBatteryChanged(robot.getBattery())

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
        val expected = when (val current = state.value) {
            is GuideState.Navigating -> current.dest.route.getOrNull(current.seg)?.location
            GuideState.Returning -> standbyLocation
            GuideState.Docking -> homeBase
            else -> null
        } ?: return
        if (!location.trim().equals(expected.trim(), ignoreCase = true)) return
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

    override fun onRobotReady(isReady: Boolean) {
        if (!isReady) {
            connected = false
            _battery.value = null
            return
        }
        if (connected) return
        connected = true
        val previous = state.value
        onBatteryChanged(robot.getBattery())
        if (previous != state.value) return
        when (previous) {
            GuideState.Idle -> dispatch(GuideEvent.ReturnToStandby)
            GuideState.Returning -> robot.goTo(standbyLocation)
            GuideState.Docking -> if (_adminMode.value) robot.goTo(homeBase)
            else -> Unit
        }
    }

    override fun onBatteryChanged(battery: RobotBattery?) {
        val reading = battery?.takeIf { it.percent in 0..100 }
        _battery.value = reading
        if (!_adminMode.value || reading == null || !robot.isReady()) return
        when (state.value) {
            GuideState.Docking -> if (reading.isCharging) dispatch(GuideEvent.DockDetected)
            GuideState.Charging -> if (reading.percent == 100) {
                exitAdminMode()
            }
            else -> if (reading.percent <= 80) dispatch(GuideEvent.BatteryReturn)
        }
    }
}
