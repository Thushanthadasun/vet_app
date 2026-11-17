package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateHelpers {

    fun todayYearMonthDay(): Triple<Int, Int, Int> {
        val cal = Calendar.getInstance()
        return Triple(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH), // 0-indexed (Calendar.MARCH == 2)
            cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    fun isMarch6Today(): Boolean {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.MONTH) == Calendar.MARCH &&
            cal.get(Calendar.DAY_OF_MONTH) == 6
    }

    fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

    fun toEpochDay(year: Int, monthZeroBased: Int, dayOfMonth: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return (cal.timeInMillis + cal.timeZone.getOffset(cal.timeInMillis)) / 86_400_000L
    }

    fun todayEpochDay(): Long {
        val (y, m, d) = todayYearMonthDay()
        return toEpochDay(y, m, d)
    }

    fun epochDayToCalendar(epochDay: Long): Calendar {
        val cal = Calendar.getInstance()
        val approxUtc = epochDay * 86_400_000L + 43_200_000L
        cal.timeInMillis = approxUtc - cal.timeZone.getOffset(approxUtc)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    fun formatEpochDay(epochDay: Long): String {
        val cal = epochDayToCalendar(epochDay)
        return SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).format(cal.time)
    }

    fun formatMillisDateTime(millis: Long): String =
        SimpleDateFormat("MMM d, h:mm a", Locale.ENGLISH).format(Date(millis))

    fun nextMarch6MorningMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val target = Calendar.getInstance().apply {
            set(Calendar.YEAR, now.get(Calendar.YEAR))
            set(Calendar.MONTH, Calendar.MARCH)
            set(Calendar.DAY_OF_MONTH, 6)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= nowMillis) {
            target.add(Calendar.YEAR, 1)
        }
        return target.timeInMillis
    }

    fun combineDateAndTime(epochDay: Long, hour24: Int, minute: Int): Long {
        val cal = epochDayToCalendar(epochDay)
        cal.set(Calendar.HOUR_OF_DAY, hour24)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun monthTitle(year: Int, monthZeroBased: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(cal.time)
    }

    fun daysInMonth(year: Int, monthZeroBased: Int): Pair<Int, Int> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDayOffset = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY).coerceIn(0, 6)
        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return firstDayOffset to totalDays
    }
}
