package com.example.moaiplanner.ui.welcome

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.moaiplanner.R
import com.example.moaiplanner.data.user.GoogleSignInHelper
import com.example.moaiplanner.data.user.UserAuthentication
import com.example.moaiplanner.databinding.SigninFragmentBinding
import com.example.moaiplanner.util.NetworkUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class SigninFragment : Fragment() {

    lateinit var binding: SigninFragmentBinding
    lateinit var firebase: UserAuthentication
    private lateinit var googleSignInHelper: GoogleSignInHelper
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        googleSignInHelper.handleActivityResult(result.resultCode, result.data)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SigninFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        NetworkUtils.notifyMissingNetwork(requireContext(), activity)

        binding.buttonGoogleLogin.setOnClickListener {
            Log.d("USER", "Google Sign in Requested")
            googleSignInHelper = GoogleSignInHelper(requireActivity(), signInLauncher, view)
            googleSignInHelper.signInGoogle()
        }

        binding.forgotPassword.setOnClickListener {
            Log.d("USER", "Reset Password Requested")
            showResetDialog()
        }

        binding.buttonSignIn.setOnClickListener {
            Log.d("USER", "Sign in Requested")
            firebase = UserAuthentication(requireActivity().application, view, requireActivity())
            lifecycleScope.launch(Dispatchers.IO) {
                val email = binding.editTextEmail.text.toString()
                val password = binding.editTextPassword.text.toString()
                firebase.signInAndSaveUser(email, password, requireActivity(), view)
            }
        }
    }

    private fun showResetDialog() {
        val layoutInflater = LayoutInflater.from(context)
        val dialogView = layoutInflater.inflate(R.layout.dialog_reset_password, null)
        val editEmail = dialogView.findViewById<EditText>(R.id.emailText)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.inserisci_la_email_associata_all_account))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.send_email)) { _, _ ->
                val text = editEmail.text.toString()
                if (text.isNotBlank()) {
                    firebase.resetPassword(text)
                }
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
            }
            .create()
        dialog.show()
    }
}