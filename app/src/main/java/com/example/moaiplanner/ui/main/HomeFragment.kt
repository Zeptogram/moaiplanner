package com.example.moaiplanner.ui.main

import RecyclerViewAdapter
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moaiplanner.R
import com.example.moaiplanner.data.repository.user.AuthRepository
import com.example.moaiplanner.databinding.HomeFragmentBinding
import com.example.moaiplanner.databinding.SigninFragmentBinding
import com.example.moaiplanner.util.ItemsViewModel


class HomeFragment : Fragment() {
    lateinit var binding: HomeFragmentBinding
    lateinit var firebase: AuthRepository

    fun newInstance(): HomeFragment? {
        return HomeFragment()
    }

    override fun onStart() {
        super.onStart()

        firebase = AuthRepository(requireActivity().application)
        if (!firebase.isUserAuthenticated()) {
            findNavController().navigate(R.id.welcomeActivity)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = HomeFragmentBinding.inflate(inflater, container, false)

        // Inflate il layout per il fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // initializing variables of grid view with their ids.
        val recyclerview = activity?.findViewById<RecyclerView>(R.id.recyclerview)

        // this creates a vertical layout Manager
        GridLayoutManager(requireActivity(), 2).also { recyclerview?.layoutManager = it }

        // ArrayList of class ItemsViewModel
        val data = ArrayList<ItemsViewModel>()

        for (i in 1..9) {
            data.add(ItemsViewModel("Item " + i))
        }

        // This will pass the ArrayList to our Adapter
        val adapter = RecyclerViewAdapter(data)

        // Setting the Adapter with the recyclerview
        recyclerview?.adapter = adapter

        // on below line we are adding on item
        // click listener for our grid view.

    }
}