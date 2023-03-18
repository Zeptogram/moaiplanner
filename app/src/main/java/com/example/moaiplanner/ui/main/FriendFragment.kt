package com.example.moaiplanner.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moaiplanner.R
import com.example.moaiplanner.adapter.CalendarAdapter
import com.example.moaiplanner.data.calendar.CalendarData
import com.example.moaiplanner.data.user.AuthRepository
import com.example.moaiplanner.databinding.FriendFragmentBinding
import com.example.moaiplanner.databinding.TodoFragmentBinding
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class FriendFragment: Fragment() {

    lateinit var binding: FriendFragmentBinding
    private var friends = mutableListOf<FriendItem>()
    private val filteredList = ArrayList<FriendItem>()

    private lateinit var toolbar: Toolbar

    // Adapter per la RecyclerView
    private lateinit var adapter: FriendFragment.FriendAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FriendFragmentBinding.inflate(inflater, container, false)

        toolbar = activity?.findViewById<Toolbar>(R.id.topAppBar)!!
        toolbar?.menu?.setGroupVisible(R.id.edit, false)
        toolbar?.menu?.setGroupVisible(R.id.sett, false)

        toolbar?.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.settings -> {
                    findNavController().navigate(
                        R.id.optionsFragment, null,
                        navOptions {
                            anim {
                                enter = android.R.anim.fade_in
                                popEnter = android.R.anim.fade_in
                            }
                        }
                    )
                }
            }
            true
        }

        // Inizializza la RecyclerView
        adapter = FriendAdapter(friends)
        val recyclerView = binding.friendView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        friends.add(FriendItem("IShowSpeed", R.drawable.huge_avatar))
        adapter.notifyItemInserted(friends.lastIndex)
        friends.add(FriendItem("Marco Bianchi", R.drawable.account))
        adapter.notifyItemInserted(friends.lastIndex)
        filteredList.addAll(friends)

        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filteredList.clear()
                if (newText.isNullOrEmpty()) {
                    filteredList.addAll(friends)
                } else {
                    friends.filterTo(filteredList) {
                        it.username.contains(newText, true)
                    }
                }
                adapter.notifyDataSetChanged()
                return true
            }
        })

        // Inflate il layout per il fragment
        return binding.root
    }

    // Aggiungere successivamente variabile per sapere se amico in modo da cambiare il bottone + immagine
    data class FriendItem(val username: String, var image: Int)

    // Adapter per la RecyclerView
    private inner class FriendAdapter(private val items: List<FriendItem>) : RecyclerView.Adapter<FriendViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.friend_template, parent, false)
            return FriendViewHolder(view)
        }
        override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
            val item = filteredList[position]
            holder.textView.text = item.username
            holder.image.setImageResource(item.image)

        }
        override fun getItemCount(): Int = filteredList.size

    }


    // ViewHolder per gli elementi della RecyclerView
    private inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.friend_name)
        val image: ImageView = itemView.findViewById(R.id.profilepic)

    }

}

