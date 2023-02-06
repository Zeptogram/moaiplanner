package com.example.moaiplanner.ui.main


import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.moaiplanner.R
import com.example.moaiplanner.databinding.OptionsFragmentBinding
import com.example.moaiplanner.data.repository.settings.SettingsRepository
import com.example.moaiplanner.data.repository.user.AuthRepository
import com.example.moaiplanner.model.SettingsViewModelFactory
import com.example.moaiplanner.model.SettingsViewModel
import com.example.moaiplanner.ui.welcome.SigninFragment
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.FirebaseStorageKtxRegistrar
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.IOException
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // TODO: Scegliere dove mettere le richieste a Firebase/Firestore se onCreateView/onViewGreated o altro
        settingsRepository = SettingsRepository(requireActivity())
        val factory = SettingsViewModelFactory(SettingsRepository(requireActivity()))
        settingsViewModel = ViewModelProvider(requireActivity(), factory)[SettingsViewModel::class.java]
        if (savedInstanceState != null) {
            settingsViewModel.onRestoreInstanceState(savedInstanceState)
        }
        binding = OptionsFragmentBinding.inflate(inflater, container, false)
        binding.viewModel = settingsViewModel

        return binding.root
    }

    override fun onStart() {
        super.onStart()

        firebase = AuthRepository(requireActivity().application)
        storage = Firebase.storage
        storageRef = storage.reference
        avatar = storageRef.child("${firebase.getCurretUid()}/avatar.png")

        avatar.downloadUrl.addOnSuccessListener { task ->
            var bitmap: Bitmap? = null
            lifecycleScope.launch(Dispatchers.IO) {
                bitmap = convertBitmapFromURL(task.toString())
            }.invokeOnCompletion {
                updateUI(bitmap)
            }
        }.addOnFailureListener { task ->
            Log.d("FIRESTORE-AVATAR", task.toString())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // imposta valore sessione quando viene modificato
        binding.durataPomodoro.addTextChangedListener {
            //Log.d("SETTINGS", it.toString())
            if(it.toString().isBlank()) {
                settingsViewModel.session.value = "5"
            }
            else {
                settingsViewModel.session.value = it.toString()
            }
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

        binding.notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {

            }
        }

        binding.editImage.setOnClickListener() {
            Intent(Intent.ACTION_GET_CONTENT).also {
                it.type = "image/*"
                startActivityForResult(it, 0)
            }
        }

        binding.buttonLogout.setOnClickListener() {
            lifecycleScope.launch(Dispatchers.IO) {
                firebase.signOut()
            }.invokeOnCompletion {
                lifecycleScope.launch(Dispatchers.Main) {
                    findNavController().navigate(R.id.welcomeActivity)
                }
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
                Toast.makeText(context, "Image upload failed", Toast.LENGTH_SHORT).show()
                stream.close()
            }.addOnSuccessListener { taskSnapshot ->
                Toast.makeText(context, "Image uploaded successful", Toast.LENGTH_SHORT).show()
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