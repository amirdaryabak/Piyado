package com.amirdaryabak.runningapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.*
import androidx.fragment.app.viewModels
import com.amirdaryabak.runningapp.R
import com.amirdaryabak.runningapp.databinding.FragmentStatisticsBinding
import com.amirdaryabak.runningapp.eventbus.BottomNavigationShowEvent
import com.amirdaryabak.runningapp.other.CustomMarkerView
import com.amirdaryabak.runningapp.other.TrackingUtility
import com.amirdaryabak.runningapp.ui.BaseFragment
import com.amirdaryabak.runningapp.ui.viewmodels.StatisticsViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject
import kotlin.math.round

@AndroidEntryPoint
class StatisticsFragment : BaseFragment(R.layout.fragment_statistics) {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels()

    @Inject
    lateinit var eventBus: EventBus

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        eventBus.post(BottomNavigationShowEvent())

        subscribeToObservers()
        setupBarChart()
    }

    private fun setupBarChart() {
        binding.apply {
            barChart.xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawLabels(false)
                axisLineColor = getColor(requireContext(), R.color.colorPrimary)
                textColor = getColor(requireContext(), R.color.colorPrimary)
                textSize = 50F
                setDrawGridLines(true)
            }
            barChart.axisLeft.apply {
                axisLineColor = getColor(requireContext(), R.color.colorPrimary)
                textColor = getColor(requireContext(), R.color.colorPrimary)
                textSize = 50F
                setDrawGridLines(true)
            }
            barChart.axisRight.apply {
                axisLineColor = getColor(requireContext(), R.color.colorPrimary)
                textColor = getColor(requireContext(), R.color.colorPrimary)
                textSize = 50F
                setDrawGridLines(true)
            }
            barChart.apply {
                description.text = getString(R.string.average_speed_over_time)
                description.textColor = getColor(requireContext(), R.color.colorPrimary)
                description.textSize = 20F
                legend.isEnabled = false
            }
        }
    }

    private fun subscribeToObservers() {
        viewModel.totalTimeRun.observe(viewLifecycleOwner) {
            it?.let {
                val totalTimeRun = TrackingUtility.getFormattedStopWatchTime(it)
                binding.tvTotalTime.text = totalTimeRun
            }
        }
        viewModel.totalDistance.observe(viewLifecycleOwner){
            it?.let {
                val km = it / 1000f
                val totalDistance = round(km * 10f) / 10f
                val totalDistanceString = "${totalDistance}km"
                binding.tvTotalDistance.text = totalDistanceString
            }
        }
        viewModel.totalAvgSpeed.observe(viewLifecycleOwner){
            it?.let {
                val avgSpeed = round(it * 10f) / 10f
                val avgSpeedString = "${avgSpeed}km/h"
                binding.tvAverageSpeed.text = avgSpeedString
            }
        }
        viewModel.totalCaloriesBurned.observe(viewLifecycleOwner) {
            it?.let {
                val totalCalories = "${it}kcal"
                binding.tvTotalCalories.text = totalCalories
            }
        }
        viewModel.runsSortedByDate.observe(viewLifecycleOwner) {
            it?.let {
                val allAvgSpeeds = it.indices.map { i -> BarEntry(i.toFloat(), it[i].avgSpeedInKMH) }
                val barDataSet = BarDataSet(allAvgSpeeds, getString(R.string.average_speed_over_time)).apply {
                    valueTextColor = getColor(requireContext(), R.color.colorPrimary)
                    color = getColor(requireContext(), R.color.colorAccent)
                }
                binding.apply {
                    barChart.data = BarData(barDataSet)
                    barChart.marker = CustomMarkerView(it.reversed(), requireContext(), R.layout.marker_view)
                    barChart.invalidate()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}