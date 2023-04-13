package com.moai.planner.util

import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.moai.planner.R

class NavigationHelper {

    companion object {
        fun navigateToAndPop(view: View, fragment: Int) {
            findNavController(view).popBackStack()
            findNavController(view).navigate(fragment, null,
                navOptions {
                    anim {
                        enter = androidx.appcompat.R.anim.abc_fade_in
                        popEnter = androidx.appcompat.R.anim.abc_fade_in
                        exit =  androidx.appcompat.R.anim.abc_fade_out
                        popExit = androidx.appcompat.R.anim.abc_fade_out
                    }
                }, null
            )
        }

        fun navigateTo(view: View, fragment: Int) {
            findNavController(view).navigate(fragment, null,
                navOptions {
                    anim {
                        enter = androidx.appcompat.R.anim.abc_fade_in
                        popEnter = androidx.appcompat.R.anim.abc_fade_in
                        exit =  androidx.appcompat.R.anim.abc_fade_out
                        popExit = androidx.appcompat.R.anim.abc_fade_out

                    }
                }, null
            )
        }

        fun navigateFromActivity(navHostFragment: NavHostFragment, fragment: Int){
            navHostFragment.findNavController().popBackStack()
            navHostFragment.findNavController().navigate(fragment, null,
                navOptions {
                    anim {
                        enter = androidx.appcompat.R.anim.abc_fade_in
                        popEnter = androidx.appcompat.R.anim.abc_fade_in
                        exit =  androidx.appcompat.R.anim.abc_fade_out
                        popExit = androidx.appcompat.R.anim.abc_fade_out
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