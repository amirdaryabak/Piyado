package com.amirdaryabak.runningapp.ui.fragments

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.amirdaryabak.runningapp.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CancelTrackingDialog :  DialogFragment() {

    private var yesListener: (() -> Unit)? = null

    fun setYesListener(listener: () -> Unit) {
        yesListener = listener
    }

    private var noListener: (() -> Unit)? = null

    fun setNoListener(listener: () -> Unit) {
        noListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.cancel_run))
            .setMessage(getString(R.string.dialog_message))
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                yesListener?.let {yes ->
                    yes()
                }
            }
            .setNegativeButton(getString(R.string.no)) { dialogInterface, _ ->
                noListener?.let { no ->
                    no()
                }
            }
            .create()
    }
}