package com.example.moaiplanner.ui.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.widget.CheckBox
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moaiplanner.R
import com.example.moaiplanner.adapter.FolderViewAdapter
import com.example.moaiplanner.data.calendar.CalendarData
import com.example.moaiplanner.data.notes.FolderManager
import com.example.moaiplanner.data.todo.ToDoFetcher
import com.example.moaiplanner.data.user.UserAuthentication
import com.example.moaiplanner.databinding.HomeFragmentBinding
import com.example.moaiplanner.util.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList


class HomeFragment : Fragment() {
    lateinit var binding: HomeFragmentBinding
    lateinit var firebase: UserAuthentication
    private lateinit var storage: FirebaseStorage
    private lateinit var db: FirebaseDatabase
    private lateinit var storageRef: StorageReference
    private lateinit var userDir: StorageReference
    private lateinit var todoListRef: DatabaseReference
    private val toDoList = mutableListOf<ToDoItem>()
    private val toDoAdapter = ToDoViewerAdapter(toDoList)
    private lateinit var currentDate: String
    private var calendarData = CalendarData(Date(), true)
    private lateinit var avatar: StorageReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = HomeFragmentBinding.inflate(inflater, container, false)
        firebase = UserAuthentication(requireActivity().application)
        storage = Firebase.storage
        storageRef = storage.reference
        avatar = storageRef.child("${firebase.getCurrentUid()}/avatar.png")
        userDir = storageRef.child("${firebase.getCurrentUid()}")
        db = FirebaseDatabase.getInstance()
        todoListRef = db.getReference("users/" + firebase.getCurrentUid().toString())
        currentDate = calendarData.calendarYear + "/" + calendarData.calendarMonth + "/" + calendarData.calendarDate
        // Inflate il layout per il fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        NetworkUtils.notifyMissingNetwork(requireContext(), requireView(), requireActivity())
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val toolbar = activity?.findViewById<androidx.appcompat.widget.Toolbar>(R.id.topAppBar)
        toolbar?.menu?.setGroupVisible(R.id.edit, false)
        toolbar?.menu?.setGroupVisible(R.id.sett, true)
        toolbar?.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.settings -> {
                    NavigationHelper.navigateTo(view, R.id.optionsFragment)
                }
            }
            true
        }
        val todo = ToDoFetcher()
        todo.fetchToDoListHomeFromFirebase(toDoList, currentDate, toDoAdapter, todoListRef, binding.completed)
        // Mette la home come main
        bottomNav.menu.getItem(0).isChecked = true
        // Download dell'immagine
        Utils.loadImage(userDir, firebase, avatar, binding.profilepic)
        binding.user.text = firebase.getDisplayName()
    }
    @SuppressLint("NotifyDataSetChanged")
    override fun onStart() {
        super.onStart()
        firebase = UserAuthentication(requireActivity().application)
        if (!firebase.isUserAuthenticated()) {
            findNavController().navigate(R.id.welcomeActivity)
        }
        val recyclerview = activity?.findViewById<RecyclerView>(R.id.recyclerview)
        val todoview = activity?.findViewById<RecyclerView>(R.id.todoList)
        todoview?.setHasFixedSize(true)
        todoview?.isNestedScrollingEnabled = true
        todoview!!.adapter = toDoAdapter
        GridLayoutManager(requireActivity(), 1).also { recyclerview?.layoutManager = it }
        val fm = FolderManager(requireActivity(), requireView())
        val data = ArrayList<FolderItem>()
        val adapter = FolderViewAdapter(data)
        recyclerview?.adapter = adapter
        // OnClick on Recycler elements
        adapter.setOnItemClickListener(object : FolderViewAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                if(position == 0) {
                    view?.let { NavigationHelper.navigateTo(it, R.id.fileFragment) }
                }
            }
            override fun onItemLongClick(position: Int) {
                // Empty
            }
        })
        lifecycleScope.launch(Dispatchers.IO) {
            fm.loadHome(userDir, adapter, data)
        }.invokeOnCompletion {
            adapter.notifyDataSetChanged()
        }

    }

    // Parte di ToDoList
    inner class ToDoViewerAdapter(private val items: List<ToDoItem>) : RecyclerView.Adapter<ToDoViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToDoViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.todo_view, parent, false)
            return ToDoViewHolder(view)
        }
        override fun onBindViewHolder(holder: ToDoViewHolder, position: Int) {
            val item = items[position]
            holder.textView.text = item.task
            holder.timeView.text = item.time
            holder.checkBox.isChecked = item.isDone
            holder.timeView.text = item.time
            holder.checkBox.setOnClickListener {
                item.isDone = !item.isDone
                modifyItemState(item.id, item.isDone)
            }

        }
        override fun getItemCount(): Int = items.size
    }

    // ViewHolder per gli elementi della RecyclerView
    inner class ToDoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
        var timeView: TextView = itemView.findViewById(R.id.timeView)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
    }

     fun modifyItemState(itemObjectId: String, isDone: Boolean) {
        val date = currentDate.split("/")
        val ref = todoListRef.child("todolist").child(date[0]).child(date[1]).child(date[2]).child(itemObjectId)
        ref.child("done").setValue(isDone)
    }





}