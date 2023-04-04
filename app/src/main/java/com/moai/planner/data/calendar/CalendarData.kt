package com.moai.planner.data.calendar

import java.text.SimpleDateFormat
import java.util.*

data class CalendarData(var data: Date, var isSelected: Boolean = false) {

    val calendarDay: String
        get() = SimpleDateFormat("EE", Locale.getDefault()).format(data)

    val calendarDate: String
        get() {
            val cal = Calendar.getInstance()
            cal.time = data
            return cal[Calendar.DAY_OF_MONTH].toString()
        }
    val calendarMonth: String
        get() {
            val cal = Calendar.getInstance()
            cal.time = data
            return (cal[Calendar.MONTH] + 1).toString()
        }
    val calendarYear: String
        get() {
            val cal = Calendar.getInstance()
            cal.time = data
            return cal[Calendar.YEAR].toString()
        }
}
