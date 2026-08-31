package com.muse.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Fmt {

    fun photoDate(ms: Long): String =
        SimpleDateFormat("yyyy.MM.dd", Locale.US).format(Date(ms))

    private fun ym(ms: Long): String =
        SimpleDateFormat("yyyy.MM", Locale.US).format(Date(ms))

    private fun md(ms: Long): String =
        SimpleDateFormat("MM.dd", Locale.US).format(Date(ms))

    fun homeRange(startMs: Long, endMs: Long): String {
        val sc = Calendar.getInstance().apply { timeInMillis = startMs }
        val ec = Calendar.getInstance().apply { timeInMillis = endMs }
        return if (sc.get(Calendar.YEAR) == ec.get(Calendar.YEAR)) {
            "${ym(startMs)} — ${md(endMs)}"
        } else {
            "${ym(startMs)} — ${ym(endMs)}"
        }
    }

    fun coverRange(startMs: Long, endMs: Long): String {
        val sc = Calendar.getInstance().apply { timeInMillis = startMs }
        val ec = Calendar.getInstance().apply { timeInMillis = endMs }
        return if (sc.get(Calendar.YEAR) == ec.get(Calendar.YEAR)) {
            "${photoDate(startMs)} — ${md(endMs)}"
        } else {
            "${photoDate(startMs)} — ${photoDate(endMs)}"
        }
    }

    fun artifacts(n: Int): String = "$n ARTIFACTS"

    fun archiveNo(n: Int): String = "ARCHIVE " + no(n)

    fun no(n: Int): String = n.toString().padStart(4, '0')

    fun monthName(ms: Long): String =
        SimpleDateFormat("MMMM", Locale.US).format(Date(ms)).uppercase(Locale.US)

    fun monthKey(ms: Long): String =
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date(ms))

    fun dayKey(ms: Long): Int {
        val c = Calendar.getInstance()
        c.timeInMillis = ms
        return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
    }
}
