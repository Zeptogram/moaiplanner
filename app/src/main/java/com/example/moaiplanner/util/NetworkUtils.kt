package com.example.moaiplanner.util

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.View
import com.example.moaiplanner.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object NetworkUtils {

   fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(
            NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    fun notifyMissingNetwork(context: Context, view: View, activity: Activity? = null) {

        if (!isNetworkAvailable(context)) {

            if (activity != null) {
                MaterialAlertDialogBuilder(context)
                    .setTitle(activity.getString(R.string.network_missing))
                    .setMessage(activity.getString(R.string.network_try))
                    .setPositiveButton(activity.getString(R.string.ok)) { _, _ ->
                        NavigationHelper.navigateToAndPop(view, R.id.noteFragment)
                    }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .create()
                    .show()
            }
        }
    }

    fun notifyMissingNetwork(context: Context, activity: Activity? = null) {

        if (!isNetworkAvailable(context)) {

            if (activity != null) {
                MaterialAlertDialogBuilder(context)
                    .setTitle(activity.getString(R.string.network_missing))
                    .setMessage(activity.getString(R.string.network_try))
                    .setPositiveButton(activity.getString(R.string.ok)) { _, _ ->
                        activity.finish()
                    }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .create()
                    .show()
            }
        }
    }



}
