package com.example.moaiplanner.ui.main

import FolderViewAdapter
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moaiplanner.R
import com.example.moaiplanner.data.repository.user.AuthRepository
import com.example.moaiplanner.databinding.HomeFragmentBinding
import com.example.moaiplanner.databinding.NotelistFragmentBinding
import com.example.moaiplanner.util.FolderItem
import com.example.moaiplanner.util.ItemsViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.component1
import com.google.firebase.storage.ktx.component2
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.internal.notifyAll
import java.io.FileDescriptor
import java.io.FileInputStream

class FileFragment: Fragment() {
    lateinit var binding: NotelistFragmentBinding
    private var files = ArrayList<FolderItem>()
    private var shownFiles = ArrayList<FolderItem>()
    lateinit var firebase: AuthRepository
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference
    private lateinit var userDirNotes: StorageReference
    private var currentFolder = ""

    private lateinit var toolbar: Toolbar

    // Adapter per la RecyclerView
    private lateinit var adapter: FolderViewAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = NotelistFragmentBinding.inflate(inflater, container, false)

        firebase = AuthRepository(requireActivity().application)
        storage = Firebase.storage
        storageRef = storage.reference
        userDirNotes = storageRef.child("${firebase.getCurretUid()}/notes")

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


        binding.buttonShowall.setOnClickListener {

            binding.buttonShowall.isEnabled = false
            var iterator = files.iterator()
            while(iterator.hasNext()) {
                val file = iterator.next()
                if(!file.isFavourite && !shownFiles.contains(file)) {

                    shownFiles.add(file)
                    adapter.notifyItemInserted(shownFiles.lastIndex)

                }

            }
            binding.buttonFavourites.isEnabled = true

        }

        binding.buttonFavourites.setOnClickListener {

            binding.buttonFavourites.isEnabled = false
            var i: Int = 0
            Log.d("TEST", files.toString())
            var iterator = shownFiles.iterator()
            while(iterator.hasNext()) {
                val file = iterator.next()
                if(!file.isFavourite) {
                    iterator.remove()
                    adapter.notifyItemRemoved(i)
                    i--

                }
                i++
            }
            binding.buttonShowall.isEnabled = true
        }



        // Inflate il layout per il fragment
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        firebase = AuthRepository(requireActivity().application)
        if (!firebase.isUserAuthenticated()) {
            findNavController().navigate(R.id.welcomeActivity)
        }

        // initializing variables of grid view with their ids.
        // Inizializza la RecyclerView

        adapter = FolderViewAdapter(shownFiles)
        val recyclerView = binding.files
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter




        // OnClick on Recycler elements
        adapter.setOnItemClickListener(object : FolderViewAdapter.onItemClickListener {

            override fun onItemClick(position: Int) {
                val bundle = Bundle()
                // Se è un file, allora navigation al note fragmnet passando nome file nel bundle
                if (adapter.getFileName(position).endsWith(".md")) {
                    val navHostFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                    val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
                    bottomNav.menu.getItem(1).isChecked = true
                    bundle.putString("noteDir", currentFolder.plus(adapter.getFileName(position)))
                    setFragmentResult("noteDirFromHome", bundle)
                    navHostFragment.findNavController().navigate(R.id.noteFragment, null,
                        navOptions {
                            anim {
                                enter = android.R.anim.fade_in
                                popEnter = android.R.anim.fade_in
                            }
                        }, null)
                } else {
                    files.clear()
                    shownFiles.clear()
                    adapter.notifyDataSetChanged()
                    binding.buttonFavourites.isEnabled = true
                    binding.buttonShowall.isEnabled = false
                    // TODO: Dialog per creazione folder e navigazione folder (Con pulsante back tolgo "prova" da directory
                    currentFolder = "prova/"
                    getCollections(files, adapter, "prova")
                }
            }
        })


        getCollections(files, adapter, "")



    }


    fun getCollections(data: ArrayList<FolderItem>, adapter: FolderViewAdapter, folderName: String) {
        // Get list of files from Firestore
        lifecycleScope.launch(Dispatchers.IO) {
            val folder = storageRef.child("${firebase.getCurretUid()}/notes/${folderName}")
            Log.d("collectionNotesRef", folder.toString())
            folder.listAll()
                .addOnSuccessListener { (items, prefixes) ->
                    prefixes.forEach { prefix ->
                        Log.d("FIRESTORAGE-PREFIX", prefix.toString())
                        data.add(FolderItem(prefix.toString().split("/").last(), "ciao", false, R.drawable.folder))
                        prefix.listAll()
                    }

                    items.forEach { item ->
                        Log.d("FIRESTORAGE-ITEM", item.toString())
                        if (item.toString().split("/").last().contains("^[^.]*\$|.*\\.md\$".toRegex()))
                            data.add(FolderItem(item.toString().split("/").last(), "ciao", false, R.drawable.baseline_insert_drive_file_24))
                    }
                }
                .addOnFailureListener {
                    Log.d("FIRESTORAGE-ERROR", "Error getting file list")
                    Toast.makeText(context, "Error getting files", Toast.LENGTH_SHORT).show()
                }
                .addOnSuccessListener {
                    //adapter = RecyclerViewAdapter(data)
                    //recyclerview?.adapter = adapter

                    shownFiles.clear()
                    shownFiles.addAll(files)
                    adapter.notifyDataSetChanged()
                }

        }



    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 0) {
            val uri = data?.data
            Log.d("NOTE URI", uri.toString())

            val fileName = "oggi.md"
            val stream = FileInputStream(uri?.let { context?.contentResolver?.openFileDescriptor(it, "r")?.fileDescriptor ?: FileDescriptor() })

            val noteDir = storageRef.child("${firebase.getCurretUid()}/notes/prova/${fileName}")
            val uploadTask = noteDir.putStream(stream)

            // Register observers to listen for when the download is done or if it fails
            uploadTask.addOnFailureListener {
                Toast.makeText(context, "Note upload failed", Toast.LENGTH_SHORT).show()
                stream.close()
            }.addOnSuccessListener { taskSnapshot ->
                Toast.makeText(context, "Note uploaded successful", Toast.LENGTH_SHORT).show()
                stream.close()
            }
        }
    }







}