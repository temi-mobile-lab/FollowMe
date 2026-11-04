package kr.ac.kw.followme.robot

/**
 * temi SDK를 감싸는 경계. GuideStateMachine/VoiceController 는 이 인터페이스만 알고, 실제 로봇
 * ([TemiRobotController]) 이든 가짜 로봇([FakeRobotController])이든 신경 쓰지 않는다.
 *
 * 명령(goTo, speak, startListening)은 결과를 즉시 리턴하지 않는다 — 결과는 [RobotListener]
 * 콜백으로 비동기로 온다. temi SDK 의 OnGoToLocationStatusChangedListener / TtsListener /
 * AsrListener 모델과 같다. 각 enum/메서드는 temi SDK 1.138.0 소스(Robot.kt)를 직접 확인하고
 * 맞췄다 — goTo 의 파라미터 구성, GoToStatus/TtsStatus 의 문자열 값, SpeedLevel 값 모두 실제
 * SDK 와 일치한다.
 */
interface RobotController {

    /**
     * 로봇(SDK)에 연결하고 콜백을 받기 시작한다. MainActivity.onStart 에서 호출한다.
     * [TemiRobotController] 는 여기서 Robot 싱글턴에 리스너를 등록한다. [FakeRobotController]
     * 는 아무 것도 할 필요가 없어 빈 구현이다.
     */
    fun start()

    /** MainActivity.onStop 에서 호출. 리스너 등록을 해제한다. */
    fun stop()

    /** 이 컨트롤러의 이벤트를 받을 대상을 등록/해제한다 (여러 개 등록 가능 — GuideStateMachine, VoiceController 등). */
    fun addListener(listener: RobotListener)
    fun removeListener(listener: RobotListener)

    /**
     * 저장된 위치로 이동을 시작한다. 결과는 [RobotListener.onGoToStatusChanged] 로 온다.
     * @param speed temi SpeedLevel. 동행 안내는 기본적으로 저속(SLOW)을 쓴다.
     */
    fun goTo(location: String, speed: GoToSpeed = GoToSpeed.SLOW)

    /** 진행 중인 이동을 멈춘다. */
    fun stopMovement()

    /** TTS 를 재생한다. 결과는 [RobotListener.onTtsStatusChanged] 로 온다. */
    fun speak(text: String)

    /** temi 에 저장된 위치 이름 목록. destinations.json 의 location 과 대조하는 데 쓴다. */
    fun getLocations(): List<String>

    /** 로봇(SDK)이 명령을 받을 준비가 됐는지. OnRobotReadyListener 대응. */
    fun isReady(): Boolean

    /**
     * 음성 인식을 시작한다(temi 의 wakeup). 사람이 말을 마치면 결과가
     * [RobotListener.onSpeechRecognized] 로 온다. 마이크 권한은 필요 없다 — temi 런처가
     * 처리하고 SDK 는 AIDL 로만 요청한다.
     */
    fun startListening()

    /** 듣기를 중단한다(temi 의 finishConversation). */
    fun stopListening()
}

/** temi SDK 의 SpeedLevel 과 1:1 대응 (navigation.model.SpeedLevel). */
enum class GoToSpeed { VERY_SLOW, SLOW, MEDIUM, HIGH, VERY_HIGH }

/** OnGoToLocationStatusChangedListener 의 status 문자열과 대응. */
enum class GoToStatus { START, CALCULATING, GOING, COMPLETE, ABORT, REPOSING }

/**
 * TtsRequest.Status 중 이 앱이 구분해서 쓰는 것만 추렸다.
 * NOT_ALLOWED/CANCELED 도 다음 동작으로 못 넘어간다는 점에서 ERROR 와 똑같이 취급한다
 * (TemiRobotController 에서 셋 다 ERROR 로 매핑).
 */
enum class TtsStatus { STARTED, COMPLETED, ERROR }

/** (확장) OnDetectionStateChangedListener 의 상태와 대응 — 사람 감지. */
enum class DetectionState { IDLE, LOST, DETECTED }

/**
 * 로봇 콜백. 기본 구현을 비워 둬서(관심 없는 콜백은 override 안 해도 되게) 구현 부담을 줄였다.
 */
interface RobotListener {
    fun onGoToStatusChanged(location: String, status: GoToStatus, description: String = "") {}
    fun onTtsStatusChanged(status: TtsStatus) {}
    fun onRobotReady(isReady: Boolean) {}

    /** 음성 인식 결과(사람이 한 말 전체 문장). */
    fun onSpeechRecognized(text: String) {}

    /** (확장) 사람 감지 상태 변화. */
    fun onDetectionStateChanged(state: DetectionState) {}
}
