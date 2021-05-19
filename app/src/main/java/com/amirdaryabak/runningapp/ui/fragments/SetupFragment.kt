package com.amirdaryabak.runningapp.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.amirdaryabak.runningapp.R
import com.amirdaryabak.runningapp.databinding.FragmentSetupBinding
import com.amirdaryabak.runningapp.storage.PrefsUtils
import com.amirdaryabak.runningapp.ui.BaseFragment
import com.amirdaryabak.runningapp.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

@AndroidEntryPoint
class SetupFragment : BaseFragment(R.layout.fragment_setup) {

    private var _binding: FragmentSetupBinding? = null
    private val binding get() = _binding!!

    private var selectedGender = -1

    @Inject
    lateinit var eventBus: EventBus

    @Inject
    lateinit var prefsUtils: PrefsUtils

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (prefsUtils.isLogin()) {
            intentToMainActivity()
        }

        setArrayListToSpWeight(createWeightArrayList())
        setArrayListToSpAge(createAgeArrayList())

        binding.apply {
            btnMale.setOnClickListener {
                selectedGender = 1
                txtDummyGender.isEnabled = true
                btnMale.isEnabled = false
                btnMale.isActivated = false
                btnFemale.isEnabled = true
                btnFemale.isActivated = false
            }
            btnFemale.setOnClickListener {
                selectedGender = 0
                txtDummyGender.isEnabled = true
                btnFemale.isEnabled = false
                btnFemale.isActivated = false
                btnMale.isEnabled = true
                btnMale.isActivated = false
            }
            btnSubmit.setOnClickListener {
                if (validateInputs()) {
                    intentToMainActivity()
                }
            }
        }

    }

    private fun intentToMainActivity() {
        Intent(requireActivity(), MainActivity::class.java).also {
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(it)
            requireActivity().finish()
        }
    }

    private fun setErrorToPickGenderBtn() {
        binding.apply {
            txtDummyGender.isEnabled = false
            btnMale.isEnabled = true
            btnMale.isActivated = true
            btnFemale.isEnabled = true
            btnFemale.isActivated = true
        }
    }

    private fun validateInputs(): Boolean {
        var isNameValidated: Boolean
        var isFamilyNameValidated: Boolean
        var isWeightValidated: Boolean
        var isAgeValidated: Boolean
        var isGenderValidated: Boolean
        binding.apply {
            when {
                tieName.text.toString().isEmpty() -> {
                    isNameValidated = false
                    tilName.error = getString(R.string.enter_name)
                }
                tieName.text.toString().length <= 2 -> {
                    isNameValidated = false
                    tilName.error = getString(R.string.enter_valid_name)
                }
                else -> {
                    isNameValidated = true
                    tilName.error = null
                }
            }
            when {
                tieFamilyName.text.toString().isEmpty() -> {
                    isFamilyNameValidated = false
                    tilFamilyName.error = getString(R.string.enter_family_name)
                }
                tieFamilyName.text.toString().length <= 2 -> {
                    isFamilyNameValidated = false
                    tilFamilyName.error = getString(R.string.enter_valid_family_name)
                }
                else -> {
                    isFamilyNameValidated = true
                    tilFamilyName.error = null
                }
            }
            when {
                spWeight.text.toString().isEmpty() -> {
                    isWeightValidated = false
                    tilWeight.error = getString(R.string.enter_weight)
                }
                else -> {
                    isWeightValidated = true
                    spWeight.error = null
                }
            }
            when {
                spAge.text.toString().isEmpty() -> {
                    isAgeValidated = false
                    tilAge.error = getString(R.string.enter_age)
                }
                else -> {
                    isAgeValidated = true
                    spAge.error = null
                }
            }
            if (selectedGender == -1) {
                isGenderValidated = false
                setErrorToPickGenderBtn()
            } else {
                isGenderValidated = true
            }
        }
        return if(isNameValidated && isFamilyNameValidated && isWeightValidated && isAgeValidated && isGenderValidated) {
            writePersonalDataToSharedPref()
        } else {
            false
        }
    }

    private fun writePersonalDataToSharedPref(): Boolean {
        binding.apply {
            prefsUtils.apply {
                setFirstName(tieName.text.toString())
                setLastName(tieFamilyName.text.toString())
                setWeight(spWeight.text.toString().toLong())
                setAge(spAge.text.toString().toInt())
                setGender(selectedGender)
            }
            return true
        }
    }

    private fun createAgeArrayList(): ArrayList<Int> {
        val arrayList = ArrayList<Int>()
        for (i in 10..100) {
            arrayList.add(i)
        }
        return arrayList
    }

    private fun createWeightArrayList(): ArrayList<Long> {
        val arrayList = ArrayList<Long>()
        for (i in 40L..150L) {
            arrayList.add(i)
        }
        return arrayList
    }

    private fun setArrayListToSpAge(myArrayList: ArrayList<Int>) {
        val adapter =
            ArrayAdapter(requireContext(), R.layout.spinner_item, myArrayList)
        binding.apply {
            (tilAge.editText as? AutoCompleteTextView)?.setAdapter(adapter)
            (tilAge.editText as? AutoCompleteTextView)?.setOnItemClickListener { adapterView, view, i, l ->

            }
        }

    }

    private fun setArrayListToSpWeight(myArrayList: ArrayList<Long>) {
        val adapter =
            ArrayAdapter(requireContext(), R.layout.spinner_item, myArrayList)
        binding.apply {
            (tilWeight.editText as? AutoCompleteTextView)?.setAdapter(adapter)
            (tilWeight.editText as? AutoCompleteTextView)?.setOnItemClickListener { adapterView, view, i, l ->

            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}