package com.amirdaryabak.runningapp.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.amirdaryabak.runningapp.R
import com.amirdaryabak.runningapp.models.Feature
import com.amirdaryabak.runningapp.models.GeoJson
import com.amirdaryabak.runningapp.other.Constants
import com.amirdaryabak.runningapp.other.TrackingUtility
import com.amirdaryabak.runningapp.ui.MainActivity
import com.google.android.gms.location.*
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit


private const val TAG = "ForegroundService"
private const val NOTIFICATION_ID = 12345678
private const val NOTIFICATION_CHANNEL_ID = "Service"

@AndroidEntryPoint
class ForegroundOnlyLocationService : LifecycleService() {

    companion object {
        val isTracking = MutableLiveData<Boolean>()
        var geoJson = GeoJson()
        var lastIndexOfFeatures: Int = 0
        var isServicePaused = false
        var serviceKilled = false
        var latLngBounds: LatLngBounds.Builder = LatLngBounds.Builder()
        val timeRunInMillis = MutableLiveData<Long>()
        val locationsList = MutableLiveData<Location?>()

        /***
         * START 10
         * RUNNING 20
         * PAUSED 30
         * FINISHED 40
         */
        var currentState = 10
    }

    private val timeRunInSeconds = MutableLiveData<Long>()

    private var configurationChange = false

    private var serviceRunningInForeground = false

    private val localBinder = LocalBinder()

    private lateinit var notificationManager: NotificationManager

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    var isFirstRun = true

    override fun onCreate() {
        super.onCreate()
        postInitialValues()

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.create().apply {
            interval = TimeUnit.SECONDS.toMillis(5)
            fastestInterval = TimeUnit.SECONDS.toMillis(5)
            maxWaitTime = TimeUnit.SECONDS.toMillis(10)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val lastLocation = locationResult.lastLocation
                locationsList.postValue(lastLocation)
                Timber.d("amir location: %s", "${lastLocation.latitude}, ${lastLocation.longitude}")
            }
        }

        timeRunInSeconds.observe(this) {
            if (serviceRunningInForeground) {
                notificationManager.notify(
                    NOTIFICATION_ID,
                    generateNotification(it, isTimerEnabled)
                )
            }
        }
    }

    private fun postInitialValues() {
        isTracking.postValue(false)
        timeRunInMillis.postValue(0L)
        timeRunInSeconds.postValue(0L)
        geoJson = GeoJson()
        latLngBounds = LatLngBounds.Builder()
        locationsList.postValue(null)
        currentState = 10

        lapTime = 0L
        timeRun = 0L
        timeStarted = 0L
        lastSecondTimestamp = 0L
    }

    private fun killService() {
        isFirstRun = true
        serviceKilled = true
        pauseService()
        isServicePaused = false
        postInitialValues()
        stopForeground(true)
        stopSelf()
    }

    private fun pauseService() {
        isServicePaused = true
        unsubscribeToLocationUpdates()
        isTracking.postValue(false)
        isTimerEnabled = false
        if (serviceRunningInForeground) {
            notificationManager.notify(
                NOTIFICATION_ID,
                generateNotification(timeRunInSeconds.value!!, isTimerEnabled)
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        intent?.let {
            when (it.action) {
                Constants.ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstRun) {
                        currentState = 20
                        subscribeToLocationUpdates()
                        isFirstRun = false
                    } else {
                        currentState = 20
                        isServicePaused = false
                        subscribeToLocationUpdates()
                        Timber.d("Resuming service...")
                        geoJson.features.add(Feature())
                        lastIndexOfFeatures += 1
                        startTimer()
                    }
                }
                Constants.ACTION_PAUSE_SERVICE -> {
                    currentState = 30
                    Timber.d("Paused service")
                    pauseService()
                }
                Constants.ACTION_STOP_SERVICE -> {
                    currentState = 40
                    Timber.d("Stopped service")
                    killService()
                }
            }
        }

        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false
        return localBinder
    }

    override fun onRebind(intent: Intent) {
        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        // NOTE: If this method is called due to a configuration change in MainActivity,
        // we do nothing.
        if (!configurationChange) {
            val notification = generateNotification(timeRunInSeconds.value!!, isTimerEnabled)
            startForeground(NOTIFICATION_ID, notification)
            isTracking.postValue(true)
            serviceRunningInForeground = true
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy()")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configurationChange = true
    }

    private fun subscribeToLocationUpdates() {
        startService(Intent(applicationContext, ForegroundOnlyLocationService::class.java))
        try {
            startTimer()
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper()
            )
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }

    private fun unsubscribeToLocationUpdates() {
        try {
            val removeTask = fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            removeTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Location Callback removed.")
                } else {
                    Log.d(TAG, "Failed to remove Location Callback.")
                }
            }
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }

    private fun generateNotification(
        timeRunInSeconds: Long,
        isTimerEnabled: Boolean
    ): Notification {
        val titleText = "سرویس در حال انجام"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID, titleText, NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle(titleText)

        val activityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).also {
                it.action = Constants.ACTION_SHOW_TRACKING_FRAGMENT
            },
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)

        val notificationActionText = if (isTimerEnabled) "Pause" else "Resume"
        val pendingIntent = if (isTimerEnabled) {
            val pauseIntent = Intent(this, ForegroundOnlyLocationService::class.java).apply {
                action = Constants.ACTION_PAUSE_SERVICE
            }
            PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        } else {
            val resumeIntent = Intent(this, ForegroundOnlyLocationService::class.java).apply {
                action = Constants.ACTION_START_OR_RESUME_SERVICE
            }
            PendingIntent.getService(this, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        notificationCompatBuilder
//            .setStyle(bigTextStyle)
            .setContentTitle(titleText)
            .setContentText(TrackingUtility.getFormattedStopWatchTime(timeRunInSeconds * 1000L))
            .setSmallIcon(R.drawable.ic_launch)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(activityPendingIntent)
        /*if (isTimerEnabled) {
            notificationCompatBuilder.addAction(
                R.drawable.ic_launch,
                notificationActionText,
                pendingIntent
            )
        } else {
            notificationCompatBuilder.addAction(
                R.drawable.ic_launch,
                notificationActionText,
                pendingIntent
            )
        }*/
        return notificationCompatBuilder.build()
    }

    /*private fun updateNotificationTrackingState(isTracking: Boolean) {
        val notificationActionText = if(isTracking) "Pause" else "Resume"
        val pendingIntent = if(isTracking) {
            val pauseIntent = Intent(this, TrackingService::class.java).apply {
                action = Constants.ACTION_PAUSE_SERVICE
            }
            PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        } else {
            val resumeIntent = Intent(this, TrackingService::class.java).apply {
                action = Constants.ACTION_START_OR_RESUME_SERVICE
            }
            PendingIntent.getService(this, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        curNotificationBuilder.javaClass.getDeclaredField("mActions").apply {
            isAccessible = true
            set(curNotificationBuilder, ArrayList<NotificationCompat.Action>())
        }
        if (!serviceKilled) {
            curNotificationBuilder = baseNotificationBuilder
                .addAction(R.drawable.ic_pause_black_24dp, notificationActionText, pendingIntent)
            notificationManager.notify(Constants.NOTIFICATION_ID, curNotificationBuilder.build())
        }
    }*/

    private var isTimerEnabled = false
    private var lapTime = 0L
    private var timeRun = 0L
    private var timeStarted = 0L
    private var lastSecondTimestamp = 0L

    private fun startTimer() {
        isTracking.postValue(true)
        timeStarted = System.currentTimeMillis()
        isTimerEnabled = true
        CoroutineScope(Dispatchers.Main).launch {
            while (isTracking.value!!) {
                // time difference between now and timeStarted
                lapTime = System.currentTimeMillis() - timeStarted
                // post the new lapTime
                timeRunInMillis.postValue(timeRun + lapTime)
                if (timeRunInMillis.value!! >= lastSecondTimestamp + 1000L) {
                    timeRunInSeconds.postValue(timeRunInSeconds.value!! + 1)
                    lastSecondTimestamp += 1000L
                }
                delay(Constants.TIMER_UPDATE_INTERVAL)
            }
            timeRun += lapTime
        }
    }

    inner class LocalBinder : Binder() {
        internal val service: ForegroundOnlyLocationService
            get() = this@ForegroundOnlyLocationService
    }
}
