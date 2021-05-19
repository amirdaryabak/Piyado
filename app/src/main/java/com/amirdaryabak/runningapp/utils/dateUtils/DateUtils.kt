package com.amirdaryabak.runningapp.utils.dateUtils

import android.text.format.DateFormat
import java.util.*

object DateUtils {

    fun getIranianDate(calendar: Calendar): String {
        val day = DateFormat.format("dd", calendar).toString().toInt()
        val month = DateFormat.format("MM", calendar).toString().toInt()
        val year = DateFormat.format("yyyy", calendar).toString().toInt()
        return PersianDate(year, month, day).iranianDate
    }
}