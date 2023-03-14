package com.example.moaiplanner.ui.main


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.moaiplanner.R
import com.example.moaiplanner.data.repository.settings.SettingsRepository
import com.example.moaiplanner.data.repository.user.AuthRepository
import com.example.moaiplanner.databinding.OptionsFragmentBinding
import com.example.moaiplanner.model.SettingsViewModel
import com.example.moaiplanner.model.SettingsViewModelFactory
import com.example.moaiplanner.ui.welcome.WelcomeActivity
import com.example.moaiplanner.util.disableNotifications
import com.example.moaiplanner.util.enableLight
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL


class OptionsFragment : Fragment() {

    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var binding: OptionsFragmentBinding
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var firebase: AuthRepository
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference
    private lateinit var avatar: StorageReference

    fun newInstance(): OptionsFragment? {
        return OptionsFragment()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        settingsViewModel.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        settingsViewModel.restoreSettings()

    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val toolbar = activity?.findViewById<androidx.appcompat.widget.Toolbar>(R.id.topAppBar)
        toolbar?.menu?.setGroupVisible(R.id.edit, false)
        toolbar?.menu?.setGroupVisible(R.id.sett, false)
        settingsRepository = SettingsRepository(requireActivity())
        val factory = SettingsViewModelFactory(SettingsRepository(requireActivity()))
        settingsViewModel = ViewModelProvider(requireActivity(), factory)[SettingsViewModel::class.java]
        if (savedInstanceState != null) {
            settingsViewModel.onRestoreInstanceState(savedInstanceState)
        }

        binding = OptionsFragmentBinding.inflate(inflater, container, false)
        binding.viewModel = settingsViewModel
        firebase = AuthRepository(requireActivity().application)
        storage = Firebase.storage
        storageRef = storage.reference
        avatar = storageRef.child("${firebase.getCurretUid()}/avatar.png")

        avatar.downloadUrl.addOnSuccessListener { uri ->
            Picasso.get()
                .load(uri.toString())
                .priority(Picasso.Priority.HIGH)
                .into(binding.profilepic)
        }.addOnFailureListener {
            Log.e("A", "File not found in storage: ${it.message}")
        }


        return binding.root

        // Inflate il layout per il fragment
        //return inflater.inflate(R.layout.options_fragment, container, false)
    }




    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // imposta valore sessione quando viene modificato
        binding.durataPomodoro.addTextChangedListener {
            //Log.d("SETTINGS", it.toString())
            if(it.toString().isBlank()) {
                settingsViewModel.session.value = "5"
            }

            else
                settingsViewModel.session.value = it.toString()
        }

        // imposta valore break quando viene modificato
        binding.durataPausa.addTextChangedListener {
            //Log.d("SETTINGS", it.toString())
            if(it.toString().isBlank()) {
                settingsViewModel.pausa.value = "1"
            }
            else
                settingsViewModel.pausa.value = it.toString()
        }

        binding.numeroRound.addTextChangedListener {
            //Log.d("SETTINGS", it.toString())
            if(it.toString().isBlank()) {
                settingsViewModel.round.value = "1"
            }
            else
                settingsViewModel.round.value = it.toString()
        }

        binding.notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsViewModel.notifiche.value = isChecked
            disableNotifications(isChecked, requireContext())
        }

        binding.themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsViewModel.lightMode.value = isChecked
            enableLight(isChecked)
        }

        binding.editImage.setOnClickListener() {
            Intent(Intent.ACTION_GET_CONTENT).also {
                it.type = "image/*"
                startActivityForResult(it, 0)
            }
        }

        binding.buttonLogout.setOnClickListener() {
            lifecycleScope.launch(Dispatchers.IO) {
                firebase.signOut(requireContext())
            }.invokeOnCompletion {
                val intent = Intent(requireActivity(), WelcomeActivity::class.java)
                startActivity(intent)
                requireActivity().finish()

            }
        }

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
            binding.profilepic.setImageBitmap(bitmap)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 0) {
            val uri = data?.data
            Log.d("AVATAR URI", uri.toString())
            val stream = FileInputStream(uri?.let { context?.contentResolver?.openFileDescriptor(it, "r")?.fileDescriptor ?: FileDescriptor() })
            // val file = Uri.fromFile(uri?.toFile() as File)

            //val file = File(uri?.toFile())
            val uploadTask = avatar.putStream(stream)

            // Register observers to listen for when the download is done or if it fails
            uploadTask.addOnFailureListener {
                view?.let { it1 ->
                    Snackbar.make(it1, "Image upload failed", Snackbar.LENGTH_SHORT)
                        .setAction("OK") {
                            // Responds to click on the action
                        }
                        //.setBackgroundTint(resources.getColor(R.color.pr))
                        .setActionTextColor(resources.getColor(R.color.primary, null))
                        .setAnchorView(activity?.findViewById(R.id.bottom_navigation))
                        .show()
                }
                stream.close()
            }.addOnSuccessListener { taskSnapshot ->
                view?.let { it1 ->
                    Snackbar.make(it1, "Imaged uploaded successfully", Snackbar.LENGTH_SHORT)
                        .setAction("OK") {
                            // Responds to click on the action
                        }
                        //.setBackgroundTint(resources.getColor(R.color.pr))
                        .setActionTextColor(resources.getColor(R.color.primary, null))
                        .setAnchorView(activity?.findViewById(R.id.bottom_navigation))
                        .show()
                }
                stream.close()
                binding.profilepic.setImageURI(uri)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        settingsViewModel.saveSettings()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let { settingsViewModel.onRestoreInstanceState(it) }
    }




}

