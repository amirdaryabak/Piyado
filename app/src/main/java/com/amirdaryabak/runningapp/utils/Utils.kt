package com.amirdaryabak.runningapp.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import es.dmoral.toasty.Toasty

object Utils {

    fun showDialog(
        context: Context,
        title: String,
        message: String,
        positiveText: String,
        negativeText: String,
        positiveButtonFun: () -> Unit,
        negativeButtonFun: () -> Unit,
        isCancelable: Boolean = true
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(isCancelable)
            .setPositiveButton(positiveText) { dialog, which ->
                positiveButtonFun.invoke()
            }
            .setNegativeButton(negativeText) { dialog, which ->
                negativeButtonFun.invoke()
            }
            .show()
    }

    fun toasty(context: Context, message: String, type: Int = 1, length: Int = Toast.LENGTH_SHORT) {
        /**
         * 1 normal
         * 2 success
         * 3 error
         * 4 info
         * 5 warning
         */
        when (type) {
            1 -> Toasty.normal(context, message, length).show()
            2 -> Toasty.success(context, message, length).show()
            3 -> Toasty.error(context, message, length).show()
            4 -> Toasty.info(context, message, length).show()
            5 -> Toasty.warning(context, message, length).show()
        }
    }

    @SuppressLint("HardwareIds")
    fun getAndroidId(context: Context): String = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    )

    fun isPackageNameInstalledOrNot(activity: Activity, packageNme: String): Boolean {
        val pm: PackageManager = activity.packageManager
        try {
            pm.getPackageInfo(packageNme, PackageManager.GET_ACTIVITIES)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
        }
        return false
    }

}