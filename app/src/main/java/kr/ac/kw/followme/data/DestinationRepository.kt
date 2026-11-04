package kr.ac.kw.followme.data

import android.content.Context
import android.util.Log
import org.json.JSONObject

/**
 * assets/destinations.json 로더.
 *
 * 외부 직렬화 라이브러리 없이 org.json 만 사용한다(의존성 최소화).
 * 필드가 빠져 있으면 예외를 던지지 않고 기본값으로 채운 뒤 로그만 남긴다.
 */
class DestinationRepository(private val context: Context) {

    fun load(fileName: String = ASSET_NAME): DestinationCatalog = try {
        parse(context.assets.open(fileName).bufferedReader().use { it.readText() })
    } catch (e: Exception) {
        Log.e(TAG, "목적지 데이터를 읽지 못했습니다: $fileName", e)
        DestinationCatalog.EMPTY
    }

    private fun parse(json: String): DestinationCatalog {
        val root = JSONObject(json)
        val array = root.optJSONArray("destinations")
        val list = buildList {
            for (i in 0 until (array?.length() ?: 0)) {
                val obj = array!!.getJSONObject(i)
                val steps = obj.optJSONArray("route")
                val route = buildList {
                    for (j in 0 until (steps?.length() ?: 0)) {
                        val step = steps!!.getJSONObject(j)
                        val location = step.optString("location")
                        if (location.isBlank()) {
                            Log.w(TAG, "route[$j] 에 location 이 없습니다: ${obj.optString("id")}")
                            continue
                        }
                        add(RouteStep(location = location, say = step.optString("say")))
                    }
                }
                if (route.isEmpty()) {
                    Log.w(TAG, "route 가 비어 있어 건너뜁니다: ${obj.optString("id")}")
                    continue
                }
                add(
                    Destination(
                        id = obj.optString("id"),
                        displayName = obj.optString("displayName"),
                        category = obj.optString("category", "기타"),
                        building = obj.optString("building", ""),
                        route = route,
                        arrivalMessage = obj.optString("arrivalMessage"),
                    )
                )
            }
        }
        return DestinationCatalog(
            version = root.optInt("version", 1),
            homeBase = root.optString("homeBase", "home base"),
            destinations = list,
        )
    }

    private companion object {
        const val TAG = "DestinationRepository"
        const val ASSET_NAME = "destinations.json"
    }
}
