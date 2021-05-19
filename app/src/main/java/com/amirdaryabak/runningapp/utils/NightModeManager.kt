package com.amirdaryabak.runningapp.utils

import androidx.appcompat.app.AppCompatDelegate
import com.amirdaryabak.runningapp.storage.LocalStorage
import javax.inject.Inject

class NightModeManager
@Inject constructor(
    private val localStorage: LocalStorage
) {

    enum class Mode {
        ON,
        OFF,
        AUTO
    }

    fun setNightMode(mode: Mode) {

        val currentMode = getCurrentNightMode()

        if (mode == currentMode) {
            return
        }

        setMode(mode)

    }

    fun setDefaultNightMode() {
        val currentMode = getCurrentNightMode()
        setMode(currentMode)
    }

    private fun getCurrentNightMode(): Mode {
        return Mode.valueOf(localStorage.getNightMode() ?: Mode.OFF.name)
    }

    private fun setMode(currentMode: Mode) {
        when (currentMode) {
            Mode.ON -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            Mode.OFF -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            Mode.AUTO -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        localStorage.setNightMode(currentMode.name)
    }

}