package com.amirdaryabak.runningapp.repositories

import com.amirdaryabak.runningapp.db.Run
import com.amirdaryabak.runningapp.db.RunDAO
import com.amirdaryabak.runningapp.models.GeoJson
import com.amirdaryabak.runningapp.models.GeometryDB
import javax.inject.Inject

class MainRepository @Inject constructor(
    private val runDao: RunDAO
) {

    suspend fun insertRun(run: Run) = runDao.insertRun(run)

    suspend fun insertGeometryDB(geometryDB: GeometryDB) = runDao.insertGeometryDB(geometryDB)

    suspend fun getRunWithGeometryDBWithRunId(id: Int) = runDao.getRunWithGeometryDBWithRunId(id)

    suspend fun deleteRun(run: Run) = runDao.deleteRun(run)

    fun getAllRunsSortedByDate() = runDao.getAllRunsSortedByDate()

    fun getAllRunsSortedByDistance() = runDao.getAllRunsSortedByDistance()

    fun getAllRunsSortedByTimeInMillis() = runDao.getAllRunsSortedByTimeInMillis()

    fun getAllRunsSortedByAvgSpeed() = runDao.getAllRunsSortedByAvgSpeed()

    fun getAllRunsSortedByCaloriesBurned() = runDao.getAllRunsSortedByCaloriesBurned()

    fun getTotalAvgSpeed() = runDao.getTotalAvgSpeed()

    fun getTotalDistance() = runDao.getTotalDistance()

    fun getTotalCaloriesBurned() = runDao.getTotalCaloriesBurned()

    fun getTotalTimeInMillis() = runDao.getTotalTimeInMillis()
}