package kr.ac.kw.followme.data

/**
 * 안내 경로의 한 구간(segment).
 *
 * @param location temi 에 저장된 위치 이름. 반드시 로봇의 saved location 과 철자가 같아야 한다.
 * @param say      이 위치에 도착했을 때 읽어 줄 방향 안내 문구. 빈 문자열이면 TTS 없이 다음 구간으로 넘어간다.
 */
data class RouteStep(
    val location: String,
    val say: String,
)

/**
 * 목적지 하나. assets/destinations.json 에서 읽어 온다.
 * 맵이 바뀌면 JSON 만 수정하고 코드는 건드리지 않는다.
 */
data class Destination(
    val id: String,
    val displayName: String,
    val category: String,
    val building: String,
    val route: List<RouteStep>,
    val arrivalMessage: String,
) {
    /** 마지막 구간이 실제 목적지다. */
    val finalLocation: String get() = route.last().location

    /** 경로에 등장하는 모든 temi 위치 이름 */
    val locations: List<String> get() = route.map { it.location }
}

/** destinations.json 전체 */
data class DestinationCatalog(
    val version: Int,
    val homeBase: String,
    val destinations: List<Destination>,
) {
    /** JSON 에 등장하는 순서를 유지한 카테고리 목록. 선택 화면의 탭을 여기서 만든다. */
    val categories: List<String> get() = destinations.map { it.category }.distinct()

    fun byId(id: String): Destination? = destinations.firstOrNull { it.id == id }

    /** 앱 시작 시 temi 의 getLocations() 결과와 대조해 누락된 위치를 찾는다. */
    fun missingLocations(savedLocations: Collection<String>): List<String> {
        val saved = savedLocations.map { it.trim().lowercase() }.toSet()
        return destinations
            .flatMap { it.locations }
            .plus(homeBase)
            .distinct()
            .filter { it.trim().lowercase() !in saved }
    }

    companion object {
        val EMPTY = DestinationCatalog(version = 0, homeBase = "home base", destinations = emptyList())
    }
}
