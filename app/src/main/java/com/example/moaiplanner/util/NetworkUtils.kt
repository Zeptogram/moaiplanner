package com.example.moaiplanner.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.View
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.example.moaiplanner.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object NetworkUtils {

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_CELLULAR)
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnectedOrConnecting
        }
    }

    fun notifyMissingNetwork(context: Context, view: View, activity: Activity? = null) {

        if (!isNetworkAvailable(context)) {

            MaterialAlertDialogBuilder(context)
                .setTitle("Connessione di rete assente")
                .setMessage("Connettiti alla rete e riprova")
                .setPositiveButton("OK") { dialog, which ->
                    if(activity!= null) {
                        activity.finish()
                    } else {
                        view.findNavController().popBackStack()
                        view.findNavController().navigate(R.id.noteFragment)
                    }
                }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .create()
                .show()
        }
    }



}
