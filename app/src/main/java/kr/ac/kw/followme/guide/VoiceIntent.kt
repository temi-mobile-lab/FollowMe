package kr.ac.kw.followme.guide

import kr.ac.kw.followme.data.DestinationCatalog

/**
 * 음성으로 들어온 말 한 마디를 해석한 결과. Gemini 가 돌려준 JSON 을 이 타입으로 바꾼다.
 * [kr.ac.kw.followme.voice.VoiceController] 가 이 결과를 보고 적절한 [GuideEvent] 로 바꿔
 * `onEvent()` 에 넣는다 — 즉, 음성은 터치와 똑같은 입구(GuideEvent)로 들어오는 또 하나의
 * 입력 채널일 뿐이고, GuideStateMachine/GuideTransitions 는 음성에 대해 아무것도 모른다.
 */
sealed interface VoiceIntent {
    /** 로봇이 소리 내어 답할 한국어 한 문장. */
    val reply: String

    data class SelectDestination(val destinationId: String, override val reply: String) : VoiceIntent
    data class Confirm(override val reply: String) : VoiceIntent
    data class Cancel(override val reply: String) : VoiceIntent
    /** 도착 확인("완료", "고마워" 등). */
    data class Done(override val reply: String) : VoiceIntent
    /** 길 안내와 무관한 질문에 대한 짧은 대답. */
    data class Answer(override val reply: String) : VoiceIntent
    data class Unknown(override val reply: String) : VoiceIntent
}

/**
 * 사람이 한 말(heardText)을 현재 상태(state)와 목적지 목록(catalog)을 참고해 [VoiceIntent] 로
 * 해석한다. 네트워크 호출이 들어갈 수 있어 suspend 다.
 */
interface VoiceIntentInterpreter {
    suspend fun interpret(heardText: String, state: GuideState, catalog: DestinationCatalog): VoiceIntent
}
