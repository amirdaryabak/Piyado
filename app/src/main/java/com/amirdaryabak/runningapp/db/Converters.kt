package com.amirdaryabak.runningapp.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


class Converters {

    @TypeConverter
    fun fromString(value: String): ArrayList<ArrayList<Double>> {
        val listType: Type = object : TypeToken<ArrayList<ArrayList<Double>>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromArrayList(list: ArrayList<ArrayList<Double>>): String {
        return Gson().toJson(list)
    }

}