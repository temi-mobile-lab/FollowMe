package kr.ac.kw.followme.guide

/**
 * CSV 한 줄로 남길 로그 항목. 평가용 소요 시간/성공률/재시도 횟수 계산에 쓰인다.
 *
 * - destination/segment 는 전이 후(next) 값이 없으면(Idle/Returning 등 목적지 정보가 없는
 *   상태) 전이 전(prev) 값으로 채워 넣는다 — "어떤 목적지를 안내하다가 끝났는지"가
 *   로그에서 끊기지 않게 하기 위해서다. [GuideStateMachine] 참고.
 */
data class GuideLogEntry(
    val timestamp: Long,
    val prevState: String,
    val event: String,
    val nextState: String,
    val destination: String?,
    val segment: Int?,
)

/**
 * GuideStateMachine 은 이 인터페이스만 알아서, 저장 방식(CSV 파일/테스트용 메모리/미사용)을 모른다.
 */
fun interface GuideLogger {
    fun log(entry: GuideLogEntry)

    companion object {
        /** 로깅이 필요 없을 때(Preview, 간단 테스트 등) 쓰는 아무 동작도 안 하는 로거. */
        val NOOP = GuideLogger { }
    }
}
