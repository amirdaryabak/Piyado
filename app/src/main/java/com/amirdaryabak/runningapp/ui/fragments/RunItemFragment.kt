package com.amirdaryabak.runningapp.ui.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.amirdaryabak.runningapp.R
import com.amirdaryabak.runningapp.databinding.FragmentRunItemBinding
import com.amirdaryabak.runningapp.eventbus.BottomNavigationShowEvent
import com.amirdaryabak.runningapp.models.Feature
import com.amirdaryabak.runningapp.models.GeoJson
import com.amirdaryabak.runningapp.models.Geometry
import com.amirdaryabak.runningapp.other.TrackingUtility
import com.amirdaryabak.runningapp.ui.BaseFragment
import com.amirdaryabak.runningapp.ui.viewmodels.MainViewModel
import com.amirdaryabak.runningapp.utils.dateUtils.DateUtils
import com.google.gson.Gson
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.localization.LocalizationPlugin
import com.mapbox.mapboxsdk.plugins.localization.MapLocale
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_running.*
import org.greenrobot.eventbus.EventBus
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class RunItemFragment : BaseFragment(R.layout.fragment_run_item), OnMapReadyCallback {

    private var _binding: FragmentRunItemBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<MainViewModel>()
    private val args by navArgs<RunItemFragmentArgs>()


    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapBoxView: MapView
    private lateinit var loadedMapStyle: Style

    private val lineSource: String = "line-source"
    private val lineLayer: String = "linelayer"
    private var c = 0

    private lateinit var callback: OnBackPressedCallback

    @Inject
    lateinit var eventBus: EventBus

    private val TAG = this::class.java.name


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRunItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        eventBus.post(BottomNavigationShowEvent())


        onCustomBackPressed()
        setUpMapBox(savedInstanceState)

        binding.apply {
            btnCalories.isEnabled = false
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
                    }
                }
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

    private fun onCustomBackPressed() {
        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
//                if (isRunFinished) {
                /*showDialog(
                    requireContext(),
                    "هشدار",
                    "با خروج اطلاعات ذخیره نمیشود، آیا از خروج مطمئن هستید؟",
                    "بله",
                    "خیر",
                    { findNavController().popBackStack() },
                    { },
                )*/
//                } else {
                try {
                    findNavController().popBackStack()
                } catch (t: Throwable) {
                }
//                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(callback)
    }

    @SuppressLint("VisibleForTests")
    private fun setUpMapBox(savedInstanceState: Bundle?) {
        mapBoxView = requireActivity().findViewById(R.id.mapBoxView)
        mapBoxView.onCreate(savedInstanceState)
        mapBoxView.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            val localizationPlugin = LocalizationPlugin(mapBoxView, mapboxMap, style)
            localizationPlugin.setMapLanguage(MapLocale.LOCAL_NAME)
            loadedMapStyle = style

            mapboxMap.uiSettings.apply {
                isLogoEnabled = false
//                isZoomGesturesEnabled = true
//                isScrollGesturesEnabled = false
                setAllGesturesEnabled(false)
            }

            val geoJson = GeoJson()
            viewModel.getRunWithGeometryDBWithRunId(args.runId)
                .observe(viewLifecycleOwner) { runWithGeometryDB ->
                    binding.apply {
                        val run = runWithGeometryDB.run
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = run.timestamp
                        }
                        val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
                        tvDate.text = dateFormat.format(calendar.time)

                        val avgSpeed = "${run.avgSpeedInKMH}km/h"
                        tvAvgSpeed.text = avgSpeed

                        val distanceInKm = "${run.distanceInMeters / 1000f}km"
                        tvDistance.text = distanceInKm

                        tvTime.text = TrackingUtility.getFormattedStopWatchTime(run.timeInMillis)

                        val caloriesBurned = "${run.caloriesBurned} kcal"
                        btnCalories.text = caloriesBurned

                        shareContent(
                            DateUtils.getIranianDate(calendar),
                            avgSpeed,
                            distanceInKm,
                            caloriesBurned
                        )
                    }
                    val latLngBounds: LatLngBounds.Builder = LatLngBounds.Builder()
                    runWithGeometryDB.geometryDB.forEachIndexed { index, geometryDB ->
                        geoJson.features[index].geometry = Geometry(
                            geometryDB.coordinates,
                            geometryDB.type,
                        )
                        geometryDB.coordinates.forEach { coordinates ->
                            latLngBounds.include(
                                LatLng(coordinates[1], coordinates[0])
                            )
                        }
                        geoJson.features.add(Feature())
                    }
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
                    drawLines(FeatureCollection.fromJson(Gson().toJson(geoJson)))
                }
        }
    }

    private fun shareContent(
        dateFormat: String,
        avgSpeed: String,
        distanceInKm: String,
        caloriesBurned: String
    ) {
        binding.apply {
            btnPrimary.setOnClickListener {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "تاریخ: $dateFormat \n" +
                                "میانگین سرعت: $avgSpeed \n" +
                                "مسافت: $distanceInKm \n" +
                                "کالری سوخته شده: $caloriesBurned"
                    )
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(sendIntent, null))
            }
        }
    }

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