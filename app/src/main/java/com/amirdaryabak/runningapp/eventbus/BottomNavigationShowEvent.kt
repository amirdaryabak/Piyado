package com.amirdaryabak.runningapp.eventbus

data class BottomNavigationShowEvent(
    val show: Boolean = true,
    val haveToFinishApp: Boolean = false,
    val haveToPerformClick: Boolean = false,
)
