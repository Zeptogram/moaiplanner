package com.example.moaiplanner.ui.welcome

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation.findNavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.example.moaiplanner.databinding.RegisterFragmentBinding
import com.example.moaiplanner.R
import com.example.moaiplanner.databinding.OptionsFragmentBinding


class RegisterFragment : Fragment() {

    lateinit var binding: RegisterFragmentBinding


    fun newInstance(): RegisterFragment? {
        return RegisterFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = RegisterFragmentBinding.inflate(inflater, container, false)
        // Inflate il layout per il fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)




    }

}