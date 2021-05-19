package com.amirdaryabak.runningapp.models

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity
data class GeometryDB(
    val coordinates: ArrayList<ArrayList<Double>>,
    val runId: Int,
    val type: String= "LineString",
    @PrimaryKey
    val id: Int? = null
)