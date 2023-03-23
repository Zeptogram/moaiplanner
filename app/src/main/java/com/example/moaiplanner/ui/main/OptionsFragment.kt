package com.example.moaiplanner.ui.main


import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.moaiplanner.R
import com.example.moaiplanner.data.repository.settings.SettingsRepository
import com.example.moaiplanner.data.user.UserAuthentication
import com.example.moaiplanner.databinding.OptionsFragmentBinding
import com.example.moaiplanner.model.SettingsViewModel
import com.example.moaiplanner.model.SettingsViewModelFactory
import com.example.moaiplanner.ui.welcome.WelcomeActivity
import com.example.moaiplanner.util.disableNotifications
import com.example.moaiplanner.util.enableLight
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.component1
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.IOException
import java.net.URL


class OptionsFragment : Fragment() {

    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var binding: OptionsFragmentBinding
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var firebase: UserAuthentication
    private lateinit var storageRef: StorageReference
    private lateinit var avatar: StorageReference
    private lateinit var ref: StorageReference
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





        return binding.root

        // Inflate il layout per il fragment
        //return inflater.inflate(R.layout.options_fragment, container, false)
    }




    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebase = UserAuthentication(requireActivity().application, view)
        storageRef = Firebase.storage.reference
        avatar = storageRef.child("${firebase.getCurrentUid()}/avatar.png")
        ref = storageRef.child("${firebase.getCurrentUid()}/")

        if(firebase.getProvider() == "google.com") {
            binding.buttonChangePass.isEnabled = false
            binding.buttonChangeEmail.isEnabled = false
            binding.buttonChangeName.isEnabled = false
        }

        ref.listAll().addOnSuccessListener { (items) ->
            items.forEach { item ->
                Log.d("OPTIONS", item.toString())
                if(item.toString().substringAfterLast("/") == "avatar.png") {
                    downloadImage()
                }
            }
            if(firebase.getProvider() == "google.com") {
                var uri = firebase.getGoogleImage().toString()
                Picasso.get()
                    .load(uri)
                    .priority(Picasso.Priority.HIGH)
                    .into(binding.profilepic)
            }
        }
        .addOnFailureListener {
            Log.e("IMAGE", "Using default picture")
        }

        binding.usermail.text = firebase.getEmail()
        binding.username.text = firebase.getDisplayName()


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

        binding.buttonChangeName.setOnClickListener {
            showEditNameDialog()
        }

        binding.buttonChangeEmail.setOnClickListener {
            showEditMailDialog()
        }

        binding.buttonChangePass.setOnClickListener {
            showEditPasswordDialog()
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

    private fun showEditNameDialog() {
        val layoutInflater = LayoutInflater.from(context)
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_username, null)
        val editUsername = dialogView.findViewById<EditText>(R.id.userText)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Modifica l'Username")
            .setView(dialogView)
            .setPositiveButton("Modifica") { dialog, which ->
                val text = editUsername.text.toString()

                if (text.isNotBlank()) {
                    firebase.setDisplayName(text, binding.username)
                }
            }
            .setNegativeButton("Annulla") { dialog, which ->
            }
            .create()

        dialog.show()

    }

    private fun showEditMailDialog() {
        val layoutInflater = LayoutInflater.from(context)
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_email, null)
        val editEmail = dialogView.findViewById<EditText>(R.id.emailText)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Modifica la Email")
            .setView(dialogView)
            .setPositiveButton("Modifica") { dialog, which ->
                val text = editEmail.text.toString()

                if (text.isNotBlank()) {
                    firebase.setEmail(text, binding.username)
                }
            }
            .setNegativeButton("Annulla") { dialog, which ->
            }
            .create()

        dialog.show()

    }

    private fun showEditPasswordDialog() {
        val layoutInflater = LayoutInflater.from(context)
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val oldPass = dialogView.findViewById<EditText>(R.id.oldPassText)
        val newPass = dialogView.findViewById<EditText>(R.id.newPassText)


        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Modifica la Password")
            .setView(dialogView)
            .setPositiveButton("Modifica") { dialog, which ->
                val old = oldPass.text.toString()
                var new = newPass.text.toString()

                if (old.isNotBlank() && new.isNotBlank()) {
                    firebase.setPassword(old, new)
                }
            }
            .setNegativeButton("Annulla") { dialog, which ->
            }
            .create()

        dialog.show()

    }

    private fun downloadImage(){
        avatar.downloadUrl.addOnSuccessListener { uri ->
            Picasso.get()
                .load(uri.toString())
                .priority(Picasso.Priority.HIGH)
                .into(binding.profilepic)
        }.addOnFailureListener { exception ->
            if (exception is StorageException &&
                (exception as StorageException).errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                // File not found, handle appropriately
                Log.e("IMAGE", "File not found in storage")
            } else {
                // Other storage exception, handle appropriately
                Log.e("IMAGE", "StorageException: ${exception.message}")
            }
        }
    }


}

