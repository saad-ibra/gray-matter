package com.example.graymatter.android.util

import java.util.Calendar

object DateGroupUtils {
    fun getGroupForTimestamp(timestamp: Long): String {
        if (timestamp <= 0) return "Older"
        
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }
        
        val nowYear = now.get(Calendar.YEAR)
        val nowDay = now.get(Calendar.DAY_OF_YEAR)
        val targetYear = target.get(Calendar.YEAR)
        val targetDay = target.get(Calendar.DAY_OF_YEAR)
        
        val diffDays = when {
            nowYear == targetYear -> nowDay - targetDay
            nowYear - targetYear == 1 -> (nowDay + now.getActualMaximum(Calendar.DAY_OF_YEAR)) - targetDay
            else -> 999
        }
        
        return when {
            diffDays <= 0 -> "Today"
            diffDays == 1 -> "Yesterday"
            diffDays <= 7 -> "Previous 7 Days"
            diffDays <= 30 -> "Previous 30 Days"
            else -> "Older"
        }
    }
}
