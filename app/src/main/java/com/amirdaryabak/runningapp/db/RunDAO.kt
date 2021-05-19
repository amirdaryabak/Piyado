package com.amirdaryabak.runningapp.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.amirdaryabak.runningapp.models.GeoJson
import com.amirdaryabak.runningapp.models.GeometryDB
import com.amirdaryabak.runningapp.models.relations.RunWithGeometryDB

@Dao
interface RunDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: Run): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeometryDB(geometryDB: GeometryDB)

    @Transaction
    @Query("SELECT * FROM running_table WHERE id = :id")
    suspend fun getRunWithGeometryDBWithRunId(id: Int): RunWithGeometryDB

    @Delete
    suspend fun deleteRun(run: Run)

    @Query("SELECT * FROM running_table ORDER BY timestamp DESC")
    fun getRunAndGeometryByTimestamp(): LiveData<List<RunWithGeometryDB>>

    @Query("SELECT * FROM running_table ORDER BY timestamp DESC")
    fun getAllRunsSortedByDate(): LiveData<List<Run>>

    @Query("SELECT * FROM running_table ORDER BY timeInMillis DESC")
    fun getAllRunsSortedByTimeInMillis(): LiveData<List<Run>>

    @Query("SELECT * FROM running_table ORDER BY caloriesBurned DESC")
    fun getAllRunsSortedByCaloriesBurned(): LiveData<List<Run>>

    @Query("SELECT * FROM running_table ORDER BY avgSpeedInKMH DESC")
    fun getAllRunsSortedByAvgSpeed(): LiveData<List<Run>>

    @Query("SELECT * FROM running_table ORDER BY distanceInMeters DESC")
    fun getAllRunsSortedByDistance(): LiveData<List<Run>>

    @Query("SELECT SUM(timeInMillis) FROM running_table")
    fun getTotalTimeInMillis(): LiveData<Long>

    @Query("SELECT SUM(caloriesBurned) FROM running_table")
    fun getTotalCaloriesBurned(): LiveData<Int>

    @Query("SELECT SUM(distanceInMeters) FROM running_table")
    fun getTotalDistance(): LiveData<Int>

    @Query("SELECT AVG(timeInMillis) FROM running_table")
    fun getTotalAvgSpeed(): LiveData<Float>

}
