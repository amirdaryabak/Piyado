package com.amirdaryabak.runningapp.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.amirdaryabak.runningapp.R
import com.amirdaryabak.runningapp.databinding.ActivityMainBinding
import com.amirdaryabak.runningapp.other.Constants
import dagger.hilt.android.AndroidEntryPoint
import com.amirdaryabak.runningapp.eventbus.BottomNavigationShowEvent
import com.amirdaryabak.runningapp.services.ForegroundOnlyLocationService
import ir.netbar.nbdriver.navigation.setupWithNavController
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var currentNavController: LiveData<NavController>? = null

    private var haveToFinishApp: Boolean = true

    @Inject
    lateinit var eventBus: EventBus

    private var foregroundOnlyLocationServiceBound = false

    private var foregroundOnlyLocationService: ForegroundOnlyLocationService? = null

    private val foregroundOnlyServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ForegroundOnlyLocationService.LocalBinder
            foregroundOnlyLocationService = binder.service
            foregroundOnlyLocationServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            foregroundOnlyLocationService = null
            foregroundOnlyLocationServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setLocale()
        setContentView(binding.root)

        if (savedInstanceState == null) {
            setupBottomNavigationBar()
        }

        navigateToTrackingFragmentIfNeeded(intent)


    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBottomNavigationShowEvent(bottomNavigationShowEvent: BottomNavigationShowEvent) {
        if (bottomNavigationShowEvent.show) {
            visibleBottomNavigation()
        } else {
            goneBottomNavigation()
        }
        haveToFinishApp = bottomNavigationShowEvent.haveToFinishApp
    }

    private fun visibleBottomNavigation() {
        binding.bottomNavigationView.visibility = View.VISIBLE
    }

    private fun goneBottomNavigation() {
        binding.bottomNavigationView.visibility = View.GONE
    }

    private fun setupBottomNavigationBar() {
        val navGraphIds = listOf(
            R.navigation.nav_home_graph,
            R.navigation.nav_statistics_graph,
            R.navigation.nav_profile_graph,
        )

        val controller = binding.bottomNavigationView.setupWithNavController(
            navGraphIds = navGraphIds,
            fragmentManager = supportFragmentManager,
            containerId = R.id.nav_host_fragment,
            intent = intent
        )

        controller.observe(this) { navController ->
            setupActionBarWithNavController(navController)
        }
        currentNavController = controller
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        setupBottomNavigationBar()
    }

    override fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp() ?: false
    }

    override fun onStart() {
        super.onStart()
        eventBus.register(this)

        val serviceIntent = Intent(this, ForegroundOnlyLocationService::class.java)
        bindService(
            serviceIntent,
            foregroundOnlyServiceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        super.onStop()
        eventBus.unregister(this)

        if (foregroundOnlyLocationServiceBound) {
            unbindService(foregroundOnlyServiceConnection)
            foregroundOnlyLocationServiceBound = false
        }
    }

    private fun setLocale(languageCode: String = "fa") {
        resources.configuration.setLocale(Locale(languageCode))
    }

    override fun onBackPressed() {
        if (haveToFinishApp) {
            finish()
        } else {
            super.onBackPressed()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navigateToTrackingFragmentIfNeeded(intent)
    }

    private fun navigateToTrackingFragmentIfNeeded(intent: Intent?) {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        when (intent?.action) {
            Constants.ACTION_SHOW_TRACKING_FRAGMENT -> {
                NavHostFragment.findNavController(navHostFragment).navigate(
                    R.id.action_global_runningFragment,
                    Bundle().apply {
//                        putString("kazem")
                    }
                )
            }
            Constants.ACTION_SHOW_RUN_ITEM_FRAGMENT -> {
                val runId = intent.getIntExtra("runId", 1)
                NavHostFragment.findNavController(navHostFragment).navigate(
                    R.id.action_global_runItemFragment,
                    Bundle().apply {
                        putInt("runId", runId)
                    }
                )
            }
        }
    }

}