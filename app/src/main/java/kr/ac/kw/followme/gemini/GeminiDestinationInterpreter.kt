package kr.ac.kw.followme.gemini

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.ac.kw.followme.data.DestinationCatalog
import kr.ac.kw.followme.guide.GuideState
import kr.ac.kw.followme.guide.VoiceIntent
import kr.ac.kw.followme.guide.VoiceIntentInterpreter
import org.json.JSONArray
import org.json.JSONObject

/**
 * [VoiceIntentInterpreter] 의 Gemini 구현. LibraryRobot 레퍼런스 프로젝트의 "문장 → JSON"
 * 스키마 패턴(anyOf + Structured Output)을 FollowMe 의 목적지 선택 도메인에 맞게 옮겼다.
 *
 * action 마다 필요한 필드만 anyOf 로 나눠서 모델이 엉뚱한 필드를 채우지 않게 한다:
 *  - selectDestination  destinationId(목적지 목록 중 하나) + reply
 *  - confirm / cancel / done / answer / unknown  reply 만
 */
class GeminiDestinationInterpreter(
    private val client: GeminiClient,
) : VoiceIntentInterpreter {

    override suspend fun interpret(
        heardText: String,
        state: GuideState,
        catalog: DestinationCatalog,
    ): VoiceIntent = withContext(Dispatchers.IO) {
        try {
            val answer = client.ask(heardText, systemPrompt(state, catalog), schema(catalog))
            parse(JSONObject(answer))
        } catch (e: Exception) {
            VoiceIntent.Unknown("죄송해요, 다시 한 번 말씀해 주시겠어요?")
        }
    }

    private fun parse(result: JSONObject): VoiceIntent {
        val reply = result.optString("reply", "")
        return when (result.optString("action")) {
            "selectDestination" -> VoiceIntent.SelectDestination(result.optString("destinationId"), reply)
            "confirm" -> VoiceIntent.Confirm(reply)
            "cancel" -> VoiceIntent.Cancel(reply)
            "done" -> VoiceIntent.Done(reply)
            "answer" -> VoiceIntent.Answer(reply)
            else -> VoiceIntent.Unknown(reply.ifBlank { "무슨 말씀인지 잘 못 들었어요. 다시 말씀해 주시겠어요?" })
        }
    }

    private fun systemPrompt(state: GuideState, catalog: DestinationCatalog): String {
        val destinationList = catalog.destinations.joinToString("\n") {
            "- ${it.id}: ${it.displayName} (${it.building} · ${it.category})"
        }
        return """
            너는 광운대학교 새빛관·비마관을 안내하는 로봇 temi 다. 사용자의 말을 듣고 무엇을 할지 정한다.
            지금 상황: ${describe(state)}

            안내할 수 있는 목적지 목록(id: 이름 (건물 · 분류)):
            $destinationList

            - 사용자가 목적지 이름이나 호실을 말하면(예: "비마관 104호 가고 싶어", "로봇학부 사무실로 안내해줘")
              action=selectDestination, destinationId 에 위 목록의 id 를 정확히 넣는다.
              목록에 없는 곳(화장실, 다른 건물 등)을 말하면 action=answer 로 안내할 수 없다고 말한다.
            - "네", "가자", "시작해줘", "그래" 처럼 동의하는 말이면 action=confirm.
            - "아니", "취소", "그만", "다시 선택할래" 처럼 멈추거나 되돌리고 싶어하면 action=cancel.
            - "다 왔어", "고마워", "끝", "완료" 처럼 안내를 마치고 싶어하면 action=done.
            - 길 안내와 무관한 질문이면 action=answer 로 한국어로 짧게 답한다. 모르는 정보는 지어내지 않는다.
            - 무슨 말인지 모르겠으면 action=unknown 으로 다시 말해 달라고 한다.
            reply 는 로봇이 소리 내어 말할 한국어 한 문장이다.
        """.trimIndent()
    }

    private fun describe(state: GuideState): String = when (state) {
        is GuideState.Idle -> "대기 중 — 아직 목적지를 고르지 않았다."
        is GuideState.Selecting -> "목적지를 고르는 중이다."
        is GuideState.Confirming -> "'${state.dest.displayName}'(으)로 안내를 시작할지 확인하는 중이다."
        is GuideState.Arrived -> "'${state.dest.displayName}'에 도착해서 사용자의 완료 확인을 기다리는 중이다."
        is GuideState.Navigating, is GuideState.WaypointGuide -> "이동 중이다. 급한 취소가 아니면 반응하지 않는다."
        is GuideState.Recovering -> "경로를 재시도하는 중이다."
        is GuideState.Returning -> "대기 장소로 복귀하는 중이다."
        is GuideState.Error -> "오류로 안내를 마친 상태다."
        is GuideState.WaitingUser -> "사용자를 기다리는 중이다."
    }

    /** 문자열 필드 — description 은 모델에게 주는 설명 */
    private fun text(description: String) = JSONObject().put("type", "string").put("description", description)

    /** 이 값들 중 하나만 */
    private fun oneOf(vararg values: String) = JSONObject().put("type", "string").put("enum", JSONArray(values.toList()))

    /** 필드가 모두 필수인 object */
    private fun allRequired(fields: Map<String, JSONObject>) = JSONObject()
        .put("type", "object")
        .put("properties", JSONObject(fields))
        .put("required", JSONArray(fields.keys.toList()))

    private fun schema(catalog: DestinationCatalog): JSONObject {
        val reply = text("로봇이 소리 내어 말할 한국어 한 문장")
        val destinationId = text("안내할 수 있는 목적지의 id").apply {
            if (catalog.destinations.isNotEmpty()) {
                put("enum", JSONArray(catalog.destinations.map { it.id }))
            }
        }
        return JSONObject().put(
            "anyOf",
            JSONArray()
                .put(allRequired(mapOf("action" to oneOf("selectDestination"), "destinationId" to destinationId, "reply" to reply)))
                .put(allRequired(mapOf("action" to oneOf("confirm"), "reply" to reply)))
                .put(allRequired(mapOf("action" to oneOf("cancel"), "reply" to reply)))
                .put(allRequired(mapOf("action" to oneOf("done"), "reply" to reply)))
                .put(allRequired(mapOf("action" to oneOf("answer", "unknown"), "reply" to reply))),
        )
    }
}
