package com.amirdaryabak.runningapp.ui.viewmodels

import android.content.res.AssetManager
import androidx.lifecycle.*
import com.amirdaryabak.runningapp.repositories.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import java.io.InputStream
import java.util.*
import javax.inject.Inject

@HiltViewModel
class RunningViewModel @Inject constructor(
    private val mainRepository: MainRepository
): ViewModel() {


    fun getGeoJson(assets: AssetManager?): String {
        val inputStream = assets?.open("example.geojson")
        val convertedStream = convertStreamToString(inputStream)
        Timber.d("GeoJson: %s", convertedStream)
        return convertedStream
    }

    private fun convertStreamToString(`is`: InputStream?): String {
        val scanner: Scanner = Scanner(`is`).useDelimiter("\\A")
        return if (scanner.hasNext()) scanner.next() else ""
    }

    /*private val _states: MutableLiveData<Event<ResourceTest<>>> =
        MutableLiveData()
    val states: LiveData<Event<ResourceTest<DynamicResponse<List<FilterContentEntity>>>>> = _states
    private var statesJob: Job? = null*/

}