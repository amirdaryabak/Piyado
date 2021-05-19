package com.amirdaryabak.runningapp.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amirdaryabak.runningapp.R
import com.amirdaryabak.runningapp.adapters.RunAdapter
import com.amirdaryabak.runningapp.databinding.CustomViewDialogBinding
import com.amirdaryabak.runningapp.databinding.FragmentHomeBinding
import com.amirdaryabak.runningapp.eventbus.BottomNavigationShowEvent
import com.amirdaryabak.runningapp.other.SortType
import com.amirdaryabak.runningapp.storage.PrefsUtils
import com.amirdaryabak.runningapp.ui.BaseFragment
import com.amirdaryabak.runningapp.ui.viewmodels.MainViewModel
import com.amirdaryabak.runningapp.utils.SwipeToDeleteCallbackLeft
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_AROUND_PERMISSIONS_REQUEST_CODE = 35

@AndroidEntryPoint
class HomeFragment : BaseFragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()

    private lateinit var materialAlertDialogBuilder: AlertDialog
    private lateinit var customAlertDialogView: CustomViewDialogBinding

    private lateinit var runAdapter: RunAdapter

    private lateinit var timer: CountDownTimer

    @Inject
    lateinit var eventBus: EventBus

    @Inject
    lateinit var prefsUtils: PrefsUtils

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        customAlertDialogView = CustomViewDialogBinding.inflate(inflater, container, false)
        materialAlertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
            .setView(customAlertDialogView.root)
            .create()

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        eventBus.post(BottomNavigationShowEvent(haveToFinishApp = true))

        setupRecyclerView()
        enableSwipeToCargoesRecyclerView()
        if (!foregroundPermissionApproved()) {
            if (!prefsUtils.getAskAgainLocationPermission()) {
                launchCustomAlertDialog()
            }
        }

        when (viewModel.sortType) {
            SortType.DATE -> binding.spFilter.setSelection(0)
            SortType.RUNNING_TIME -> binding.spFilter.setSelection(1)
            SortType.DISTANCE -> binding.spFilter.setSelection(2)
            SortType.AVG_SPEED -> binding.spFilter.setSelection(3)
            SortType.CALORIES_BURNED -> binding.spFilter.setSelection(4)
        }

        binding.apply {
            fab.setOnClickListener {
                if (foregroundPermissionApproved()) {
                    try {
                        findNavController().navigate(
                            HomeFragmentDirections.actionHomeFragmentToRunningFragment()
                        )
                    } catch (t: Throwable) {}
                } else {
                    requestForegroundPermissions(REQUEST_AROUND_PERMISSIONS_REQUEST_CODE)
                }
            }
        }

        binding.spFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                pos: Int,
                id: Long
            ) {
                when (pos) {
                    0 -> viewModel.sortRuns(SortType.DATE)
                    1 -> viewModel.sortRuns(SortType.RUNNING_TIME)
                    2 -> viewModel.sortRuns(SortType.DISTANCE)
                    3 -> viewModel.sortRuns(SortType.AVG_SPEED)
                    4 -> viewModel.sortRuns(SortType.CALORIES_BURNED)
                }
            }
        }

        viewModel.runs.observe(viewLifecycleOwner) {
            runAdapter.submitList(it)
            if (it.isEmpty()) {
                binding.emptyListTv.visibility = View.VISIBLE
            } else {
                binding.emptyListTv.visibility = View.GONE
            }
        }

    }

    private fun launchCustomAlertDialog() {
        materialAlertDialogBuilder.show()
        customAlertDialogView.apply {
            btnAgree.setOnClickListener {
                prefsUtils.setAskAgainLocationPermission(cbDoNotAskAgain.isChecked)
                requestForegroundPermissions(REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE)
                materialAlertDialogBuilder.dismiss()
            }
            btnNotAgree.setOnClickListener {
                prefsUtils.setAskAgainLocationPermission(cbDoNotAskAgain.isChecked)
                materialAlertDialogBuilder.dismiss()
            }
        }
    }

    private fun setupRecyclerView() = binding.rvRuns.apply {
        runAdapter = RunAdapter(
            clickListener = { item , position ->
                try {
                    findNavController().navigate(
                        HomeFragmentDirections.actionHomeFragmentToRunItemFragment(
                            item.id!!
                        )
                    )
                } catch (t: Throwable) {}
            }
        )
        adapter = runAdapter
        layoutManager = LinearLayoutManager(requireContext())
        addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
    }

    private fun requestForegroundPermissions(requestCode: Int) {
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            requestCode
        )
    }

    private fun foregroundPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_AROUND_PERMISSIONS_REQUEST_CODE -> when (PackageManager.PERMISSION_GRANTED) {
                grantResults[0] -> {
                    try {
                        findNavController().navigate(
                            HomeFragmentDirections.actionHomeFragmentToRunningFragment()
                        )
                    } catch (t: Throwable) {}
                }
            }
        }
    }
    private fun enableSwipeToCargoesRecyclerView() {
        val swipeToDeleteCallback: SwipeToDeleteCallbackLeft =
            object : SwipeToDeleteCallbackLeft(requireContext()) {
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, i: Int) {
                    val position = viewHolder.layoutPosition
                    val item = runAdapter.currentList[position]
                    handleTimer(
                        text = "در صورت ادامه، از لیست شما حذف میشود، آیا مطمئن هستید؟",
                        cancelFunction = {
                            runAdapter.notifyItemChanged(position)
                        },
                        callFunction = {
                            runAdapter.notifyItemChanged(position)
                            viewModel.deleteRun(item)
                        }
                    )
                }
            }
        val itemTouchHelperRight = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelperRight.attachToRecyclerView(binding.rvRuns)
    }

    private fun handleTimer(
        text: String,
        second: Int = 10,
        cancelFunction: () -> Unit,
        callFunction: () -> Unit,
    ) {
        binding.warningUi.apply {
            timer = object : CountDownTimer((second * 1000 + 1000).toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    var seconds = (millisUntilFinished / 1000).toInt()
                    seconds %= 60
                    txtTimer.text = seconds.toString()
                }
                override fun onFinish() {
                    txtTimer.text = "0"
                    warningView.visibility = View.GONE
                    cancelFunction.invoke()
                }
            }.start()
            warningView.setOnClickListener { /* NO-OP */ }
            warningView.visibility = View.VISIBLE
            txtErrorText.text = text
            warningAnimation.visibility = View.VISIBLE
            txtTimer.visibility = View.VISIBLE
            btnAgree.visibility = View.VISIBLE
            btnAgree.setOnClickListener {
                timer.cancel()
                warningView.visibility = View.GONE
                callFunction.invoke()
            }
            btnCancel.visibility = View.VISIBLE
            btnCancel.setOnClickListener {
                warningView.visibility = View.GONE
                timer.cancel()
                cancelFunction.invoke()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (this::timer.isInitialized) {
            timer.cancel()
        }
    }

    override fun onStop() {
        super.onStop()
        if (this::timer.isInitialized) {
            timer.cancel()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (this::timer.isInitialized) {
            timer.cancel()
        }
        _binding = null
    }
}