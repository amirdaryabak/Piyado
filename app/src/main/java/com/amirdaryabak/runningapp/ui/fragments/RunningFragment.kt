package com.amirdaryabak.runningapp.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.fragment.findNavController
import com.amirdaryabak.runningapp.BuildConfig
import com.amirdaryabak.runningapp.R
import com.amirdaryabak.runningapp.databinding.FragmentRunningBinding
import com.amirdaryabak.runningapp.db.Run
import com.amirdaryabak.runningapp.db.RunDAO
import com.amirdaryabak.runningapp.eventbus.BottomNavigationShowEvent
import com.amirdaryabak.runningapp.models.GeometryDB
import com.amirdaryabak.runningapp.other.Constants
import com.amirdaryabak.runningapp.other.TrackingUtility
import com.amirdaryabak.runningapp.services.ForegroundOnlyLocationService
import com.amirdaryabak.runningapp.services.ForegroundOnlyLocationService.Companion.currentState
import com.amirdaryabak.runningapp.services.ForegroundOnlyLocationService.Companion.geoJson
import com.amirdaryabak.runningapp.services.ForegroundOnlyLocationService.Companion.lastIndexOfFeatures
import com.amirdaryabak.runningapp.services.ForegroundOnlyLocationService.Companion.latLngBounds
import com.amirdaryabak.runningapp.storage.PrefsUtils
import com.amirdaryabak.runningapp.ui.BaseFragment
import com.amirdaryabak.runningapp.ui.MainActivity
import com.amirdaryabak.runningapp.ui.viewmodels.MainViewModel
import com.amirdaryabak.runningapp.utils.Utils
import com.amirdaryabak.runningapp.utils.Utils.showDialog
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.*
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.localization.LocalizationPlugin
import com.mapbox.mapboxsdk.plugins.localization.MapLocale
import com.mapbox.mapboxsdk.snapshotter.MapSnapshotter
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_running.*
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.math.round


private const val CANCEL_TRACKING_DIALOG_TAG = "CancelDialog"
private const val DEFAULT_INTERVAL_IN_MILLISECONDS = 2000L
private const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

@AndroidEntryPoint
class RunningFragment : BaseFragment(R.layout.fragment_running), OnMapReadyCallback,
    PermissionsListener, OnLocationClickListener, OnCameraTrackingChangedListener {

    private var _binding: FragmentRunningBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<MainViewModel>()
    private var permissionsManager: PermissionsManager = PermissionsManager(this)

    private val lineSource: String = "line-source"
    private val lineLayer: String = "linelayer"
    private var c = 0

    private var isRunFinished: Boolean = false
    private var isRunTooSmall: Boolean = false

    private var isTracking = false
    private var curTimeInMillis = 0L

    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapBoxView: MapView
    private lateinit var locationComponent: LocationComponent
    private lateinit var loadedMapStyle: Style

    @CameraMode.Mode
    private var cameraMode = CameraMode.TRACKING

    @RenderMode.Mode
    private var renderMode = RenderMode.NORMAL

    private lateinit var callback: OnBackPressedCallback

    private lateinit var userFusedLocationProviderClient: FusedLocationProviderClient

    @Inject
    lateinit var runDAO: RunDAO

    @Inject
    lateinit var eventBus: EventBus

    @Inject
    lateinit var prefsUtils: PrefsUtils

    @set:Inject
    var weight = 80f


    private val TAG = this::class.java.name


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRunningBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        eventBus.post(BottomNavigationShowEvent())

        onCustomBackPressed()
        setUpMapBox(savedInstanceState)
        handleOnConfigureChangedDialog(savedInstanceState)

        when (currentState) {
            10 -> {
                handleStart()
            }
            20 -> {
                handleRunning()
            }
            30 -> {
                handlePaused()
            }
            40 -> {
                finishRun()
            }
        }


        binding.apply {
            fabCurrentLocation.setOnClickListener {
                if (PermissionsManager.areLocationPermissionsGranted(requireContext())) {
                    if (isLocationEnabled()) {
                        binding.locationLoading.isVisible = true
                        binding.ivLocation.isVisible = false
                        goToUserLocation()
                    } else {
                        showDialog(
                            requireContext(),
                            "موقعیت مکانی",
                            "موقعیت مکانی شما خاموش است",
                            "روشن کردن موقعیت مکانی",
                            "انصراف",
                            { openLocationSetting() },
                            {}
                        )
                    }
                } else {
                    permissionsManager = PermissionsManager(this@RunningFragment)
                    permissionsManager.requestLocationPermissions(requireActivity())
                }
            }
        }

        subscribeToObservers()

//        sendNotification(1)

    }

    private fun handleLoadingView(
        text: String = "در حال ذخیره اطلاعات",
        isLoading: Boolean = true
    ) {
        binding.errorUi.apply {
            if (isLoading) {
                errorView.setOnClickListener { /* NO-OP */ }
                errorView.visibility = View.VISIBLE
                loadingOrigin.visibility = View.VISIBLE
                txtErrorText.text = text
                btnRetry.visibility = View.GONE
                btnEditProfile.visibility = View.GONE
                btnCancel.visibility = View.GONE
                btnContactUs.visibility = View.GONE
            } else {
                errorView.visibility = View.GONE
            }
        }
    }

    private fun finishRun() {
        if (this::mapboxMap.isInitialized) {
            mapboxMap.getStyle {
                if (geoJson.features.isNullOrEmpty() || geoJson.features.last().geometry.coordinates.isNullOrEmpty()) {
                    showRunTooSmallDialog()
                } else {
                    if (geoJson.features.last().geometry.coordinates[0].size == 0) {
                        geoJson.features.removeAt(lastIndexOfFeatures - 1)
                    }
                    drawLines(FeatureCollection.fromJson(Gson().toJson(geoJson)))
                }
            }
            mapboxMap.resetNorth()
        }
        fitCameraToPositionsAndEndRun()
    }

    private fun handlePaused() {
        currentState = 30
        handlePrimaryButton(
            getString(R.string.start_again),
        ) {
            sendCommandToService(Constants.ACTION_START_OR_RESUME_SERVICE)
            handleRunning()
        }
        handleSecondaryButton(getString(R.string.finish_run)) {
            finishRun()
        }
    }

    private fun handleRunning() {
        currentState = 20
        handlePrimaryButton(
            getString(R.string.pause),
        ) {
            sendCommandToService(Constants.ACTION_PAUSE_SERVICE)
            handlePaused()
        }
        handleSecondaryButton(getString(R.string.cancel_run)) {
            sendCommandToService(Constants.ACTION_PAUSE_SERVICE)
            handlePaused()
            showCancelTrackingDialog()
        }
    }

    private fun handleStart() {
        currentState = 10
        handlePrimaryButton(
            getString(R.string.start),
        ) {
            if (isLocationEnabled()) {
                if (foregroundPermissionApproved()) {
                    sendCommandToService(Constants.ACTION_START_OR_RESUME_SERVICE)
                } else {
                    requestForegroundPermissions()
                }
            } else {
                showDialog(
                    requireContext(),
                    "موقعیت مکانی",
                    "موقعیت مکانی شما خاموش است",
                    "روشن کردن موقعیت مکانی",
                    "انصراف",
                    { openLocationSetting() },
                    {}
                )
            }
            handleRunning()
        }
        handleSecondaryButton("", false) {}
    }

    private fun handleSecondaryButton(
        text: String,
        isVisible: Boolean = true,
        callFunction: () -> Unit,
    ) {
        binding.apply {
            btnFinishRun.text = text
            btnFinishRun.isVisible = isVisible
            btnFinishRun.setOnClickListener {
                callFunction.invoke()
            }
        }
    }

    private fun handlePrimaryButton(
        text: String,
        isVisible: Boolean = true,
        callFunction: () -> Unit,
    ) {
        binding.apply {
            btnToggleRun.text = text
            btnToggleRun.isVisible = isVisible
            btnToggleRun.setOnClickListener {
                callFunction.invoke()
            }
        }
    }


    private fun foregroundPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun requestForegroundPermissions() {
        val provideRationale = foregroundPermissionApproved()
        if (provideRationale) {
            showSnackbar(
                getString(R.string.permission_rationale),
                "OK",
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
                )
            }
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE) {
            when (PackageManager.PERMISSION_GRANTED) {
                grantResults[0] -> {
                    sendCommandToService(Constants.ACTION_START_OR_RESUME_SERVICE)
                }
                else -> {
                    // TODO handle deny all the time
                    /*showSnackbar(
                        getString(R.string.permission_denied_explanation),
                        getString(R.string.settings),
                    ) {
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts(
                            "package",
                            BuildConfig.APPLICATION_ID,
                            null
                        )
                        intent.data = uri
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    }*/
                }
            }
        }
    }

    //region <<<<<<<<<<<<<<<<<<<<- Commons ->>>>>>>>>>>>>>>>>>>>

    private fun openLocationSetting() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun subscribeToObservers() {
        ForegroundOnlyLocationService.locationsList.observe(viewLifecycleOwner) {
            it?.let {
                addPathPoint(it, lastIndexOfFeatures)
                moveCameraToLocation(it.latitude, it.longitude, 17.0)
            }
        }

        ForegroundOnlyLocationService.isTracking.observe(viewLifecycleOwner) {
            this.isTracking = it
        }

        ForegroundOnlyLocationService.timeRunInMillis.observe(viewLifecycleOwner) {
            curTimeInMillis = it
            val formattedTime = TrackingUtility.getFormattedStopWatchTime(curTimeInMillis, true)
            binding.tvTimer.text = formattedTime
        }
    }

    private fun endRunAndSaveToDb() {
        handleLoadingView()
        var distanceInMeters = 0
        geoJson.features.forEach { feature ->
            distanceInMeters += TrackingUtility.calculateCoordinatesLength(
                feature.geometry.coordinates
            ).toInt()
        }
        val avgSpeed =
            round((distanceInMeters / 1000f) / (curTimeInMillis / 1000f / 60 / 60) * 10) / 10f
        val dateTimestamp = Calendar.getInstance().timeInMillis
        val caloriesBurned = ((distanceInMeters / 1000f) * weight).toInt()
        val run = Run(dateTimestamp, avgSpeed, distanceInMeters, curTimeInMillis, caloriesBurned)
        viewModel.insertRun(run)
        viewModel.runId.observe(viewLifecycleOwner) { id ->
            val runId = id.toInt()
            geoJson.features.forEach {
                viewModel.insertGeometryDB(
                    GeometryDB(
                        it.geometry.coordinates,
                        runId
                    )
                )
                sendNotification(runId)
                stopRun()
                handleLoadingView(isLoading = false)
            }
        }
    }

    private fun sendCommandToService(action: String) =
        Intent(requireContext(), ForegroundOnlyLocationService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }

    private fun stopRun() {
        binding.tvTimer.text = "00:00:00"
//        ForegroundOnlyLocationService.postInitialValues()
        sendCommandToService(Constants.ACTION_STOP_SERVICE)
        try {
            findNavController().popBackStack()
        } catch (t: Throwable) {
        }
    }

    private fun sendNotification(runId: Int) {
        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            action = Constants.ACTION_SHOW_RUN_ITEM_FRAGMENT
            putExtra("runId", runId)
        }
        /*val pendingIntent2 = NavDeepLinkBuilder(requireContext())
            .setGraph(R.navigation.nav_home_graph)
            .setDestination(R.id.runItemFragment)
            .setArguments(
                Bundle().apply {
                putInt("runId", runId)
                }
            )
            .createPendingIntent()*/
        val pendingIntent = TaskStackBuilder.create(requireContext()).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        createNotificationChannel()
        val notification = NotificationCompat.Builder(requireContext(), "CHANNEL_RUNS")
            .setContentTitle("اطلاعات با موفقیت ذخیره شد")
            .setContentText("برای دیدن جزئیات کلیک کنید")
            .setSmallIcon(R.drawable.ic_run)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        val notificationManager = NotificationManagerCompat.from(requireContext())
        notificationManager.notify(Constants.NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "CHANNEL_RUNS",
                "Runs",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lightColor = Color.BLUE
                enableLights(true)
            }
            val manager =
                requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)

        }
    }

    private fun onCustomBackPressed() {
        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isRunFinished) {
                    showDialog(
                        requireContext(),
                        "هشدار",
                        "با خروج اطلاعات ذخیره نمیشود، آیا از خروج مطمئن هستید؟",
                        "بله",
                        "خیر",
                        {
                            try {
                                findNavController().popBackStack()
                            } catch (t: Throwable) {
                            }
                        },
                        { },
                    )
                } else {
                    try {
                        findNavController().popBackStack()
                    } catch (t: Throwable) {
                    }
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(callback)
    }

    private fun handleOnConfigureChangedDialog(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            val cancelTrackingDialog = parentFragmentManager.findFragmentByTag(
                CANCEL_TRACKING_DIALOG_TAG
            ) as CancelTrackingDialog?
            cancelTrackingDialog?.setYesListener {
                stopRun()
            }
        }
    }

    private fun loadBitmapFromView(view: View): Bitmap {
        val b = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        view.draw(c)
        return b
    }

    private fun showCancelTrackingDialog() {
        CancelTrackingDialog().apply {
            setYesListener {
                stopRun()
            }
        }.show(parentFragmentManager, CANCEL_TRACKING_DIALOG_TAG)
    }

    //endregion

    //region <<<<<<<<<<<<<<<<<<<<- UserLocation ->>>>>>>>>>>>>>>>>>>>

    private fun addPathPoint(location: Location?, lastIndexOfFeatures: Int) {
        location?.let {
            geoJson.features[lastIndexOfFeatures].geometry.coordinates.apply {
                latLngBounds.include(
                    LatLng(location.latitude, location.longitude)
                )
                add(
                    arrayListOf(
                        location.longitude,
                        location.latitude,
                    )
                )
            }
            drawLines(FeatureCollection.fromJson(Gson().toJson(geoJson)))
        }
    }

    @SuppressLint("MissingPermission")
    private fun goToUserLocation() {
        val request = LocationRequest().apply {
            interval = DEFAULT_INTERVAL_IN_MILLISECONDS
            fastestInterval = DEFAULT_MAX_WAIT_TIME
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        userFusedLocationProviderClient.requestLocationUpdates(
            request,
            goToUserLocationCallback,
            Looper.getMainLooper()
        )
    }

    private val goToUserLocationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult?) {
            super.onLocationResult(result)
            result?.locations?.let { locations ->
                updateCamera(locations[0])
                userFusedLocationProviderClient.removeLocationUpdates(this)
            }
        }
    }

    //endregion

    //region <<<<<<<<<<<<<<<<<<<<- MapBox ->>>>>>>>>>>>>>>>>>>>

    private fun updateCamera(location: Location) {
        if (this::mapboxMap.isInitialized) {
            mapboxMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        location.latitude,
                        location.longitude
                    ), 17.0
                )
            )
            mapboxMap.locationComponent.forceLocationUpdate(location)
            binding.locationLoading.isVisible = false
            binding.ivLocation.isVisible = true
        }
    }

    private fun fitCameraToPositionsAndEndRun() {
        try {
            mapboxMap.easeCamera(
                CameraUpdateFactory.newLatLngBounds(
                    latLngBounds.build(),
                    100,
                    400,
                    100,
                    400
                ),
                600
            )
        } catch (t: Throwable) {
            isRunTooSmall = true
            Timber.d(t)
        }
        isRunFinished = true
        handlePrimaryButton("ذخیره اطلاعات") {
            if (isRunTooSmall) {
                showRunTooSmallDialog()
            } else {
                endRunAndSaveToDb()
            }
        }
        handleSecondaryButton("", false) {}
    }

    private fun showRunTooSmallDialog() {
        showDialog(
            requireContext(),
            "هشدار",
            "اطلاعات کمتر از حد مجاز برای ذخیره است در نتیجه اطلاعات ذخیره نمیشود",
            "بسیار خب",
            "",
            {
                try {
                    findNavController().popBackStack()
                } catch (t: Throwable) {
                }
            },
            {
                try {
                    // TODO baw ok zakhire nakon vali khob bezar edame bede!!!
                    findNavController().popBackStack()
                } catch (t: Throwable) {
                }
            },
            false
        )
    }

    private fun moveCameraToLocation(latitude: Double, longitude: Double, zoom: Double) {
        if (this::mapboxMap.isInitialized) {
            mapboxMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        latitude,
                        longitude
                    ), zoom
                )
            )
        }
    }

    private fun drawLines(featureCollection: FeatureCollection) {
        if (this::mapboxMap.isInitialized) {
            mapboxMap.getStyle { style: Style ->
                if (featureCollection.features() != null) {
                    if (featureCollection.features()!!.size > 0) {
                        c += 1
                        style.addSource(GeoJsonSource("$lineSource$c", featureCollection))
                        style.addLayer(
                            LineLayer("$lineLayer$c", "$lineSource$c")
                                .withProperties(
                                    PropertyFactory.lineCap(Property.LINE_CAP_SQUARE),
                                    PropertyFactory.lineJoin(Property.LINE_JOIN_MITER),
                                    PropertyFactory.lineOpacity(.7f),
                                    PropertyFactory.lineWidth(3f),
                                    PropertyFactory.lineColor(Color.parseColor("#415A80"))
                                )
                        )
//                    moveCameraToLocation(35.740225, 51.414541, 17.0)
                    }
                }
            }
        }
    }

    @SuppressLint("VisibleForTests")
    private fun setUpMapBox(savedInstanceState: Bundle?) {
        mapBoxView = requireActivity().findViewById(R.id.mapBoxView)
        mapBoxView.onCreate(savedInstanceState)
        mapBoxView.getMapAsync(this)
        userFusedLocationProviderClient = FusedLocationProviderClient(requireActivity())
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            val localizationPlugin = LocalizationPlugin(mapBoxView, mapboxMap, style)
            localizationPlugin.setMapLanguage(MapLocale.LOCAL_NAME)
            loadedMapStyle = style
            enableLocationComponent(style)
//            binding.fabCurrentLocation.performClick()

            mapboxMap.uiSettings.apply {
                isLogoEnabled = false
            }

            // Retrieve and customize the Maps SDK's LocationComponent
            locationComponent = mapboxMap.locationComponent
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions
                    .builder(requireContext(), style)
                    .useDefaultLocationEngine(true)
                    .locationEngineRequest(
                        LocationEngineRequest.Builder(750)
                            .setFastestInterval(750)
                            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                            .build()
                    )
                    .build()
            )

            locationComponent.isLocationComponentEnabled = true
            locationComponent.addOnLocationClickListener(this)
            locationComponent.addOnCameraTrackingChangedListener(this)
            locationComponent.cameraMode = cameraMode
//            locationComponent.forceLocationUpdate(lastLocation)

//            setRendererMode(RenderMode.GPS)
//            setCameraTrackingMode(CameraMode.TRACKING)

        }
    }

    private fun setRendererMode(@RenderMode.Mode mode: Int) {
//        renderMode = mode
        locationComponent.renderMode = mode
    }

    private fun setCameraTrackingMode(@CameraMode.Mode mode: Int) {
        locationComponent.setCameraMode(mode, object : OnLocationCameraTransitionListener {
            override fun onLocationCameraTransitionFinished(@CameraMode.Mode cameraMode: Int) {
                if (mode != CameraMode.NONE) {
                    locationComponent.zoomWhileTracking(
                        17.0,
                        750,
                        object : MapboxMap.CancelableCallback {
                            override fun onCancel() {
                                // No impl
                            }

                            override fun onFinish() {
                                locationComponent.tiltWhileTracking(45.0)
                            }
                        })
                } else {
                    mapboxMap.easeCamera(CameraUpdateFactory.tiltTo(0.0))
                }
            }

            override fun onLocationCameraTransitionCanceled(@CameraMode.Mode cameraMode: Int) {
                // No impl
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(requireContext())) {

            // Create and customize the LocationComponent's options
            val customLocationComponentOptions = LocationComponentOptions.builder(requireContext())
                .trackingGesturesManagement(true)
                .accuracyColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.mapbox_plugins_green
                    )
                )
                .build()

            val locationComponentActivationOptions =
                LocationComponentActivationOptions.builder(requireContext(), loadedMapStyle)
                    .locationComponentOptions(customLocationComponentOptions)
                    .build()

            // Get an instance of the LocationComponent and then adjust its settings
            mapboxMap.locationComponent.apply {

                // Activate the LocationComponent with options
                activateLocationComponent(locationComponentActivationOptions)

                // Enable to make the LocationComponent visible
                isLocationComponentEnabled = true

                // Set the LocationComponent's camera mode
                cameraMode = CameraMode.TRACKING

                // Set the LocationComponent's render mode
                renderMode = RenderMode.COMPASS
            }
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(requireActivity())
        }
    }


    /*override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }*/

    override fun onExplanationNeeded(permissionsToExplain: List<String>) = Unit

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocationComponent(mapboxMap.style!!)
        } else {
        }
    }

    override fun onLocationComponentClick() = Unit

    override fun onCameraTrackingDismissed() = Unit

    override fun onCameraTrackingChanged(currentMode: Int) {
        this.cameraMode = currentMode
    }

    //endregion

    //region <<<<<<<<<<<<<<<<<<<<- overrides ->>>>>>>>>>>>>>>>>>>>

    override fun onDestroyView() {
        super.onDestroyView()
        callback.isEnabled = false
        callback.remove()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        mapBoxView.onStart()

    }

    override fun onResume() {
        super.onResume()
        mapBoxView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapBoxView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapBoxView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapBoxView.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapBoxView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapBoxView.onLowMemory()
    }

    //endregion

}