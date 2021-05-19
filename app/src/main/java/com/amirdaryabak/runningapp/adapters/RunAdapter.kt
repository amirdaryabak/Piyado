package com.amirdaryabak.runningapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amirdaryabak.runningapp.databinding.ItemRunBinding
import com.amirdaryabak.runningapp.db.Run
import com.amirdaryabak.runningapp.other.TrackingUtility
import com.amirdaryabak.runningapp.utils.dateUtils.DateUtils
import com.bumptech.glide.Glide
import java.util.*

class RunAdapter(
    private val clickListener: (Run, Int) -> Unit,
) : ListAdapter<Run, RunAdapter.RunViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RunViewHolder {
        val binding = ItemRunBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RunViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RunViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem)
    }

    inner class RunViewHolder(private val binding: ItemRunBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.apply {
                root.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val run = getItem(position)
                        clickListener.invoke(run, adapterPosition)
                    }
                }
                tvCalories.setOnClickListener {
                    root.performClick()
                }
            }
        }

        fun bind(run: Run) {
            binding.apply {

                val calendar = Calendar.getInstance().apply {
                    timeInMillis = run.timestamp
                }

                tvDate.text = DateUtils.getIranianDate(calendar)

                val avgSpeed = "${run.avgSpeedInKMH}km/h"
                tvAvgSpeed.text = avgSpeed

                val distanceInKm = "${run.distanceInMeters / 1000f}km"
                tvDistance.text = distanceInKm

                tvTime.text = TrackingUtility.getFormattedStopWatchTime(run.timeInMillis)

                val caloriesBurned = "${run.caloriesBurned} kcal"
                tvCalories.text = caloriesBurned
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Run>() {
        override fun areItemsTheSame(oldItem: Run, newItem: Run) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Run, newItem: Run) =
            oldItem == newItem
    }

}














