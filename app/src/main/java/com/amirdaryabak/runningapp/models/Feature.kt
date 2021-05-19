package com.amirdaryabak.runningapp.models

import androidx.annotation.Keep

@Keep
data class Feature(
    var geometry: Geometry = Geometry(),
    val properties: Properties = Properties(),
    val type: String = "Feature",
)