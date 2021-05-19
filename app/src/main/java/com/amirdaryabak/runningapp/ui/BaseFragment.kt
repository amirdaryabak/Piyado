package com.amirdaryabak.runningapp.ui

import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.amirdaryabak.runningapp.R
import com.google.android.material.snackbar.Snackbar

abstract class BaseFragment(layoutId: Int): Fragment(layoutId) {

    fun showSnackbar(
        text: String,
        actionText: String = "بسیار خب",
        duration: Int = Snackbar.LENGTH_LONG,
        callFunction: () -> Unit = {}
    ) {
        Snackbar.make(
            requireView(), text,
            duration
        ).setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .setAction(actionText) {
                callFunction.invoke()
            }.apply {
                setActionTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
            }
            .show()
    }
}