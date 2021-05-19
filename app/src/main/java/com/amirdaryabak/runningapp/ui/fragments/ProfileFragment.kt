package com.amirdaryabak.runningapp.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.amirdaryabak.runningapp.R
import com.amirdaryabak.runningapp.databinding.FragmentProfileBinding
import com.amirdaryabak.runningapp.eventbus.BottomNavigationShowEvent
import com.amirdaryabak.runningapp.storage.PrefsUtils
import com.amirdaryabak.runningapp.ui.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import java.lang.RuntimeException
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList


@AndroidEntryPoint
class ProfileFragment : BaseFragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
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
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        eventBus.post(BottomNavigationShowEvent())

        setValuesToViews()
        disableFields()

        setUpEditProfile()

        binding.apply {
            btnChangeTheme.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("bazaar://details?id=${requireActivity().packageName}")
                intent.setPackage("com.farsitel.bazaar")
                startActivity(intent)
                /*val url = "https://developer.chrome.com/docs/android/custom-tabs/integration-guide/"
                val builder = CustomTabsIntent.Builder()
                val params = CustomTabColorSchemeParams.Builder()
                    .setNavigationBarColor(ContextCompat.getColor(requireActivity(), R.color.colorAccent))
                    .setToolbarColor(ContextCompat.getColor(requireActivity(), R.color.colorPrimary))
                    .setSecondaryToolbarColor(
                        ContextCompat.getColor(
                            requireActivity(),
                            R.color.logoRed
                        )
                    )
                    .build()
                builder.setDefaultColorSchemeParams(params)
                val customTabsIntent = builder.build()
                customTabsIntent.launchUrl(requireContext(), Uri.parse(url))*/
                try {
                    findNavController().navigate(
                        ProfileFragmentDirections.actionProfileFragmentToNightModeDialog()
                    )
                } catch (t: Throwable) {
                }
            }
        }

    }

    private fun setLocale(languageCode: String = "fa") {
        resources.configuration.setLocale(Locale(languageCode))
    }

    private fun setUpEditProfile() {
        handleBtn(getString(R.string.change_your_information)) {
            binding.tvWelcome.text = getString(R.string.change_your_information)
            handleBtn(getString(R.string.apply_changes)) {
                if (validateInputs()) {
                    binding.tvWelcome.text = getString(R.string.your_information)
                    disableFields()
                }
                setUpEditProfile()
            }
            enableFields()
            setArrayListToSpWeight(createWeightArrayList())
            setArrayListToSpAge(createAgeArrayList())
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
                    spWeight.error = getString(R.string.enter_weight)
                }
                else -> {
                    isWeightValidated = true
                    spWeight.error = null
                }
            }
            when {
                spAge.text.toString().isEmpty() -> {
                    isAgeValidated = false
                    spAge.error = getString(R.string.enter_age)
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
        return if (isNameValidated && isFamilyNameValidated && isWeightValidated && isAgeValidated && isGenderValidated) {
            writePersonalDataToSharedPref()
        } else {
            false
        }
    }

    private fun handleBtn(text: String, func: () -> Unit) {
        binding.apply {
            btnEditProfile.text = text
            btnEditProfile.setOnClickListener {
                func.invoke()
            }
        }
    }

    private fun disableFields() {
        binding.apply {
            tilName.isEnabled = false
            tilFamilyName.isEnabled = false
            tilWeight.isEnabled = false
            tilAge.isEnabled = false
            btnFemale.setOnClickListener { }
            btnMale.setOnClickListener { }
        }
    }

    private fun enableFields() {
        binding.apply {
            tilName.isEnabled = true
            tilFamilyName.isEnabled = true
            tilWeight.isEnabled = true
            tilAge.isEnabled = true
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
            when (prefsUtils.getGender()) {
                0 -> btnFemale.performClick()
                1 -> btnMale.performClick()
            }
        }
    }

    private fun setValuesToViews() {
        binding.apply {
            tieName.setText(prefsUtils.getFirstName())
            tieFamilyName.setText(prefsUtils.getLastName())
            spWeight.setText(prefsUtils.getWeight().toString())
            spAge.setText(prefsUtils.getAge().toString())
            when (prefsUtils.getGender()) {
                0 -> {
                    txtDummyGender.isEnabled = true
                    btnFemale.isEnabled = false
                    btnFemale.isActivated = false
                    btnMale.isEnabled = true
                    btnMale.isActivated = false
                }
                1 -> {
                    txtDummyGender.isEnabled = true
                    btnMale.isEnabled = false
                    btnMale.isActivated = false
                    btnFemale.isEnabled = true
                    btnFemale.isActivated = false
                }
            }
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