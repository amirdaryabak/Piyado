package com.amirdaryabak.runningapp.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.amirdaryabak.runningapp.db.RunningDatabase
import com.amirdaryabak.runningapp.other.Constants
import com.amirdaryabak.runningapp.other.Constants.RUNNING_DATABASE_NAME
import com.amirdaryabak.runningapp.storage.PrefsUtils
import com.amirdaryabak.runningapp.storage.PrefsUtilsImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.greenrobot.eventbus.EventBus
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideRunningDatabase(
        @ApplicationContext app: Context
    ) = Room.databaseBuilder(
        app,
        RunningDatabase::class.java,
        RUNNING_DATABASE_NAME
    ).build()

    @Singleton
    @Provides
    fun provideRunDao(db: RunningDatabase) = db.getRunDao()

    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext app: Context): SharedPreferences =
        app.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Singleton
    @Provides
    fun provideName(sharedPref: SharedPreferences) =
        sharedPref.getString(Constants.KEY_NAME, "") ?: ""

    @Singleton
    @Provides
    fun provideWeight(sharedPref: SharedPreferences) =
        sharedPref.getFloat(Constants.KEY_WEIGHT, 80f)

    @Singleton
    @Provides
    fun provideFirstTimeToggle(sharedPref: SharedPreferences) =
        sharedPref.getBoolean(Constants.KEY_FIRST_TIME_TOGGLE, true)

    @Singleton
    @Provides
    fun provideEventBus(): EventBus = EventBus.getDefault()

    @Singleton
    @Provides
    fun providePrefsUtils(
        @ApplicationContext context: Context
    ) = PrefsUtilsImpl(
        context.getSharedPreferences(
            Constants.SHARED_PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
    ) as PrefsUtils

}








