package com.example.moaiplanner.ui.welcome

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebase = AuthRepository(requireActivity().application)

        val storage = Firebase.storage
        val storageRef = storage.reference
        val image = storageRef.child("cat_gang.png")

        image.downloadUrl.addOnCompleteListener { task ->
            var bitmap: Bitmap? = null
            lifecycleScope.launch(Dispatchers.IO) {
                bitmap = convertBitmapFromURL(task.result.toString())
            }.invokeOnCompletion {
                updateUI(bitmap)
            }
        }

        binding.buttonEmail.setOnClickListener {
            findNavController().navigate(R.id.signinFragment)
        }

        binding.buttonGoogle.setOnClickListener {
            findNavController().navigate(R.id.googleSignInActivity)
        }

        binding.textViewSignUpNow.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }
    }
}