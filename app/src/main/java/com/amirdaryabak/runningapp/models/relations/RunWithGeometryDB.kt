package com.amirdaryabak.runningapp.models.relations

import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Relation
import com.amirdaryabak.runningapp.db.Run
import com.amirdaryabak.runningapp.models.GeometryDB

@Keep
data class RunWithGeometryDB(
    @Embedded val run: Run,
    @Relation(
        parentColumn = "id",
        entityColumn = "runId"
    )
    val geometryDB: List<GeometryDB>
)