package com.amirdaryabak.runningapp.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.amirdaryabak.runningapp.models.GeoJson
import com.amirdaryabak.runningapp.models.GeometryDB

@Database(
    entities = [
        Run::class,
        GeometryDB::class,
    ],
    version = 1
)
@TypeConverters(Converters::class)
abstract class RunningDatabase : RoomDatabase() {

    abstract fun getRunDao(): RunDAO
}