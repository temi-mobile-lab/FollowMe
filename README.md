# Follow Me — temi 교내 길 안내 앱

광운대학교 '모바일로봇의 이해' A조 / 새빛관·비마관 동행 길 안내 서비스.

## 현재 진행 상황

| 단계 | 내용 | 상태 |
|---|---|---|
| 1 | 프로젝트 세팅 (gradle, manifest, 패키지 구조) | 완료 |
| 2 | 데이터 모델 + destinations.json 로더 | 완료 |
| 3 | RobotController interface + FakeRobotController | 미착수 |
| 4 | GuideState / GuideEvent / transition() + JUnit | State/Event 정의만 완료 |
| 5 | GuideStateMachine (onEnter, 타임아웃, 로그) | 미착수 (임시 DemoGuideViewModel 사용 중) |
| 6 | UI 화면 | 완료 |
| 7 | TemiRobotController | 미착수 |
| 8 | 확장 기능 (사람 감지, 음성 입력) | 미착수 |

## 패키지 구조

```
kr.ac.kw.followme
├─ MainActivity.kt            키오스크 설정(화면 꺼짐 방지, 시스템 바 숨김) + Compose 진입점
├─ data/
│   ├─ Destination.kt         Destination / RouteStep / DestinationCatalog
│   ├─ DestinationRepository.kt  assets/destinations.json 로더 (org.json)
│   └─ SampleData.kt          @Preview·테스트용 더미 데이터
├─ guide/
│   ├─ GuideState.kt          상태 (Idle ~ Error, WaitingUser)
│   └─ GuideEvent.kt          이벤트 (사용자 / 로봇 콜백 / 타이머)
└─ ui/
    ├─ FollowMeApp.kt         GuideState -> 화면 라우팅
    ├─ DemoGuideViewModel.kt  ⚠️ 임시 드라이버 (5단계에서 교체)
    ├─ theme/                 색·타이포·셰이프
    ├─ components/            PrimaryButton, StatusScreen, RouteProgress
    └─ screens/               대기 / 선택 / 확인 / 안내 / 도착 / 복구 / 복귀 / 오류
```

## 빌드 · 실행

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug

# 에뮬레이터 또는 temi
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n kr.ac.kw.followme/.MainActivity

# temi 실기기 (개발자 모드 켠 뒤)
adb connect <temi-IP>:5555
```

### 에뮬레이터를 temi 태블릿 화면에 맞추기

temi 태블릿은 10.1인치 1280x800 이라 dp 기준으로도 약 1280x800 이다(밀도 추정치, **확인 필요**).
일반 태블릿 AVD 는 dp 가 훨씬 작아 레이아웃이 잘려 보이므로 테스트 전에 맞춰 준다.

```bash
adb shell wm size 1280x800
adb shell wm density 160
# 테스트 후 되돌리기
adb shell wm size reset && adb shell wm density reset
```

## 테미에서 음성 안내 사용하기

대기 화면에 **음성 안내 설정**이 보이면 눌러 Follow Me를 키오스크 앱으로 지정한다.
“엘리베이터로 안내해 줘” 또는 “새빛관 출입문으로 가자”라고 말하면 테미가 목적지를 다시 읽는다.
“네”라고 답하거나 **안내 시작**을 누르면 출발한다. 도착 후에는 다음 목적지를 말하거나 “홈베이스로 돌아가”라고 말할 수 있다.
터치와 음성은 함께 사용할 수 있다. 기본 안내 명령에는 Gemini API 키가 필요 없다.

## 목적지 데이터

`app/src/main/assets/destinations.json` 만 고치면 맵이 바뀌어도 코드 수정이 필요 없다.
`route[].location` 은 temi 에 저장된 위치 이름과 **철자까지 같아야** 한다.
새빛관 1층은 `새빛관 출입문`, `1층 엘리베이터`, `101호`부터 `106호`까지 각 위치를 테미 지도에 저장한다. 앱은 출발 전에 필요한 위치가 등록되어 있는지 확인한다.
2·3층 강의실을 선택하면 1층 엘리베이터까지 안내한다. 엘리베이터에 도착하면 층별 안내도 QR이 열리고, 같은 화면에서 설문 QR로 전환하거나 다음 목적지를 선택할 수 있다.

## 빌드 환경 메모

- AGP 9 부터 Kotlin 지원이 AGP 에 내장되어 있어 `org.jetbrains.kotlin.android` 플러그인을 적용하면 안 된다.
- `minSdk 23` (temi V2 = Android 6.0.1). temi V3 는 Android 11.
- temi SDK 의존성은 7단계에서 추가한다 (`app/build.gradle.kts` 주석 참고).
