package kr.ac.kw.followme.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kr.ac.kw.followme.BuildConfig
import kr.ac.kw.followme.data.CsvGuideLogger
import kr.ac.kw.followme.data.DestinationCatalog
import kr.ac.kw.followme.data.DestinationRepository
import kr.ac.kw.followme.gemini.GeminiClient
import kr.ac.kw.followme.gemini.GeminiDestinationInterpreter
import kr.ac.kw.followme.guide.GuideEvent
import kr.ac.kw.followme.guide.GuideStateMachine
import kr.ac.kw.followme.robot.FakeRobotController
import kr.ac.kw.followme.robot.RobotController
import kr.ac.kw.followme.robot.RobotListener
import kr.ac.kw.followme.robot.TemiRobotController
import kr.ac.kw.followme.voice.VoiceController

/**
 * 3~8단계 공용 ViewModel. 로봇(터치 상태 머신)과 음성(Gemini) 두 입력 채널을 여기서 묶는다.
 *
 * 로봇은 local.properties 의 `USE_REAL_ROBOT` 값으로 고른다 — 기본은 false(FakeRobotController)
 * 라서 로봇 없이도 에뮬레이터에서 전체 흐름을 테스트할 수 있다(로봇 사용 시간이 제한적이기
 * 때문). 실제 temi 에 배포할 때만 local.properties 에 `USE_REAL_ROBOT=true` 를 추가한다.
 */
class GuideViewModel(application: Application) : AndroidViewModel(application) {

    val catalog: DestinationCatalog = DestinationRepository(application).load()

    private val robot: RobotController = if (BuildConfig.USE_REAL_ROBOT) {
        TemiRobotController(application)
    } else {
        // abortRate 는 Recovering/Error 경로를 에뮬레이터에서도 눈으로 확인하려고 일부러 넣었다.
        FakeRobotController(
            scope = viewModelScope,
            abortRate = 0.15f,
            savedLocations = catalog.destinations.flatMap { it.locations }.toSet() + catalog.homeBase,
        )
    }

    private val machine = GuideStateMachine(
        robot = robot,
        logger = CsvGuideLogger(application),
        scope = viewModelScope,
        homeBase = catalog.homeBase,
    )

    val state = machine.state

    // GEMINI_API_KEY 가 비어 있으면(local.properties 에 키를 안 넣었으면) 음성 기능 자체를
    // 꺼 둔다 — 키 없이 Gemini 를 호출하면 매번 401 로 실패하기만 하므로, 아예 안 켜는 게 낫다.
    private val voiceController: VoiceController? = if (BuildConfig.GEMINI_API_KEY.isNotBlank()) {
        VoiceController(
            robot = robot,
            interpreter = GeminiDestinationInterpreter(GeminiClient(BuildConfig.GEMINI_API_KEY)),
            catalog = catalog,
            stateFlow = state,
            onEvent = ::onEvent,
            scope = viewModelScope,
        )
    } else {
        Log.w(TAG, "GEMINI_API_KEY 가 없어 음성 안내를 켜지 않습니다. local.properties 를 확인하세요.")
        null
    }

    init {
        // 위치 목록은 로봇과 실제로 연결된 뒤에야 의미가 있다(TemiRobotController 는 연결 전엔
        // 빈 목록을 돌려준다). onRobotReady 콜백에서 한 번 확인한다.
        robot.addListener(object : RobotListener {
            override fun onRobotReady(isReady: Boolean) {
                if (isReady) checkMissingLocations()
            }
        })
    }

    fun onEvent(event: GuideEvent) = machine.onEvent(event)

    /**
     * 에뮬레이터/개발 중 마이크 없이 음성 입력을 흉내 낼 때 쓴다. 실제 temi 에서는 쓸 일이
     * 없다(FollowMeApp 이 USE_REAL_ROBOT 일 때 이 입력창 자체를 숨긴다).
     */
    fun simulateVoiceInput(text: String) {
        (robot as? FakeRobotController)?.apply {
            startListening() // 듣는 중 상태로 만들어야 simulateSpeech 가 반영된다
            simulateSpeech(text)
        }
    }

    /** MainActivity.onStart 에서 호출. */
    fun onStart() {
        robot.start()
        machine.start()
        voiceController?.start()
    }

    /** MainActivity.onStop 에서 호출. (등록의 역순으로 해제) */
    fun onStop() {
        voiceController?.stop()
        machine.stop()
        robot.stop()
    }

    private fun checkMissingLocations() {
        val missing = catalog.missingLocations(robot.getLocations())
        if (missing.isNotEmpty()) {
            Log.w(TAG, "temi 에 없는 위치 이름: $missing")
        }
    }

    private companion object {
        const val TAG = "GuideViewModel"
    }
}
