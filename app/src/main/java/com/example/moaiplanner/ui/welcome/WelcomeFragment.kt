package com.example.moaiplanner.ui.welcome

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.moaiplanner.R
import com.example.moaiplanner.data.repository.user.AuthRepository
import com.example.moaiplanner.databinding.WelcomeFragmentBinding
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class WelcomeFragment : Fragment() {
    lateinit var binding: WelcomeFragmentBinding
    lateinit var firebase: AuthRepository
    private val REQ_ONE_TAP = 2
    private var showOneTapUI = true

    fun newInstance(): SigninFragment? {
        return SigninFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = WelcomeFragmentBinding.inflate(inflater, container, false)
        // Inflate il layout per il fragment
        return binding.root
    }

    // TODO: Rimuovere, vecchio codice per scaricare da firebase l'immagine prova dell'app
    /*
    fun convertBitmapFromURL(url: String): Bitmap? {
        try {
            val url = URL(url)
            val input = url.openStream()

            return BitmapFactory.decodeStream(input)
        } catch (e: IOException) {
            Log.d("Exception", e.toString())
        }

        return null
    }

    fun updateUI(bitmap: Bitmap?) {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.imageViewMoaiLogo.setImageBitmap(bitmap)
        }
    }
     */

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebase = AuthRepository(requireActivity().application)

        binding.buttonEmail.setOnClickListener {
            findNavController().navigate(R.id.signinFragment, null,
                navOptions {
                    anim {
                        enter = android.R.anim.fade_in
                        popEnter = android.R.anim.fade_in
                    }
                }, null)
        }

        /*binding.buttonGoogle.setOnClickListener {
            findNavController().navigate(R.id.googleSignInActivity, null,
                navOptions {
                    anim {
                        enter = android.R.anim.fade_in
                        popEnter = android.R.anim.fade_in

                    }
                }, null)
        }*/

        binding.textViewSignUpNow.setOnClickListener {
            findNavController().navigate(R.id.registerFragment, null,
                navOptions {
                    anim {
                        enter = android.R.anim.fade_in
                        popEnter = android.R.anim.fade_in

                    }
                }, null)
        }
    }
}