package com.example.moaiplanner.ui.main

import RecyclerViewAdapter
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moaiplanner.R
import com.example.moaiplanner.data.repository.user.AuthRepository
import com.example.moaiplanner.databinding.HomeFragmentBinding
import com.example.moaiplanner.databinding.SigninFragmentBinding
import com.example.moaiplanner.util.ItemsViewModel
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.component1
import com.google.firebase.storage.ktx.component2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream


class HomeFragment : Fragment() {
    lateinit var binding: HomeFragmentBinding
    lateinit var firebase: AuthRepository
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference
    private lateinit var userDir: StorageReference

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

        firebase = AuthRepository(requireActivity().application)
        storage = Firebase.storage
        storageRef = storage.reference
        userDir = storageRef.child("${firebase.getCurretUid()}")

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
        // This will pass the ArrayList to our Adapter
        var adapter = RecyclerViewAdapter(data)
        // Setting the Adapter with the recyclerview
        recyclerview?.adapter = adapter

        // Get list of files from Firestore
        userDir.listAll()
            .addOnSuccessListener { (items, prefixes) ->
                prefixes.forEach { prefix ->
                    Log.d("FIRESTORAGE-PREFIX", prefix.toString())
                    data.add(ItemsViewModel(prefix.toString().split("/").last()))
                    prefix.listAll()
                        .addOnSuccessListener {  (items) ->
                            items.forEach { item ->
                                Log.d("FIRESTORAGE-PREFIX-ITEM", item.toString())
                                // Regex che rimuove avatar e file in più che non servono nelle collections/notes
                                if (item.toString().split("/").last().contains("^[^.]*\$|.*\\.md\$".toRegex()))
                                    data.add(ItemsViewModel(item.toString().split("/").last()))
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Error getting files", Toast.LENGTH_SHORT).show()
                        }
                }

                items.forEach { item ->
                    Log.d("FIRESTORAGE-ITEM", item.toString())
                    if (item.toString().split("/").last().contains("^[^.]*\$|.*\\.md\$".toRegex()))
                        data.add(ItemsViewModel(item.toString().split("/").last()))
                }
            }
            .addOnFailureListener {
                Log.d("FIRESTORAGE-ERROR", "Error getting file list")
                Toast.makeText(context, "Error getting files", Toast.LENGTH_SHORT).show()
            }
            .addOnSuccessListener {
                adapter = RecyclerViewAdapter(data)
                recyclerview?.adapter = adapter
            }

        binding.buttonShowall.setOnClickListener {
            Intent(Intent.ACTION_OPEN_DOCUMENT).also {
                it.type = "text/markdown"
                startActivityForResult(it, 0)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 0) {
            val uri = data?.data
            Log.d("NOTE URI", uri.toString())

            val fileName = "testInCollection.md"
            val stream = FileInputStream(uri?.let { context?.contentResolver?.openFileDescriptor(it, "r")?.fileDescriptor ?: FileDescriptor() })

            val noteDir = storageRef.child("${firebase.getCurretUid()}/testCollection/${fileName}")
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