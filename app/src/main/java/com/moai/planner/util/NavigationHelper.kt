package com.moai.planner.util

import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions

class NavigationHelper {

    companion object {
        fun navigateToAndPop(view: View, fragment: Int) {
            findNavController(view).popBackStack()
            findNavController(view).navigate(fragment, null,
                navOptions {
                    anim {
                        enter = android.R.anim.fade_in
                        popEnter = android.R.anim.fade_in
                    }
                }, null
            )
        }

        fun navigateTo(view: View, fragment: Int) {
            findNavController(view).navigate(fragment, null,
                navOptions {
                    anim {
                        enter = android.R.anim.fade_in
                        popEnter = android.R.anim.fade_in
                    }
                }, null
            )
        }

        fun navigateFromActivity(navHostFragment: NavHostFragment, fragment: Int){
            navHostFragment.findNavController().popBackStack()
            navHostFragment.findNavController().navigate(fragment, null,
                navOptions {
                    anim {
                        enter = android.R.anim.fade_in
                        popEnter = android.R.anim.fade_in
                    }
                }, null
            )
        }

        fun changeActivity(currentActivity: Activity, newActivity: Class<*>) {
            val intent = Intent(currentActivity, newActivity)
            currentActivity.startActivity(intent)
            currentActivity.finish()
        }
    }
}