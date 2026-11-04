package kr.ac.kw.followme.gemini

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Gemini API 를 HTTP 로 직접 호출한다 (Interactions API).
 * LibraryRobot 레퍼런스 프로젝트에서 그대로 가져왔다 — 그쪽에서 이미 동작을 확인한 방식이다.
 *
 *  요청:  POST https://generativelanguage.googleapis.com/v1beta/interactions
 *         헤더  x-goog-api-key: <키>
 *         본문  { model, system_instruction, input, response_format(JSON 스키마) }
 *  응답:  steps[] 중 type = "model_output" 의 content[].text  ← 모델이 쓴 글 (스키마를 주면 JSON 문자열)
 */
class GeminiClient(
    private val apiKey: String,
    private val model: String = "gemma-4-26b-a4b-it",             // 기본 — Gemini API 로 쓰는 Gemma. 빠르다 (3~5초)
    private val fallbackModel: String = "gemini-3-flash-preview", // 기본 모델이 두 번 다 안 되면 이 모델로 (생각 최소, 4~6초)
) {

    private val http = OkHttpClient()
    private val first = http.newBuilder().readTimeout(30, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build()
    private val last = http.newBuilder().readTimeout(30, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build()

    /**
     * 사용자 문장을 보내고 모델의 답을 문자열로 받는다. (네트워크 — 백그라운드 스레드에서 부를 것)
     * @param schema  null 이면 자유 텍스트로 답한다. 주면 그 JSON 스키마 모양으로만 답한다 (Structured Output).
     */
    fun ask(userText: String, systemPrompt: String, schema: JSONObject? = null): String {
        repeat(2) {                                                  // 몰림(503)은 보통 1초 안에 와서, 같은 모델로 다시 보내면 대개 된다
            try {
                return send(first, request(model, userText, systemPrompt, schema))
            } catch (e: RetryableException) { }
        }
        return send(last, request(fallbackModel, userText, systemPrompt, schema))   // 그래도 안 되면 다른 모델로
    }

    private fun request(model: String, userText: String, systemPrompt: String, schema: JSONObject?): Request {
        val body = JSONObject().apply {
            put("model", model)
            put("system_instruction", systemPrompt)      // 로봇의 역할
            put("input", userText)                       // 사람이 한 말
            if (model.startsWith("gemini")) {             // Gemini 는 답하기 전에 생각부터 한다 — 짧은 판단이라 생각을 최소로 (빨라진다)
                put("generation_config", JSONObject().put("thinking_level", "minimal"))
            }
            if (schema != null) {
                put("response_format", JSONObject()
                    .put("type", "text")
                    .put("mime_type", "application/json") // JSON 으로만 답하라
                    .put("schema", schema))               // 이 모양으로만
            }
        }
        return Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/interactions")
            .header("x-goog-api-key", apiKey)
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
    }

    /** 다시 보내면 될 수도 있는 실패 — 요청 한도 초과(429), 서버 오류·과부하(500·503·504), 연결 실패·시간 초과 */
    private class RetryableException(message: String) : Exception(message)

    private fun send(client: OkHttpClient, request: Request): String {
        val response = try {
            client.newCall(request).execute()
        } catch (e: java.io.IOException) {
            throw RetryableException("네트워크 오류: ${e.message}")
        }
        response.use {
            val text = it.body?.string().orEmpty()
            if (it.code in listOf(429, 500, 503, 504)) throw RetryableException("HTTP ${it.code}")
            if (!it.isSuccessful) error("HTTP ${it.code}: ${text.take(200)}")   // 400 · 401 등은 다시 보내도 같다
            return outputText(JSONObject(text))
        }
    }

    /** 응답의 steps 중 모델 출력(model_output) 단계에서 글(text)만 꺼낸다 — 생각(thought) 단계는 건너뛴다 */
    private fun outputText(interaction: JSONObject): String {
        val steps = interaction.getJSONArray("steps")
        for (i in 0 until steps.length()) {
            val step = steps.getJSONObject(i)
            if (step.optString("type") != "model_output") continue
            val content = step.getJSONArray("content")
            for (j in 0 until content.length()) {
                val part = content.getJSONObject(j)
                if (part.optString("type") == "text") return part.getString("text")
            }
        }
        error("응답에 model_output 이 없습니다")
    }
}
