package com.amirdaryabak.runningapp.models

import androidx.annotation.Keep

@Keep
data class GeoJson(
    val features: ArrayList<Feature> = arrayListOf(Feature()),
    val type: String = "FeatureCollection",
)