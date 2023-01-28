package com.example.moaiplanner.ui.main

import RecyclerViewAdapter
import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moaiplanner.R
import com.example.moaiplanner.ui.utils.ItemsViewModel
import com.example.moaiplanner.ui.view.SettingsViewModel


class HomeFragment : Fragment() {

    fun newInstance(): HomeFragment? {
        return HomeFragment()
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {









    // Inflate il layout per il fragment
        return inflater.inflate(R.layout.home_fragment, container, false)
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