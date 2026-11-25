package kr.ac.kw.followme.data

/**
 * @Preview 와 FakeRobotController 테스트에서 쓰는 더미 데이터.
 * 실제 목적지는 assets/destinations.json 에서 읽는다.
 */
val sampleDestinations: List<Destination> = listOf(
    Destination(
        id = "saebit_entrance",
        displayName = "새빛관 출입문",
        category = "출입구",
        building = "새빛관",
        route = listOf(RouteStep("새빛관 출입문", "")),
        arrivalMessage = "새빛관 출입문에 도착했습니다.",
    ),
    Destination(
        id = "saebit_elevator_1f",
        displayName = "1층 엘리베이터",
        category = "편의시설",
        floor = 1,
        showFloorMap = true,
        building = "새빛관",
        route = listOf(RouteStep("1층 엘리베이터", "")),
        arrivalMessage = "1층 엘리베이터에 도착했습니다.",
    ),
)

val sampleDestination: Destination = sampleDestinations[1]
