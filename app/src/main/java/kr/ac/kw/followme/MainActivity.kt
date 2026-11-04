package kr.ac.kw.followme

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.ac.kw.followme.ui.FollowMeApp
import kr.ac.kw.followme.ui.GuideViewModel

class MainActivity : ComponentActivity() {

    // Compose 의 viewModel() 델리게이트 대신 Activity 레벨에서 직접 들고 있는다 —
    // onStart/onStop 에서 로봇 리스너를 등록/해제하려면 Compose 첫 컴포지션 전에도
    // 인스턴스가 있어야 하기 때문이다. (리스너는 onStart 에서 add, onStop 에서 remove)
    private val viewModel: GuideViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 로비에 상시 띄워 두는 키오스크 화면이라 꺼지지 않게 하고 시스템 바를 숨긴다.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enterImmersiveMode()

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()

            FollowMeApp(
                state = state,
                destinations = viewModel.catalog.destinations,
                onEvent = viewModel::onEvent,
                // 실제 temi 배포 빌드(USE_REAL_ROBOT=true)에서는 디버그 입력창을 아예 숨긴다.
                onSimulateVoice = if (!BuildConfig.USE_REAL_ROBOT) viewModel::simulateVoiceInput else null,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onStop() {
        viewModel.onStop()
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // 키보드가 올라왔다 내려가면 시스템 바가 다시 나타나므로 포커스를 받을 때마다 숨긴다.
        if (hasFocus) enterImmersiveMode()
    }

    private fun enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
