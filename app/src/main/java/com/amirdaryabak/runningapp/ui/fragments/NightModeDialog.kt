package com.amirdaryabak.runningapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.amirdaryabak.runningapp.R
import com.amirdaryabak.runningapp.databinding.DialogNightModeBinding
import com.amirdaryabak.runningapp.storage.LocalStorage
import com.amirdaryabak.runningapp.ui.BaseBottomSheetDialogFragment
import com.amirdaryabak.runningapp.utils.NightModeManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NightModeDialog : BaseBottomSheetDialogFragment() {

    companion object {
        const val TAG = "SelectNightModeDialog"
    }

    @Inject
    lateinit var nightModeManager: NightModeManager

    @Inject
    lateinit var localStorage: LocalStorage

    private var _binding: DialogNightModeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogNightModeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupViews()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun setupViews() {
        val currentMode = NightModeManager.Mode.valueOf(
            localStorage.getNightMode() ?: NightModeManager.Mode.AUTO.name
        )
        when (currentMode) {
            NightModeManager.Mode.OFF -> binding.nightModeOffButton.isChecked = true
            NightModeManager.Mode.ON -> binding.nightModeOnButton.isChecked = true
            NightModeManager.Mode.AUTO -> binding.nightModeAutoButton.isChecked = true
        }
        binding.nightModeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.nightModeOffButton -> nightModeManager.setNightMode(NightModeManager.Mode.OFF)
                R.id.nightModeOnButton -> nightModeManager.setNightMode(NightModeManager.Mode.ON)
                R.id.nightModeAutoButton -> nightModeManager.setNightMode(NightModeManager.Mode.AUTO)
            }
            dismiss()
        }
    }

}