package me.spica27.spicamusic.utils

import org.ocpsoft.prettytime.PrettyTime
import java.util.*

object TimeUtils {
    val prettyTime by lazy {
        PrettyTime()
    }

    fun getDateDesc(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val monthDesc =
            when (month) {
                in 1..2 -> "冬天"
                in 3..5 -> "春天"
                in 6..8 -> "夏天"
                in 9..11 -> "秋天"
                else -> "冬天"
            }
        val dayDesc =
            when (hour) {
                in 0..5 -> "凌晨"
                in 6..11 -> "上午"
                in 11..13 -> "中午"
                in 14..17 -> "下午"
                in 18..24 -> "晚上"
                else -> "晚上"
            }
        return "${monthDesc}的$dayDesc"
    }

    fun getSeason(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val month = calendar.get(Calendar.MONTH) + 1
        return when (month) {
            in 1..2 -> "冬天"
            in 3..5 -> "春天"
            in 6..8 -> "夏天"
            in 9..11 -> "秋天"
            else -> "冬天"
        }
    }

    fun getDayDesc(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 0..5 -> "凌晨"
            in 6..11 -> "上午"
            in 11..13 -> "中午"
            in 14..17 -> "下午"
            in 18..23 -> "晚上"
            else -> "未知"
        }
    }
}
