package kr.ac.kw.followme.data

import android.content.Context
import android.util.Log
import kr.ac.kw.followme.guide.GuideLogEntry
import kr.ac.kw.followme.guide.GuideLogger
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 상태 전이 로그를 CSV 파일로 저장한다.
 *
 * 저장 위치: `context.getExternalFilesDir(null)/guide_logs/guide_log_<세션 시작 시각>.csv`
 * (앱 전용 외부 저장소라 별도 저장소 권한이 필요 없다 — API 23 에서도 그대로 동작)
 *
 * 세션(앱 실행)마다 파일을 하나 새로 만든다. 평가 실험에서 "시도 1회 = 로그 파일 1개"로
 * 맞춰 두면 소요 시간/성공률 계산용 스크립트를 짜기 편하다.
 */
class CsvGuideLogger(context: Context) : GuideLogger {

    private val file: File = run {
        val dir = File(context.getExternalFilesDir(null), "guide_logs").apply { mkdirs() }
        val name = "guide_log_${FILE_STAMP.format(Date())}.csv"
        File(dir, name).apply {
            if (!exists()) writeText("timestamp,prevState,event,nextState,destination,segment\n")
        }
    }

    override fun log(entry: GuideLogEntry) {
        val line = listOf(
            entry.timestamp.toString(),
            entry.prevState,
            entry.event,
            entry.nextState,
            entry.destination.orEmpty(),
            entry.segment?.toString().orEmpty(),
        ).joinToString(",") { csvEscape(it) }
        try {
            file.appendText("$line\n")
        } catch (e: Exception) {
            Log.e(TAG, "로그 기록 실패", e)
        }
    }

    private fun csvEscape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    private companion object {
        const val TAG = "CsvGuideLogger"
        val FILE_STAMP = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    }
}
