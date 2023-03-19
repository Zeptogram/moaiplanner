package com.example.moaiplanner.ui.main

import FolderViewAdapter
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.CheckBox
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moaiplanner.R
import com.example.moaiplanner.data.calendar.CalendarData
import com.example.moaiplanner.data.user.UserAuthentication
import com.example.moaiplanner.databinding.HomeFragmentBinding
import com.example.moaiplanner.util.FolderItem
import com.example.moaiplanner.util.NetworkUtils
import com.example.moaiplanner.util.ToDoItem
import com.example.moaiplanner.util.getFolderSize
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.component1
import com.google.firebase.storage.ktx.component2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
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


    fun newInstance(): HomeFragment? {
        return HomeFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = HomeFragmentBinding.inflate(inflater, container, false)

        /*if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            AlertDialog.Builder(requireContext())
                .setTitle("Connessione di rete assente")
                .setMessage("Verifica la tua connessione di rete e riprova")
                .setPositiveButton("OK", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        }*/


        firebase = UserAuthentication(requireActivity().application)
        storage = Firebase.storage
        storageRef = storage.reference
        //        userDir = storageRef.child("${firebase.getCurretUid()}/notes")
        userDir = storageRef.child("${firebase.getCurrentUid()}")

        db = FirebaseDatabase.getInstance()
        todoListRef = db.getReference("users/" + firebase.getCurrentUid().toString())

        fetchToDoListFromFirebase()

        val toolbar = activity?.findViewById<androidx.appcompat.widget.Toolbar>(R.id.topAppBar)
        toolbar?.menu?.setGroupVisible(R.id.edit, false)
        toolbar?.menu?.setGroupVisible(R.id.sett, true)

        toolbar?.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.settings -> {
                    findNavController().navigate(R.id.optionsFragment, null,
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

        /*binding.buttonFriends.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_friendFragment, null,
                navOptions {
                    anim {
                        enter = android.R.anim.fade_in
                        popEnter = android.R.anim.fade_in
                    }
                }
            )
        }*/

        currentDate = calendarData.calendarYear + "/" + calendarData.calendarMonth + "/" + calendarData.calendarDate
        // Inflate il layout per il fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        NetworkUtils.notifyMissingNetwork(requireContext(), view)
        var bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        // Mette la home come main
        bottomNav.menu.getItem(0).isChecked = true;
    }


    override fun onStart() {
        super.onStart()

        firebase = UserAuthentication(requireActivity().application)
        if (!firebase.isUserAuthenticated()) {
            findNavController().navigate(R.id.welcomeActivity)
        }

        // initializing variables of grid view with their ids.
        val recyclerview = activity?.findViewById<RecyclerView>(R.id.recyclerview)
        val todoview = activity?.findViewById<RecyclerView>(R.id.todoList)


        todoview?.setHasFixedSize(true)
        todoview?.isNestedScrollingEnabled = true


        todoview!!.adapter = toDoAdapter




        // this creates a vertical layout Manager
        GridLayoutManager(requireActivity(), 1).also { recyclerview?.layoutManager = it }

        // ArrayList of class ItemsViewModel
        /*val data = ArrayList<ItemsViewModel>()
        // This will pass the ArrayList to our Adapter
        val adapter = RecyclerViewAdapter(data)*/
        var data = ArrayList<FolderItem>()
        var adapter = FolderViewAdapter(data)
        // Setting the Adapter with the recyclerview
        recyclerview?.adapter = adapter
        // OnClick on Recycler elements
        //adapter.setOnItemClickListener(object : RecyclerViewAdapter.onItemClickListener {
        adapter.setOnItemClickListener(object : FolderViewAdapter.onItemClickListener {
            override fun onItemClick(position: Int) {
                if(position == 0) {
                    findNavController().navigate(R.id.action_homeFragment_to_fileFragment, null,
                        navOptions {
                            anim {
                                enter = android.R.anim.fade_in
                                popEnter = android.R.anim.fade_in
                            }
                        }
                    )
                }
            }

            override fun onItemLongClick(position: Int) {
                // Empty
            }
        })


        getCollections(data, adapter)
        adapter.notifyDataSetChanged()





    }

    fun getCollections(data: ArrayList<FolderItem>, adapter: FolderViewAdapter) {
        // Get list of files from Firestore
        lifecycleScope.launch(Dispatchers.IO) {
            userDir.listAll()
                .addOnSuccessListener { (items, prefixes) ->
                    if(prefixes.isEmpty()) {
                        var noteDir = storageRef.child("${firebase.getCurrentUid()}/Notes/temp.tmp")
                        val text = " "
                        var uploadFile = noteDir.putBytes(text.toByteArray())
                        uploadFile.addOnFailureListener {
                            //resetFolderView()
                            // Handle unsuccessful uploads
                        }.addOnSuccessListener { taskSnapshot ->
                            // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
                            // ...
                            Log.d("FOLDER-CREATION", "Folder creato")
                            getCollections(data, adapter)
                        }
                    }else {
                        prefixes.forEach { prefix ->
                            Log.d("FIRESTORAGE-PREFIX", prefix.toString())
                            var fileItem = FolderItem(
                                prefix.toString().split("/").last().replace("%20", " "),
                                "",
                                false,
                                R.drawable.folder
                            )
                            data.add(fileItem)
                            getFolderSize(prefix) { bytes, files ->
                                val df = DecimalFormat("#,##0.##")
                                df.maximumFractionDigits = 2
                                var kb = bytes.toDouble() / 1024
                                val info = df.format(kb) + "KB - " + files.toString() + " Notes"
                                fileItem.folder_files = info
                                adapter.notifyDataSetChanged()
                            }
                            prefix.listAll()
                        }

                    }

                    items.forEach { item ->
                        Log.d("FIRESTORAGE-ITEM", item.toString())
                        if (item.toString().split("/").last().contains("^[^.]*\$|.*\\.md\$".toRegex()))
                            data.add(FolderItem(item.toString().split("/").last().replace("%20", " "), "Todo", false, R.drawable.baseline_insert_drive_file_24))
                    }
                }
                .addOnFailureListener {
                    Log.d("FIRESTORAGE-ERROR", "Error getting file list")
                    view?.let { it1 ->
                        Snackbar.make(it1, "Error Getting Files", Snackbar.LENGTH_SHORT)
                            .setAction("OK") {
                                // Responds to click on the action
                            }
                            //.setBackgroundTint(resources.getColor(R.color.pr))
                            .setActionTextColor(resources.getColor(R.color.primary, null))
                            .setAnchorView(activity?.findViewById(R.id.bottom_navigation))
                            .show()
                    }
                }
                .addOnSuccessListener {
                    //adapter = RecyclerViewAdapter(data)
                    //recyclerview?.adapter = adapter
                    adapter.notifyDataSetChanged()
                }
        }.invokeOnCompletion {
            /*Log.d("DATA", data.toString())
            if(data.isEmpty()) {

            }*/
        }
    }



    private inner class ToDoViewerAdapter(private val items: List<ToDoItem>) : RecyclerView.Adapter<ToDoViewHolder>() {


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
    private inner class ToDoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
        var timeView: TextView = itemView.findViewById(R.id.timeView)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)


    }

    private fun fetchToDoListFromFirebase() {
        val toDoListListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                    toDoList.clear()
                    val date = currentDate.split("/")
                    val toDoListData = snapshot.child("todolist").child(date[0]).child(date[1]).child(date[2]).children
                    for (item in toDoListData) {
                        //Log.d("Item", item.toString())
                        val toDoItem = item.getValue(ToDoItem::class.java)
                        if (toDoItem != null) {
                            if(toDoItem.id.isNotBlank() && !toDoItem.isDone) {
                                toDoList.add(toDoItem!!)
                            }
                        }
                    }
                if (toDoList.isEmpty())
                    binding.completed.visibility = View.VISIBLE
                else
                    binding.completed.visibility = View.GONE

                toDoList.sortBy { item ->
                    val format = SimpleDateFormat("HH:mm")
                    val date = format.parse(item.time)
                    date.time / (60 * 1000)
                }
                toDoAdapter.notifyDataSetChanged()

            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("HomeFragment", "loadToDoList:onCancelled", error.toException())
            }

        }
        todoListRef.addValueEventListener(toDoListListener)
    }

     fun modifyItemState(itemObjectId: String, isDone: Boolean) {
        val date = currentDate.split("/")
        val ref = todoListRef.child("todolist").child(date[0]).child(date[1]).child(date[2]).child(itemObjectId)
        ref.child("done").setValue(isDone)
    }



}