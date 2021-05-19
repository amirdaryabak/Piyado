package com.amirdaryabak.runningapp.models

import androidx.annotation.Keep

@Keep
data class Geometry(
    val coordinates: ArrayList<ArrayList<Double>> = arrayListOf(),
    val type: String = "LineString",
)