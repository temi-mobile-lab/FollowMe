package kr.ac.kw.followme.robot

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.robotemi.sdk.Robot
import com.robotemi.sdk.SttLanguage
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.listeners.OnDetectionStateChangedListener
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import com.robotemi.sdk.listeners.OnRobotReadyListener
import com.robotemi.sdk.navigation.model.SpeedLevel
import com.robotemi.sdk.voice.WakeupRequest

/**
 * 실제 temi 로봇을 감싸는 [RobotController] 구현.
 *
 * temi SDK 1.138.0 소스(Robot.kt)를 직접 읽고 맞췄다 — goTo 의 파라미터 순서, 각 리스너의
 * status 문자열 값, TtsRequest.Status, SpeedLevel 값이 전부 실제 SDK 와 일치한다.
 * 그 외 음성(wakeup/AsrListener)과 top bar 등록(onStart) 패턴은 사내 레퍼런스 프로젝트
 * (LibraryRobot)에서 실제로 동작을 확인한 방식을 그대로 따랐다.
 *
 * ⚠️ 실기기 확인 필요:
 *  - 말하는 도중(SPEAKING) wakeup() 을 부르면 자기 목소리를 주워 담는지, SDK/하드웨어가
 *    에코캔슬링을 해 주는지. VoiceController 는 TTS 가 끝난 뒤에만 듣기 시작하도록
 *    짜여 있지만, 실제 타이밍은 로봇에서 튜닝이 필요할 수 있다.
 *  - onRobotReady 가 호출되는 시점(앱 포그라운드 복귀 때마다인지, 최초 1회인지).
 *
 * @param context Application context 를 받는다(Activity 가 아니어도 된다).
 */
class TemiRobotController(private val context: Context) :
    RobotController,
    OnRobotReadyListener,
    Robot.TtsListener,
    Robot.AsrListener,
    OnGoToLocationStatusChangedListener,
    OnDetectionStateChangedListener {

    private val robot: Robot = Robot.getInstance()
    private val listeners = mutableSetOf<RobotListener>()

    @Volatile
    private var ready = false

    override fun start() {
        robot.addOnRobotReadyListener(this)
        robot.addTtsListener(this)
        robot.addAsrListener(this)
        robot.addOnGoToLocationStatusChangedListener(this)
        robot.addOnDetectionStateChangedListener(this)
    }

    override fun stop() {
        robot.removeOnRobotReadyListener(this)
        robot.removeTtsListener(this)
        robot.removeAsrListener(this)
        robot.removeOnGoToLocationStatusChangedListener(this)
        robot.removeOnDetectionStateChangedListener(this)
    }

    override fun addListener(listener: RobotListener) {
        listeners += listener
    }

    override fun removeListener(listener: RobotListener) {
        listeners -= listener
    }

    override fun goTo(location: String, speed: GoToSpeed) {
        robot.goTo(location = location, speedLevel = speed.toSpeedLevel())
    }

    override fun stopMovement() {
        robot.stopMovement()
    }

    override fun speak(text: String) {
        // 로봇/시스템 설정 언어와 상관없이 항상 한국어로 말한다 — 이 앱은 한국어 전용이다.
        robot.speak(TtsRequest.create(text, true, TtsRequest.Language.KO_KR))
    }

    override fun getLocations(): List<String> = robot.locations

    override fun isReady(): Boolean = ready

    override fun startListening() {
        robot.wakeup(listOf(SttLanguage.KO_KR), WakeupRequest(wakeupResponse = false, newSession = false))
    }

    override fun stopListening() {
        robot.finishConversation()
    }

    // --- temi SDK 콜백 → 우리 RobotListener 로 변환 ------------------------------

    override fun onRobotReady(isReady: Boolean) {
        ready = isReady
        if (isReady) registerTopBarEntry()
        listeners.toList().forEach { it.onRobotReady(isReady) }
    }

    /** 앱을 temi 상단바(Skill 목록)에 등록한다. 로봇과 연결된 뒤 한 번 해 주면 된다. */
    private fun registerTopBarEntry() {
        try {
            val component = ComponentName(context, "kr.ac.kw.followme.MainActivity")
            val info = context.packageManager.getActivityInfo(component, PackageManager.GET_META_DATA)
            robot.onStart(info)
        } catch (e: Exception) {
            Log.e(TAG, "temi 상단바 등록 실패", e)
        }
    }

    override fun onGoToLocationStatusChanged(
        location: String,
        status: String,
        descriptionId: Int,
        description: String,
    ) {
        val mapped = when (status) {
            OnGoToLocationStatusChangedListener.START -> GoToStatus.START
            OnGoToLocationStatusChangedListener.CALCULATING -> GoToStatus.CALCULATING
            OnGoToLocationStatusChangedListener.GOING -> GoToStatus.GOING
            OnGoToLocationStatusChangedListener.COMPLETE -> GoToStatus.COMPLETE
            OnGoToLocationStatusChangedListener.ABORT -> GoToStatus.ABORT
            OnGoToLocationStatusChangedListener.REPOSING -> GoToStatus.REPOSING
            else -> {
                Log.w(TAG, "알 수 없는 goTo 상태: $status")
                return
            }
        }
        listeners.toList().forEach { it.onGoToStatusChanged(location, mapped, description) }
    }

    override fun onTtsStatusChanged(ttsRequest: TtsRequest) {
        val mapped = when (ttsRequest.status) {
            TtsRequest.Status.STARTED -> TtsStatus.STARTED
            TtsRequest.Status.COMPLETED -> TtsStatus.COMPLETED
            // ERROR/NOT_ALLOWED/CANCELED 는 "말이 끝나지 못했다"는 점에서 전부 ERROR 로 합친다.
            TtsRequest.Status.ERROR, TtsRequest.Status.NOT_ALLOWED, TtsRequest.Status.CANCELED -> TtsStatus.ERROR
            // PENDING/PROCESSING 은 중간 단계라 상태 머신이 신경 쓸 필요가 없어 무시한다.
            TtsRequest.Status.PENDING, TtsRequest.Status.PROCESSING -> return
        }
        listeners.toList().forEach { it.onTtsStatusChanged(mapped) }
    }

    override fun onAsrResult(asrResult: String, sttLanguage: SttLanguage) {
        listeners.toList().forEach { it.onSpeechRecognized(asrResult) }
    }

    override fun onDetectionStateChanged(state: Int) {
        val mapped = when (state) {
            OnDetectionStateChangedListener.IDLE -> DetectionState.IDLE
            OnDetectionStateChangedListener.LOST -> DetectionState.LOST
            OnDetectionStateChangedListener.DETECTED -> DetectionState.DETECTED
            else -> return
        }
        listeners.toList().forEach { it.onDetectionStateChanged(mapped) }
    }

    private fun GoToSpeed.toSpeedLevel(): SpeedLevel = when (this) {
        GoToSpeed.VERY_SLOW -> SpeedLevel.VERY_SLOW
        GoToSpeed.SLOW -> SpeedLevel.SLOW
        GoToSpeed.MEDIUM -> SpeedLevel.MEDIUM
        GoToSpeed.HIGH -> SpeedLevel.HIGH
        GoToSpeed.VERY_HIGH -> SpeedLevel.VERY_HIGH
    }

    private companion object {
        const val TAG = "TemiRobotController"
    }
}
