package kr.ac.kw.followme.data

/**
 * @Preview 와 FakeRobotController 테스트에서 쓰는 더미 데이터.
 * 실제 목적지는 assets/destinations.json 에서 읽는다.
 */
val sampleDestinations: List<Destination> = listOf(
    Destination(
        id = "bima_101",
        displayName = "비마관 101호",
        category = "강의실",
        building = "비마관",
        route = listOf(
            RouteStep("bima_junction_a", "여기서 왼쪽으로 갑니다"),
            RouteStep("bima_101", ""),
        ),
        arrivalMessage = "비마관 101호에 도착했습니다. 오른쪽 문입니다.",
    ),
    Destination(
        id = "bima_104",
        displayName = "비마관 104호",
        category = "강의실",
        building = "비마관",
        route = listOf(
            RouteStep("bima_junction_a", "여기서 직진합니다"),
            RouteStep("bima_junction_b", "여기서 오른쪽으로 갑니다"),
            RouteStep("bima_104", ""),
        ),
        arrivalMessage = "비마관 104호에 도착했습니다. 왼쪽 문입니다.",
    ),
    Destination(
        id = "bima_dept_office",
        displayName = "로봇학부 사무실",
        category = "학과 사무실",
        building = "비마관",
        route = listOf(
            RouteStep("bima_junction_a", "여기서 오른쪽으로 갑니다"),
            RouteStep("bima_dept_office", ""),
        ),
        arrivalMessage = "로봇학부 사무실에 도착했습니다. 정면에 보이는 문입니다.",
    ),
    Destination(
        id = "saebit_lab_ai",
        displayName = "지능로봇 연구실",
        category = "연구실",
        building = "새빛관",
        route = listOf(
            RouteStep("saebit_junction_a", "여기서 오른쪽으로 갑니다"),
            RouteStep("saebit_stair_front", "앞에 계단이 있습니다. 조심해서 따라와 주세요"),
            RouteStep("saebit_lab_ai", ""),
        ),
        arrivalMessage = "지능로봇 연구실에 도착했습니다. 복도 끝 왼쪽 방입니다.",
    ),
)

val sampleDestination: Destination = sampleDestinations[1]
