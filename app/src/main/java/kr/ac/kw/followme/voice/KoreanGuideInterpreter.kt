package kr.ac.kw.followme.voice

import kr.ac.kw.followme.data.DestinationCatalog
import kr.ac.kw.followme.guide.GuideState
import kr.ac.kw.followme.guide.VoiceIntent
import kr.ac.kw.followme.guide.VoiceIntentInterpreter

class KoreanGuideInterpreter(private val fallback: VoiceIntentInterpreter? = null) : VoiceIntentInterpreter {
    override suspend fun interpret(heardText: String, state: GuideState, catalog: DestinationCatalog): VoiceIntent {
        val command = compact(heardText).removePrefix("테미야")
        if (state is GuideState.Confirming && command in confirmations) return VoiceIntent.Confirm("")
        if (state is GuideState.Arrived && command in returnCommands) return VoiceIntent.Done("")
        if (command in cancellations) return VoiceIntent.Cancel("")

        val matches = catalog.destinations.filter { destination ->
            val names = destination.voiceAliases + destination.displayName
            val prefixes = listOf("", compact(destination.building)) +
                listOfNotNull(destination.floor?.let { "${it}층" }, destination.floor?.let { compact(destination.building) + "${it}층" })
            names.any { name ->
                val alias = compact(name)
                alias.isNotEmpty() && prefixes.any { prefix ->
                    val place = prefix + alias
                    command.startsWith(place) && requestEnding.matches(command.removePrefix(place))
                }
            }
        }
        val destination = matches.singleOrNull()
        if (destination != null) {
            if (state is GuideState.Arrived && state.dest.id == destination.id) {
                return VoiceIntent.Answer(destination.arrivalMessage)
            }
            return VoiceIntent.SelectDestination(destination.id, "")
        }
        return fallback?.interpret(heardText, state, catalog) ?: VoiceIntent.Unknown(
            when (state) {
                is GuideState.Confirming -> "출발하려면 네, 다시 고르려면 취소라고 말씀해 주세요."
                is GuideState.Arrived -> "다음 목적지나 홈베이스 복귀라고 말씀해 주세요."
                else -> "안내할 목적지 이름을 말씀해 주세요."
            }
        )
    }

    private fun compact(text: String) = text.lowercase().replace(separators, "")

    private companion object {
        val separators = Regex("""[\s\p{P}]+""")
        val requestEnding = Regex(
            "(?:으로|로|에|까지|이요|요)?(?:안내|데려다|이동|가고싶|가는길알려|어디|가|데려가)?" +
                "(?:해|해줘|해줘요|해주세요|부탁해|부탁해요|줘|줘요|주세요|자|요|어|어요|예요|야)?"
        )
        val confirmations = setOf("네", "예", "응", "그래", "좋아요", "출발", "출발해", "출발해줘", "시작", "시작해줘", "가자", "안내시작")
        val cancellations = setOf("아니", "아니요", "취소", "안내취소", "취소해줘", "다시선택", "다시선택할래")
        val returnCommands = setOf("홈베이스", "홈베이스복귀", "홈베이스로복귀", "홈베이스로돌아가", "홈베이스로돌아가줘",
            "홈베이스로가줘", "홈베이스로가", "복귀", "안내완료", "안내종료", "안내끝", "완료", "끝")
    }
}
